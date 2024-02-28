package eu.darken.pgc.importer.core

import eu.darken.pgc.common.debug.logging.Logging.Priority.ERROR
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.asLog
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.flights.core.Flight
import eu.darken.pgc.flights.core.database.FlightsDatabase
import eu.darken.pgc.flights.core.igc.IGCFlightEntity
import eu.darken.pgc.flights.core.igc.IGCParser
import eu.darken.pgc.flights.core.igc.IGCStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Ingester @Inject constructor(
    private val igcParser: IGCParser,
    private val storage: IGCStorage,
    private val database: FlightsDatabase,
) {

    private val lock = Mutex()

    suspend fun ingest(payload: IngestIGCPayload): Boolean = lock.withLock {
        log(TAG) { "ingest($payload)" }

        val sha1 = payload.file.sha1().base64()
        val existing = database.findBySha1(sha1)
        if (existing != null) {
            log(TAG, WARN) { "Duplicate flight: $existing" }
            return false
        }

        // Parse early, catch invalid files
        val parsed = try {
            igcParser.parse(payload.file)
        } catch (e: Exception) {
            log(TAG, ERROR) { "Parsing failed for $payload: ${e.asLog()}" }
            throw e
        }

        val newId = Flight.Id()

        val idCollision = database.find(newId)
        if (idCollision != null) {
            log(TAG, ERROR) { "We got a UUID collision? wtf: $idCollision" }
            throw IllegalArgumentException("COLLISION: $idCollision")
        }

        val igcFlightEntity = IGCFlightEntity(
            flightId = newId,
            checksumSha1 = sha1,
            sourceType = when (payload.sourceType) {
                IngestIGCPayload.SourceType.XCTRACK -> Flight.SourceType.XCTRACK
                IngestIGCPayload.SourceType.UNKNOWN -> Flight.SourceType.UNKNOWN
            }
        )

        database.flightsIgc.insert(igcFlightEntity)
        storage.add(newId, payload.file)

        return true
    }

    companion object {
        internal val TAG = logTag("Importer", "Ingester")
    }
}