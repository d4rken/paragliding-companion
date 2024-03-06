package eu.darken.pgc.backup.ui.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.pgc.BuildConfig
import eu.darken.pgc.R
import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.ca.caString
import eu.darken.pgc.common.ca.toCaString
import eu.darken.pgc.common.coroutine.DispatcherProvider
import eu.darken.pgc.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.uix.ViewModel3
import eu.darken.pgc.flights.core.FlightRepo
import eu.darken.pgc.flights.core.igc.IGCStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import okio.buffer
import okio.sink
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class LocalBackupFragmentVM @Inject constructor(
    handle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    dispatcherProvider: DispatcherProvider,
    private val flightRepo: FlightRepo,
    private val igcStorage: IGCStorage,
    private val contentResolver: ContentResolver,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    private val currentState = MutableStateFlow<State>(State.Start())

    val state = currentState
        .asLiveData2()


    fun startBackup(path: Uri) = launch {
        var bytesWritten = 0L
        val fileName = "${BuildConfig.APPLICATION_ID}-${BuildConfig.VERSION_CODE}-${System.currentTimeMillis()}.zip"

        currentState.value = State.Progress(progressMsg = R.string.general_progress_loading.toCaString())
        val flights = flightRepo.data.first().flights
        var current = 0

        val file = DocumentFile.fromTreeUri(context, path)!!.createFile("application/x-zip", fileName)!!

        contentResolver.openOutputStream(file.uri)!!.use { os ->
            ZipOutputStream(os).use { zipOut ->
                flights.forEachIndexed { index, flight ->
                    currentState.value = State.Progress(
                        current = current++,
                        max = flights.size,
                        progressMsg = "#$index- ${flight.flightAt?.toString()} - ${flight.location}".toCaString()
                    )
                    val zipEntry = ZipEntry(
                        "${flight.flightAt?.toString()} - ${flight.location} - ${flight.id.value} - .igc"
                    )
                    zipOut.putNextEntry(zipEntry)
                    igcStorage.getRaw(flight.id)!!.use { fSource ->
                        bytesWritten += zipOut.sink().buffer().writeAll(fSource)
                    }
                    zipOut.closeEntry()
                    delay(100)
                    log(TAG, VERBOSE) { "Written: #$index $flight (now $bytesWritten bytes)" }
                }
            }
        }

        log(TAG) { "Created $file ($bytesWritten bytes for ${flights.size} flights)" }

        currentState.value = State.Result(
            resultMsg = caString {
                it.getString(R.string.backup_local_result_success_desc, "$fileName (${bytesWritten}B)")
            }
        )
    }

    sealed interface State {
        data class Start(
            val noop: Boolean = false,
        ) : State

        data class Progress(
            val current: Int = -1,
            val max: Int = -1,
            val progressMsg: CaString
        ) : State

        data class Result(
            val resultMsg: CaString
        ) : State
    }

    companion object {
        internal val TAG = logTag("Backup", "Local", "Fragment ", "VM")
    }
}