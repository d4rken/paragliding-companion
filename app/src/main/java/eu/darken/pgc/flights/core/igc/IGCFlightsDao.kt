package eu.darken.pgc.flights.core.igc

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import eu.darken.pgc.flights.core.Flight
import eu.darken.pgc.flights.core.database.FlightsDao
import kotlinx.coroutines.flow.Flow

@Dao
interface IGCFlightsDao : FlightsDao {
    @Query("SELECT * FROM flights_igc")
    suspend fun getAll(): List<IGCFlightEntity>

    @Query("SELECT * FROM flights_igc ")
    fun waterfall(): Flow<List<IGCFlightEntity>>

    @Query("SELECT * FROM flights_igc ORDER BY id DESC LIMIT 1")
    fun firehose(): Flow<IGCFlightEntity?>

    @Query("SELECT * FROM flights_igc WHERE flight_id = :flightId ORDER BY id DESC LIMIT 1")
    fun getLatest(flightId: Flight.Id): Flow<IGCFlightEntity?>

    @Query("SELECT * FROM flights_igc WHERE flight_id = :flightId")
    override fun getById(flightId: Flight.Id): IGCFlightEntity?

    @Query("SELECT * FROM flights_igc WHERE checksum_sha1 = :sha1")
    override fun getBySha1(sha1: String): IGCFlightEntity?

    @Insert
    suspend fun insert(flight: IGCFlightEntity)

    @Update
    suspend fun update(flight: IGCFlightEntity)

    @Query("DELETE FROM flights_igc WHERE flight_id = :flightId")
    suspend fun delete(flightId: Flight.Id)
}