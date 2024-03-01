package eu.darken.pgc.flights.core.database

import eu.darken.pgc.flights.core.Flight
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

interface FlightEntity {
    val flightId: Flight.Id
    val importedAt: Instant
    val checksumSha1: String

    val flightAt: OffsetDateTime?
    val flightSite: String?
    val flightDuration: Duration?
    val flightDistance: Long?
    val gliderType: String?
    val loggerInfo: String?
}