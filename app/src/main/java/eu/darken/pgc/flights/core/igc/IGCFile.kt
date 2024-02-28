package eu.darken.pgc.flights.core.igc

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

data class IGCFile(
    val aRecord: ARecord?,
    val header: HRecord?,
    val bRecords: List<BRecord>,
) {
    val manufacturerCode: String?
        get() = aRecord?.manufacturerCode

    val loggerCode: String?
        get() = aRecord?.loggerCode

    fun isValid(): Boolean {
        if (aRecord?.isValid() != true) return false
        if (header?.isValid() != true) return false
        return true
    }

    val launch: BRecord?
        get() = bRecords.firstOrNull { abs(it.altitude - bRecords.first().altitude) >= 5 }

    val landing: BRecord?
        get() = bRecords.lastOrNull()

    val flightDuration: Duration?
        get() = launch?.let { launch ->
            landing?.let { landing ->
                Duration.between(launch.time, landing.time)
            }
        }

    data class ARecord(
        val manufacturerCode: String? = null,
        val loggerCode: String? = null,
        val idExtension: String? = null,
    ) {

        fun isValid() = manufacturerCode != null && loggerCode != null
    }

    data class HRecord(
        val flightDay: LocalDate? = null,
        val flightDayNumber: Int? = null,
        val fixAccuraceMeters: Int? = null,
        val flightSite: String? = null,
        val pilotInCharge: String? = null,
        val gliderType: String? = null,
        val loggerHardware: String? = null,
        val loggerVersion: String? = null,
    ) {

        fun isValid() = flightDay != null
    }

    data class BRecord(
        val time: LocalTime,
        val location: Location,
        val validity: Char,
        val pressureAlt: Int,
        val gnssAlt: Int,
        val extra: String,
    ) {

        val altitude: Int
            get() = if (gnssAlt != 0) gnssAlt else pressureAlt

        data class Location(
            val latitude: String,
            val longitude: String,
        )
    }
}