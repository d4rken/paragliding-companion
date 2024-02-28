package eu.darken.pgc.flights.core

import eu.darken.pgc.common.coroutine.AppScope
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.flow.replayingShare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightStats @Inject constructor(
    @AppScope private val scope: CoroutineScope,
    private val repo: FlightRepo
) {
    val data = repo.data
        .map { flights ->
            Stats(
                globalCount = flights.flights.size,
                flightHours = flights.flights.sumOf { it.duration.toMillis() }.let { Duration.ofMillis(it) },
                locations = flights.flights.filter { it.location != null }.distinctBy { it.location }.size,
            )
        }
        .replayingShare(scope)

    data class Stats(
        val globalCount: Int,
        val flightHours: Duration,
        val locations: Int,
    )

    companion object {
        internal val TAG = logTag("Flights", "Stats")
    }
}