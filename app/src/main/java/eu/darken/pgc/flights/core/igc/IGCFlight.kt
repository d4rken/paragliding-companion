package eu.darken.pgc.flights.core.igc

import eu.darken.pgc.flights.core.Flight
import java.time.Duration
import java.time.OffsetDateTime

data class IGCFlight(
    override val id: Flight.Id,
    private val entity: IGCFlightEntity,
) : Flight {

    override val flightAt: OffsetDateTime?
        get() = entity.flightAt
    override val location: String?
        get() = entity.flightSite

    override val distance: Long?
        get() = entity.flightDistance
    override val duration: Duration?
        get() = entity.flightDuration

    override val gliderType: String?
        get() = entity.gliderType
    override val trackerType: String?
        get() = entity.loggerInfo
}
