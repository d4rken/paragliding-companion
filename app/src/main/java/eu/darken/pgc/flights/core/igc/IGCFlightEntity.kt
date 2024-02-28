package eu.darken.pgc.flights.core.igc

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import eu.darken.pgc.flights.core.Flight
import eu.darken.pgc.flights.core.database.FlightEntity
import java.time.Instant

@Entity(tableName = "flights_igc")
data class IGCFlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "flight_id") override val flightId: Flight.Id,
    @ColumnInfo(name = "imported_at") override val importedAt: Instant = Instant.now(),
    @ColumnInfo(name = "checksum_sha1") override val checksumSha1: String,
    @ColumnInfo(name = "flight_source") override val sourceType: Flight.SourceType,
) : FlightEntity