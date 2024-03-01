package eu.darken.pgc.flights.core.igc

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import eu.darken.pgc.flights.core.Flight
import eu.darken.pgc.flights.core.database.FlightEntity
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.roundToInt

@Entity(tableName = "flights_igc")
data class IGCFlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "flight_id") override val flightId: Flight.Id,
    @ColumnInfo(name = "imported_at") override val importedAt: Instant,
    @ColumnInfo(name = "checksum_sha1") override val checksumSha1: String,
    @ColumnInfo(name = "flight_at") override val flightAt: OffsetDateTime?,
    @ColumnInfo(name = "flight_site") override val flightSite: String?,
    @ColumnInfo(name = "flight_duration") override val flightDuration: Duration?,
    @ColumnInfo(name = "flight_distance") override val flightDistance: Long?,
    @ColumnInfo(name = "glider_type") override val gliderType: String?,
    @ColumnInfo(name = "logger_info") override val loggerInfo: String?,
) : FlightEntity

fun IGCFile.toFlightEntity(
    id: Long = 0,
    flightId: Flight.Id,
    importedAt: Instant = Instant.now(),
    checksumSha1: String,
) = IGCFlightEntity(
    id = id,
    flightId = flightId,
    importedAt = importedAt,
    checksumSha1 = checksumSha1,
    flightAt = (header?.flightDay ?: LocalDate.EPOCH).let { day ->
        bRecords.firstOrNull()?.time?.let { time ->
            val offset = ZoneOffset.ofTotalSeconds(((header?.timezoneOffset ?: 0f) * 3600).roundToInt())
            day.atTime(time.atOffset(offset))
        }
    },
    flightSite = header?.flightSite?.takeIf { it != "?" },
    flightDuration = flightDuration,
    flightDistance = null,
    gliderType = header?.gliderType,
    loggerInfo = when (aRecord?.manufacturerCode) {
        "XCT" -> "XCTrack (${header?.loggerFirmwareVersion}) @ ${header?.loggerType}"
        "XSX" -> "SKYTRAXX ${header?.loggerHardwareVersion} (${header?.loggerFirmwareVersion})"
        else -> "${header?.loggerType} (${header?.loggerFirmwareVersion}) @ ${header?.loggerHardwareVersion}"
    },
)