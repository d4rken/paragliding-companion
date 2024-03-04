package eu.darken.pgc.importer.ui

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.pgc.R
import eu.darken.pgc.common.ca.toCaString
import eu.darken.pgc.common.coroutine.DispatcherProvider
import eu.darken.pgc.common.debug.logging.Logging.Priority.INFO
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.livedata.SingleLiveEvent
import eu.darken.pgc.common.uix.ViewModel3
import eu.darken.pgc.importer.core.IngestIGCPayload
import eu.darken.pgc.importer.core.Ingester
import eu.darken.pgc.importer.core.UsbDevicesProvider
import eu.darken.pgc.importer.core.UsbImporter
import eu.darken.pgc.importer.ui.items.ManualCardVH
import eu.darken.pgc.importer.ui.items.ManualCardVH.Item.ManualImportState
import eu.darken.pgc.importer.ui.items.ReparseCardVH
import eu.darken.pgc.importer.ui.items.ReparseCardVH.Item.ReparserState
import eu.darken.pgc.importer.ui.items.UsbCardVH
import eu.darken.pgc.importer.ui.items.UsbCardVH.Item.UsbImportstate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import okhttp3.internal.closeQuietly
import okio.source
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ImporterFragmentVM @Inject constructor(
    handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val ingester: Ingester,
    private val contentResolver: ContentResolver,
    @ApplicationContext private val context: Context,
    private val usbManager: UsbManager,
    private val usbDevicesProvider: UsbDevicesProvider,
    private val usbImporter: UsbImporter,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    val events = SingleLiveEvent<ImporterEvents>()

    private val reparserState = MutableStateFlow<ReparserState>(ReparserState.Start())
    private val manualImportState = MutableStateFlow<ManualImportState>(ManualImportState.Start())
    private val usbImportState = MutableStateFlow<UsbImportstate>(UsbImportstate.Start())
    private val selectedDevice = MutableStateFlow(null as UsbDevice?)

    val state = combine(
        reparserState,
        manualImportState,
        usbImportState,
        usbDevicesProvider.devices,
        selectedDevice,
    ) { reparserState, manualState, usbImportState, usbDevices, selectedUsb ->
        val items = mutableListOf<ImporterAdapter.Item>()

        ReparseCardVH.Item(
            state = reparserState
        ).takeIf { it.state !is ReparserState.Start }?.run { items.add(this) }

        ManualCardVH.Item(
            state = manualState,
            onImport = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                events.postValue(ImporterEvents.ShowPicker(intent))
            },
            onCancel = { manualImportJob?.cancel() }
        ).run { items.add(this) }

        UsbCardVH.Item(
            selectedDevice = selectedUsb,
            devices = usbDevices.toList(),
            state = usbImportState,
            onImport = { importUsb() },
            onCancel = { usbImportJob?.cancel() },
            onDeviceSelected = { selectedDevice.value = it }
        ).run { items.add(this) }

        State(items = items)
    }.asLiveData2()

    private var manualImportJob: Job? = null
    fun importManual(uris: Set<Uri>) = launch {
        log(TAG) { "importManual(uris=${uris.size})" }
        manualImportState.value = ManualImportState.Progress(
            max = uris.size,
            progressMsg = R.string.general_progress_loading.toCaString()
        )

        val success = mutableListOf<Uri>()
        val skipped = mutableListOf<Uri>()
        val failed = mutableListOf<Pair<Uri, Exception>>()

        uris
            .filter { MimeTypeMap.getFileExtensionFromUrl(it.path!!) == "igc" }
            .forEach { uri ->
                log(TAG) { "importManual(...): $uri" }
                manualImportState.value =
                    (manualImportState.value as ManualImportState.Progress).copy(
                        progressMsg = (uri.lastPathSegment ?: uri.toString()).toCaString()
                    )

                val dangles = mutableSetOf<InputStream>()

                try {
                    val added = ingester.ingest(
                        IngestIGCPayload(
                            sourceProvider = {
                                contentResolver.openInputStream(uri)!!.also { dangles.add(it) }.source()
                            },
                            originalSource = uri.toString(),
                            sourceType = when {
                                uri.authority?.contains("org.xcontest.XCTrack") == true -> IngestIGCPayload.SourceType.XCTRACK
                                else -> IngestIGCPayload.SourceType.UNKNOWN
                            },
                        )
                    )

                    if (added) success.add(uri) else skipped.add(uri)
                } catch (e: Exception) {
                    failed.add(uri to e)
                } finally {
                    dangles.forEach { it.closeQuietly() }
                }

                manualImportState.value =
                    (manualImportState.value as ManualImportState.Progress).let {
                        it.copy(current = it.current + 1)
                    }
            }

        (uris - (success + skipped).toSet()).forEach {
            failed.add(it to IllegalArgumentException("Unknown data type"))
        }

        manualImportState.value = ManualImportState.Result(
            success = success,
            skipped = skipped,
            failed = failed
        )
    }.also { manualImportJob = it }

    fun reparse() = launch {
        log(TAG) { "reparse()" }
        reparserState.value = ReparserState.Progress(
            max = ingester.flightCount(),
            progressMsg = R.string.general_progress_loading.toCaString()
        )

        val changed = ingester.reingest { progress ->
            reparserState.value = (reparserState.value as ReparserState.Progress).let {
                it.copy(
                    progressMsg = progress,
                    current = it.current + 1
                )
            }
        }

        reparserState.value = ReparserState.Result(
            changes = changed
        )
    }

    private var usbImportJob: Job? = null
    fun importUsb() = launch {
        val device = selectedDevice.value
        log(TAG) { "importUsb($device)" }
        if (device == null) {
            usbImportState.value = UsbImportstate.Start()
            return@launch
        }

        if (!usbManager.hasPermission(device)) {
            log(TAG) { "importUsb(...): Requesting permission for device: $device" }
            events.postValue(ImporterEvents.RequestUsbPermission(device))
            return@launch
        }

        log(TAG, INFO) { "importUsb(...): Permission available for $device" }

        usbImportState.value = UsbImportstate.Progress(
            progressMsg = R.string.general_progress_loading.toCaString()
        )

        log(TAG, INFO) { "importUsb(...):  Checking target device ${device.deviceName}" }
        val result = try {
            usbImporter.import(device) { current, max, info ->
                usbImportState.value = (usbImportState.value as UsbImportstate.Progress).copy(
                    current = current,
                    max = max,
                    progressMsg = info
                )
            }
        } catch (e: CancellationException) {
            usbImportState.value = UsbImportstate.Start()
            throw e
        }

        usbImportState.value = UsbImportstate.Result(
            result = result,
        )
    }.also { usbImportJob = it }

    fun cancelImportUsb() {
        usbImportJob?.cancel()
    }

    data class State(
        val items: List<ImporterAdapter.Item>,
    )

    companion object {
        internal val TAG = logTag("Importer", "Fragment", "VM")
    }
}