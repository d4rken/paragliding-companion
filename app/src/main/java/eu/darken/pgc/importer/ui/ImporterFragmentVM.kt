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

    private val progressHolder = MutableStateFlow<State.Progress?>(null)
    private val resultHolder = MutableStateFlow<State.Result?>(null)

    val state = combine(
        progressHolder,
        resultHolder,
    ) { progress, result ->
        (progress ?: result) ?: State.Start()
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
        progressHolder.value = State.Progress(
            max = uris.size,
            progressMsg = R.string.general_progress_loading.toCaString()
        )
        resultHolder.value = null

        val success = mutableListOf<Uri>()
        val skipped = mutableListOf<Uri>()
        val failed = mutableListOf<Pair<Uri, Exception>>()

        val done = mutableSetOf<Uri>()

        uris
            .filter { !done.contains(it) }
            .filter { MimeTypeMap.getFileExtensionFromUrl(it.path!!) == "igc" }
            .forEach { uri ->
                progressHolder.value = progressHolder.value?.copy(
                    progressMsg = (uri.lastPathSegment ?: uri.toString()).toCaString()
                )

                val dangles = mutableSetOf<InputStream>()

                try {
                    val added = contentResolver.openInputStream(uri)!!.use { inputStream ->
                        inputStream.source().buffer().readByteString()
                        ingester.ingest(
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
                    }


                    if (added) success.add(uri) else skipped.add(uri)
                } catch (e: Exception) {
                    failed.add(uri to e)
                } finally {
                    dangles.forEach { it.closeQuietly() }
                }

                done.add(uri)

                progressHolder.value = progressHolder.value?.let {
                    it.copy(current = it.current + 1)
                }
            }

        uris
            .filter { !done.contains(it) }
            .forEach {
                done.add(it)
                failed.add(it to IllegalArgumentException("Unknown data type"))
            }

        resultHolder.value = State.Result(
            success = success,
            skipped = skipped,
            failed = failed
        )
        progressHolder.value = null
    }

    fun reparse() = launch {
        log(TAG) { "reparse()" }
        ingester.reingest()
    }

    sealed interface State {
        data class Start(
            val idle: Boolean = true
        ) : State


        data class Progress(
            val current: Int = 0,
            val max: Int = -1,
            val progressMsg: CaString,
        ) : State

        data class Result(
            val success: List<Uri>,
            val skipped: List<Uri>,
            val failed: List<Pair<Uri, Exception>>,
        ) : State
    }

    companion object {
        internal val TAG = logTag("Importer", "Fragment", "VM")
    }
}