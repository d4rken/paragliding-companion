package eu.darken.pgc.importer.core

import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.ca.toCaString
import eu.darken.pgc.common.debug.logging.Logging.Priority.ERROR
import eu.darken.pgc.common.debug.logging.Logging.Priority.INFO
import eu.darken.pgc.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.asLog
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.flights.core.Flight
import eu.darken.pgc.flights.core.database.FlightsDatabase
import eu.darken.pgc.flights.core.igc.IGCException
import eu.darken.pgc.flights.core.igc.IGCParser
import eu.darken.pgc.flights.core.igc.IGCStorage
import eu.darken.pgc.flights.core.igc.toFlightEntity
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import okio.Source
import okio.buffer
import java.security.MessageDigest
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Ingester @Inject constructor(
    private val igcParser: IGCParser,
    private val igcStorage: IGCStorage,
    private val database: FlightsDatabase,
) {

    private val lock = Mutex()

    suspend fun ingest(payload: IngestIGCPayload): Boolean = lock.withLock {
        log(TAG) { "ingest($payload)" }
        val start = System.currentTimeMillis()

        val sha1 = payload.sourceProvider().use { it.toFlightChecksum() }
        val existing = database.findBySha1(sha1)
        if (existing != null) {
            log(TAG, WARN) { "Duplicate flight: $existing" }
            return false
        }

        // Parse early, catch invalid files
        val parsed = payload.sourceProvider().use { it.parseAsIGC() }

        val newId = Flight.Id()

        val idCollision = database.find(newId)
        if (idCollision != null) {
            log(TAG, ERROR) { "We got a UUID collision? wtf: $idCollision" }
            throw IllegalArgumentException("COLLISION: $idCollision")
        }

        val extraFlightDay = if (parsed.header?.flightDay == null) {
            log(TAG, WARN) { "ingest(...) Incomplete header, trying to get flight day from file name/path" }
            when (payload.sourceType) {
                IngestIGCPayload.SourceType.XCTRACK -> {
                    payload.originalPath.split("/").lastOrNull()
                        ?.let { Regex("(\\d{4})-(\\d{2})-(\\d{2})-\\w{3}-\\w{3}-(\\d*)").matchAt(it, 0) }
                        ?.let {
                            LocalDate.of(
                                it.groupValues[1].toInt(),
                                it.groupValues[2].toInt(),
                                it.groupValues[3].toInt()
                            )
                        }
                        ?.also { log(TAG, WARN) { "ingest(...) Flight day from file name/path -> $it" } }
                }

                IngestIGCPayload.SourceType.SKYTRAXX -> null // TODO
                IngestIGCPayload.SourceType.UNKNOWN -> null
            }
        } else null

        val igcFlightEntity = parsed.toFlightEntity(
            flightId = newId,
            checksumSha1 = sha1,
            flightAtExra = extraFlightDay,
        )

        database.flightsIgc.insert(igcFlightEntity)
        payload.sourceProvider().use { igcStorage.add(newId, it) }

        val stop = System.currentTimeMillis()

        log(TAG) { "ingest(...) took ${stop - start}ms" }
        return true
    }

    suspend fun reingest(onProgress: (CaString) -> Unit) = lock.withLock {
        log(TAG) { "reingest()" }
        val igcFlights = database.flightsIgc.getAll()

        var changes = 0
        igcFlights.forEachIndexed { index, flight ->
            log(TAG, VERBOSE) { "Reingesting: $index: $flight" }
            onProgress(flight.flightAt.toString().toCaString())
            val igcRaw = igcStorage.getRaw(flight.flightId)!!

            // Parse early, catch invalid files
            val parsed = igcRaw.parseAsIGC()

            withContext(NonCancellable) {
                val oldEntity = database.flightsIgc.getById(flight.flightId)!!
                val newEntity = parsed.toFlightEntity(
                    id = oldEntity.id,
                    flightId = oldEntity.flightId,
                    importedAt = oldEntity.importedAt,
                    checksumSha1 = oldEntity.checksumSha1,
                    flightAtExra = oldEntity.flightAt?.toLocalDate(),
                )
                if (oldEntity != newEntity) {
                    log(TAG, INFO) { "Before (#$index): $oldEntity" }
                    log(TAG, INFO) { "After  (#$index): $newEntity" }
                    changes++
                }
                database.flightsIgc.update(newEntity)
            }
        }
        changes
    }

    private fun Source.toFlightChecksum(): String = MessageDigest.getInstance("SHA-1")
        .let { md ->
            buffer().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } > 0) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest()
        }
        .let { it.toByteString(0, it.size) }
        .base64()

    private suspend fun Source.parseAsIGC() = try {
        igcParser.parse(this)
    } catch (e: Exception) {
        log(TAG, ERROR) { "Parsing failed for $this: ${e.asLog()}" }
        throw IGCException(cause = e, "Parsing failed.")
    }

    suspend fun flightCount(): Int = database.flightCount()

    companion object {
        internal val TAG = logTag("Importer", "Ingester")
    }
}