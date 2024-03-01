package eu.darken.pgc.importer.ui

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.pgc.R
import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.ca.toCaString
import eu.darken.pgc.common.coroutine.DispatcherProvider
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.livedata.SingleLiveEvent
import eu.darken.pgc.common.uix.ViewModel3
import eu.darken.pgc.importer.core.IngestIGCPayload
import eu.darken.pgc.importer.core.Ingester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import okhttp3.internal.closeQuietly
import okio.buffer
import okio.source
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ImporterFragmentVM @Inject constructor(
    handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val ingester: Ingester,
    private val contentResolver: ContentResolver,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    val events = SingleLiveEvent<ImporterEvents>()

    private val manualImportState = MutableStateFlow<ManualImportState>(ManualImportState.Start())
    private val reparserState = MutableStateFlow<ReparserState>(ReparserState.Start())

    val state = combine(
        manualImportState,
        reparserState,
    ) { manualState, reparserState ->
        ImporterState(
            manualImport = manualState,
            reparserState = reparserState
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

    fun startIngestion(uris: Set<Uri>) = launch {
        log(TAG) { "startIngestions(uris=${uris.size})" }
        manualImportState.value = ManualImportState.Progress(
            max = uris.size,
            progressMsg = R.string.general_progress_loading.toCaString()
        )

        val success = mutableListOf<Uri>()
        val skipped = mutableListOf<Uri>()
        val failed = mutableListOf<Pair<Uri, Exception>>()

        val done = mutableSetOf<Uri>()

        uris
            .filter { !done.contains(it) }
            .filter { MimeTypeMap.getFileExtensionFromUrl(it.path!!) == "igc" }
            .forEach { uri ->
                manualImportState.value = (manualImportState.value as ManualImportState.Progress).copy(
                    progressMsg = (uri.lastPathSegment ?: uri.toString()).toCaString()
                )

                val dangles = mutableSetOf<InputStream>()

                try {
                    val added = contentResolver.openInputStream(uri)!!.use { inputStream ->
                        inputStream.source().buffer().readByteString()
                        ingester.ingest(
                            IngestIGCPayload(
                                sourceProvider = {
                                    contentResolver.openInputStream(uri)!!
                                        .also { dangles.add(it) }
                                        .source()
                                },
                                originalSource = uri.toString(),
                                sourceType = when {
                                    uri.authority?.contains("org.xcontest.XCTrack") == true -> IngestIGCPayload.SourceType.XCTRACK
                                    else -> IngestIGCPayload.SourceType.UNKNOWN
                                },
                            )
                        )
                    }


                    if (added) success.add(uri) else skipped.add(uri)
                } catch (e: Exception) {
                    failed.add(uri to e)
                } finally {
                    dangles.forEach { it.closeQuietly() }
                }

                done.add(uri)

                manualImportState.value = (manualImportState.value as ManualImportState.Progress).let {
                    it.copy(current = it.current + 1)
                }
            }

        uris
            .filter { !done.contains(it) }
            .forEach {
                done.add(it)
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

    data class ImporterState(
        val manualImport: ManualImportState = ManualImportState.Start(),
        val reparserState: ReparserState = ReparserState.Start()
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

    companion object {
        internal val TAG = logTag("Importer", "Fragment", "VM")
    }
}