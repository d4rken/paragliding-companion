package eu.darken.pgc.flights.core.database

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.pgc.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.flights.core.Flight
import eu.darken.pgc.flights.core.igc.IGCFlightsDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightsDatabase @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val database by lazy {
        Room.databaseBuilder(context, FlightsRoomDb::class.java, "flights").build()
    }

    private val daos: Set<FlightsDao>
        get() = setOf(
            database.flightsIgc()
        )

    suspend fun flightCount(): Int = daos.sumOf { it.flightCount() }

    val flightsIgc: IGCFlightsDao
        get() = database.flightsIgc()

    suspend fun find(id: Flight.Id): FlightEntity? = daos
        .firstNotNullOfOrNull { it.getById(id) }
        .also { log(TAG, VERBOSE) { "find($id) -> it" } }

    suspend fun findBySha1(sha1: String): FlightEntity? = daos
        .firstNotNullOfOrNull { it.getBySha1(sha1) }
        .also { log(TAG, VERBOSE) { "findBySha1($sha1) -> $it" } }

    companion object {
        internal val TAG = logTag("Flights", "Database")
    }
}