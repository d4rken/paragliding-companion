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
import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.ca.toCaString
import eu.darken.pgc.common.coroutine.DispatcherProvider
import eu.darken.pgc.common.debug.logging.Logging.Priority.INFO
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.livedata.SingleLiveEvent
import eu.darken.pgc.common.uix.ViewModel3
import eu.darken.pgc.importer.core.IngestIGCPayload
import eu.darken.pgc.importer.core.Ingester
import eu.darken.pgc.importer.core.MassStorageCrawler
import eu.darken.pgc.importer.core.UsbDevicesProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import me.jahnen.libaums.core.fs.UsbFile
import me.jahnen.libaums.core.fs.UsbFileInputStream
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
    private val massStorageCrawler: MassStorageCrawler,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    val events = SingleLiveEvent<ImporterEvents>()

    private val manualImportState = MutableStateFlow<ManualImportState>(ManualImportState.Start())
    private val reparserState = MutableStateFlow<ReparserState>(ReparserState.Start())
    private val usbImportState = MutableStateFlow<UsbImportstate>(UsbImportstate.Start())

    val state = combine(
        manualImportState,
        reparserState,
        usbImportState,
        usbDevicesProvider.devices,
    ) { manualState, reparserState, usbImportState, usbDevices ->
        ImporterState(
            manualImport = manualState,
            reparserState = reparserState,
            usbImportState = if (usbImportState is UsbImportstate.Start) {
                usbImportState.copy(devices = usbDevices)
            } else {
                usbImportState
            },
        )
    }.asLiveData2()

    fun startSelection() = launch {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        events.postValue(ImporterEvents.ShowPicker(intent))
    }

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
                manualImportState.value = (manualImportState.value as ManualImportState.Progress).copy(
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

                manualImportState.value = (manualImportState.value as ManualImportState.Progress).let {
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
    }

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
    fun importUsb(device: UsbDevice?) = launch {
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

        val success = mutableListOf<UsbFile>()
        val skipped = mutableListOf<UsbFile>()
        val failed = mutableListOf<Pair<UsbFile, Exception>>()

        log(TAG, INFO) { "importUsb(...):  Checking target device ${device.deviceName}" }
        val igcFiles = massStorageCrawler.crawl(device).filter { it.name.endsWith(".igc") }.toList()
        log(TAG, INFO) { "importUsb(...):  ${igcFiles.size} IGC files found!" }

        usbImportState.value = (usbImportState.value as UsbImportstate.Progress).copy(
            max = igcFiles.size
        )

        igcFiles.forEach { file ->
            if (!isActive) {
                usbImportState.value = UsbImportstate.Start()
                throw CancellationException()
            }
            log(TAG) { "importUsb(...): Ingesting $file" }
            usbImportState.value = (usbImportState.value as UsbImportstate.Progress).copy(
                progressMsg = file.absolutePath.toCaString()
            )

            val dangles = mutableSetOf<InputStream>()

            try {
                val added = ingester.ingest(
                    IngestIGCPayload(
                        sourceProvider = {
                            UsbFileInputStream(file).also { dangles.add(it) }.source()
                        },
                        originalSource = file.toString(),
                        sourceType = when {
                            device.productName?.lowercase() == "skytraxx" -> IngestIGCPayload.SourceType.SKYTRAXX
                            else -> IngestIGCPayload.SourceType.UNKNOWN
                        },
                    )
                )

                if (added) success.add(file) else skipped.add(file)
            } catch (e: Exception) {
                failed.add(file to e)
            } finally {
                dangles.forEach { it.closeQuietly() }
            }

            usbImportState.value = (usbImportState.value as UsbImportstate.Progress).let {
                it.copy(current = it.current + 1)
            }
        }

        usbImportState.value = UsbImportstate.Result(
            success = success,
            skipped = skipped,
            failed = failed
        )
    }.also { usbImportJob = it }

    fun cancelImportUsb() {
        usbImportJob?.cancel()
    }

    data class ImporterState(
        val manualImport: ManualImportState = ManualImportState.Start(),
        val reparserState: ReparserState = ReparserState.Start(),
        val usbImportState: UsbImportstate = UsbImportstate.Start(),
    )

    sealed interface ReparserState {
        data class Start(
            val idle: Boolean = true
        ) : ReparserState


        data class Progress(
            val current: Int = 0,
            val max: Int = -1,
            val progressMsg: CaString,
        ) : ReparserState

        data class Result(
            val changes: Int,
        ) : ReparserState
    }

    sealed interface ManualImportState {
        data class Start(
            val idle: Boolean = true
        ) : ManualImportState


        data class Progress(
            val current: Int = 0,
            val max: Int = -1,
            val progressMsg: CaString,
        ) : ManualImportState

        data class Result(
            val success: List<Uri>,
            val skipped: List<Uri>,
            val failed: List<Pair<Uri, Exception>>,
        ) : ManualImportState
    }


    sealed interface UsbImportstate {
        data class Start(
            val devices: Set<UsbDevice> = emptySet()
        ) : UsbImportstate

        data class Progress(
            val current: Int = 0,
            val max: Int = -1,
            val progressMsg: CaString,
        ) : UsbImportstate

        data class Result(
            val success: List<UsbFile>,
            val skipped: List<UsbFile>,
            val failed: List<Pair<UsbFile, Exception>>,
        ) : UsbImportstate
    }

    companion object {
        internal val TAG = logTag("Importer", "Fragment", "VM")
    }
}