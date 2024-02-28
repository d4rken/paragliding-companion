package eu.darken.pgc.flights.core.igc

import android.content.Context
import eu.darken.pgc.common.debug.logging.Logging.Priority.ERROR
import eu.darken.pgc.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.flights.core.Flight
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IGCStorage @Inject constructor(
    private val context: Context,
    private val igcParser: IGCParser,
) {
    private val storageDir by lazy {
        File(context.filesDir, "storage/igc").also {
            if (it.mkdirs()) log(TAG) { "Created $it" }
        }
    }

    private fun Flight.Id.toStoragePath(): File = File(storageDir, "${this.value}.igc")
    private val lock = Mutex()

    suspend fun add(id: Flight.Id, source: Source) = lock.withLock {
        log(TAG, VERBOSE) { "add($id, $source)..." }
        val path = id.toStoragePath()
        path.sink().buffer().writeAll(source)
        log(TAG, VERBOSE) { "add($id, $source) -> $path" }
    }

    suspend fun getRaw(id: Flight.Id): Source? = lock.withLock {
        log(TAG, VERBOSE) { "get($id)" }
        val path = id.toStoragePath()
        if (!path.exists()) {
            log(TAG, WARN) { "get($id): $path does not exist" }
            return@withLock null
        }
        path.source()
    }

    suspend fun get(id: Flight.Id): IGCFile? {
        log(TAG, VERBOSE) { "get($id)" }
        return getRaw(id)?.let { igcParser.parse(it) }
    }

    suspend fun remove(id: Flight.Id): Boolean = lock.withLock {
        log(TAG, VERBOSE) { "remove($id)" }
        val path = id.toStoragePath()
        if (!path.exists()) {
            log(TAG, WARN) { "remove($id): $path does not exist" }
            return@withLock false
        }
        if (!path.delete()) {
            log(TAG, ERROR) { "remove($id) failed to delete $path" }
        }
        true
    }

    companion object {
        internal val TAG = logTag("Flights", "Storage", "IGC")
    }
}