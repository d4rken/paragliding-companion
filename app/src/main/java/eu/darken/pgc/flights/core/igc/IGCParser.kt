package eu.darken.pgc.flights.core.igc

import dagger.Reusable
import eu.darken.pgc.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import okio.Source
import okio.buffer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@Reusable
class IGCParser @Inject constructor() {

    suspend fun parse(raw: Source): IGCFile {
        log(TAG) { "parse(${raw} bytes)" }
        val start = System.currentTimeMillis()

        val rawLines = raw.buffer().readUtf8().lineSequence().filterValid()

        val aRecord = ARecord.parse(rawLines)
        val hRecord = HRecord.parse(rawLines)

        val stop = System.currentTimeMillis()
        log(TAG, VERBOSE) { "parse(...) took ${stop - start}ms" }

        return IGCFile(
            aRecord = aRecord,
            header = hRecord,
        )
    }

    private fun String.isLineValid(index: Int): Boolean {
        val line = this
        if (line.isBlank()) {
            log(TAG, WARN) { "Blank line at #$index" }
            return false
        }
        if (line.length > 76) {
            log(TAG, WARN) { "Oversized line (${line.length}) at #$index" }
            return false
        }
        return true
    }

    private fun Sequence<String>.filterValid(): Sequence<String> = this
        .filterIndexed { index, line -> line.isLineValid(index) }

    data class ARecord(
        val manufacturerCode: String? = null,
        val loggerCode: String? = null,
        val idExtension: String? = null,
    ) {

        fun isValid() = manufacturerCode != null && loggerCode != null

        companion object {
            fun parse(lines: Sequence<String>): ARecord? {
                val line = lines.singleOrNull { it.first().uppercaseChar() == 'A' } ?: return null
                return ARecord(
                    manufacturerCode = line.drop(1).take(3),
                    loggerCode = line.drop(4).take(3),
                    idExtension = line.drop(7).takeIf { it.isNotBlank() },
                )
            }
        }
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

        companion object {
            fun parse(lines: Sequence<String>): HRecord? {
                val hlines = lines.filter { it.first().uppercaseChar() == 'H' }.toList()
                if (hlines.isEmpty()) return null

                var recordedAt: LocalDate? = null
                var flightDayNumber: Int? = null

                val dateR = Regex("^(?:HFDTE|HFDTEDATE:)(\\w{6}),?(\\d+)?$")
                hlines.firstNotNullOfOrNull { dateR.matchEntire(it) }?.let { match ->
                    recordedAt = LocalDate.parse(match.groupValues[1], DateTimeFormatter.ofPattern("ddMMyy"))
                    flightDayNumber = match.groups[2]?.value?.toInt()
                }

                val fixAccuracyR = Regex("^HFFXA(\\d+?)$")
                val fixAccuraceMeters: Int? = hlines
                    .firstNotNullOfOrNull { fixAccuracyR.matchEntire(it) }
                    ?.let { match -> match.groupValues[1].toInt() }

                val flightSiteR = Regex("^HOSITSite:(.+)$", RegexOption.IGNORE_CASE)
                val flightSite: String? = hlines
                    .firstNotNullOfOrNull { flightSiteR.matchEntire(it) }
                    ?.let { match -> match.groupValues[1] }

                val pilotInChargeR = Regex("^(?:HFPLTPILOT|HFPLTPILOTINCHARGE):(.+)$", RegexOption.IGNORE_CASE)
                val pilotInCharge: String? = hlines
                    .firstNotNullOfOrNull { pilotInChargeR.matchEntire(it) }
                    ?.let { match -> match.groupValues[1] }

                val glideTypeP = Regex("^HFGTYGLIDERTYPE:(.+?)$")
                val glidertype: String? = hlines
                    .firstNotNullOfOrNull { glideTypeP.matchEntire(it) }
                    ?.let { match -> match.groupValues[1] }

                val loggerHardwareR = Regex("^HFFTYFRTYPE:(.+?)$")
                val loggerHardware: String? = hlines
                    .firstNotNullOfOrNull { loggerHardwareR.matchEntire(it) }
                    ?.let { match -> match.groupValues[1] }

                val loggerVersionR = Regex("^HFRFWFIRMWAREVERSION:(.+?)$")
                val loggerVersion: String? = hlines
                    .firstNotNullOfOrNull { loggerVersionR.matchEntire(it) }
                    ?.let { match -> match.groupValues[1] }

                return HRecord(
                    flightDay = recordedAt,
                    flightDayNumber = flightDayNumber,
                    fixAccuraceMeters = fixAccuraceMeters,
                    flightSite = flightSite,
                    pilotInCharge = pilotInCharge,
                    gliderType = glidertype,
                    loggerHardware = loggerHardware,
                    loggerVersion = loggerVersion,
                )
            }
        }
    }

    companion object {
        internal val TAG = logTag("Flights", "Ingest", "Parser", "IGC")
    }
}