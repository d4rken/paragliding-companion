package eu.darken.pgc.flights.core.igc

import eu.darken.pgc.flights.core.Flight
import java.time.Duration

data class IGCFlight(
    override val id: Flight.Id,
    override val sourceType: Flight.SourceType,
    private val igcFlightEntity: IGCFlightEntity,
) : Flight {
    override val duration: Duration?
        get() = igcFlightEntity.flightDuration
    override val location: String?
        get() = igcFlightEntity.flightSite
}
