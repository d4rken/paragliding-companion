package eu.darken.pgc.flights.core.database

import eu.darken.pgc.flights.core.Flight
import java.time.Duration
import java.time.Instant

interface FlightEntity {
    val flightId: Flight.Id
    val importedAt: Instant
    val checksumSha1: String
    val sourceType: Flight.SourceType

    val flightSite: String?
    val flightDuration: Duration?
}