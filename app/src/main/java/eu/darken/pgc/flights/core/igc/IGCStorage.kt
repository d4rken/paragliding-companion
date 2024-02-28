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
import okio.ByteString
import okio.ByteString.Companion.toByteString
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

    suspend fun add(id: Flight.Id, raw: ByteString) = lock.withLock {
        log(TAG, VERBOSE) { "add($id, $raw)..." }
        val path = id.toStoragePath()
        path.writeBytes(raw.toByteArray())
        log(TAG, VERBOSE) { "add($id, $raw) -> $path" }
    }

    suspend fun get(id: Flight.Id): IGCFile? = lock.withLock {
        log(TAG, VERBOSE) { "get($id)" }
        val path = id.toStoragePath()
        if (!path.exists()) {
            log(TAG, WARN) { "get($id): $path does not exist" }
            return@withLock null
        }
        igcParser.parse(path.readBytes().toByteString())
    }

    suspend fun remove(id: Flight.Id): IGCFile? = lock.withLock {
        log(TAG, VERBOSE) { "remove($id)" }
        val path = id.toStoragePath()
        if (!path.exists()) {
            log(TAG, WARN) { "remove($id): $path does not exist" }
            return@withLock null
        }
        val raw = path.readBytes().toByteString()
        if (!path.delete()) {
            log(TAG, ERROR) { "remove($id) failed to delete $path" }
        }
        igcParser.parse(raw)
    }

    companion object {
        internal val TAG = logTag("Flights", "Storage", "IGC")
    }
}