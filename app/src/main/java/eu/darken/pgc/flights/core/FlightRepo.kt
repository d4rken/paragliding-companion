package eu.darken.pgc.flights.core

import eu.darken.pgc.common.coroutine.AppScope
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.flow.replayingShare
import eu.darken.pgc.flights.core.database.FlightsDatabase
import eu.darken.pgc.flights.core.igc.IGCFlight
import eu.darken.pgc.flights.core.igc.IGCStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightRepo @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val storage: IGCStorage,
    private val database: FlightsDatabase,
) {

    val data = database.flightsIgc.waterfall()
        .map { flightEntities ->
            Flights(
                flights = flightEntities.map { entity ->
                    IGCFlight(
                        id = entity.flightId,
                        entity = entity,
                    )
                }.toSet()
            )
        }
        .replayingShare(appScope)

    data class Flights(
        val flights: Set<Flight> = emptySet()
    )

    companion object {
        internal val TAG = logTag("Flights", "Repo")
    }
}