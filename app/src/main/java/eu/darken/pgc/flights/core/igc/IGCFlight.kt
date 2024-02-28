package eu.darken.pgc.flights.core.igc

import eu.darken.pgc.flights.core.Flight
import java.time.Duration

data class IGCFlight(
    override val id: Flight.Id,
    override val sourceType: Flight.SourceType,
    private val igcFlightEntity: IGCFlightEntity,
) : Flight {
    override val duration: Duration
        get() = Duration.ofHours(3)
    override val location: String
        get() = "Buchenberg"
}
