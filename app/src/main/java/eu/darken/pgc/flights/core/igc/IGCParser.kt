package eu.darken.pgc.flights.core.igc

import dagger.Reusable
import eu.darken.pgc.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import okio.Source
import okio.buffer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@Reusable
class IGCParser @Inject constructor() {

    suspend fun parse(raw: Source): IGCFile {
        log(TAG) { "parse(${raw} bytes)" }
        val start = System.currentTimeMillis()

        val rawLines = raw.buffer().use {
            it.readUtf8().lineSequence().filterValid()
        }

        val aRecord = parseARecord(rawLines)
        val hRecord = parseHRecord(rawLines)

        val bRecords = parseBRecords(rawLines)

        val stop = System.currentTimeMillis()
        log(TAG, VERBOSE) { "parse(...) took ${stop - start}ms" }

        return IGCFile(
            aRecord = aRecord,
            header = hRecord,
            bRecords = bRecords,
        )
    }

    private fun String.isLineValid(index: Int): Boolean {
        val line = this
        if (line.isBlank()) {
            log(TAG, WARN) { "Blank line at #$index" }
            return false
        }
        if (line.length > 76) log(TAG, WARN) { "Oversized line (${line.length}) at #$index" }

        return true
    }

    private fun Sequence<String>.filterValid(): Sequence<String> = this
        .filterIndexed { index, line -> line.isLineValid(index) }

    private fun parseARecord(lines: Sequence<String>): IGCFile.ARecord? {
        val aRecordR = Regex("^A(\\w{3})(\\w{3})(.+?)$")
        val match = lines.firstNotNullOfOrNull { aRecordR.matchEntire(it) }
        return match?.let { result ->
            IGCFile.ARecord(
                manufacturerCode = result.groups[1]?.value,
                loggerCode = result.groups[2]?.value,
                idExtension = result.groups[3]?.value,
            )
        }
    }

    private fun parseHRecord(lines: Sequence<String>): IGCFile.HRecord? {
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

        return IGCFile.HRecord(
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

    private fun parseBRecords(lines: Sequence<String>): List<IGCFile.BRecord> {
        val regex = Regex("^B(\\w{6})(\\w{7}[NS])(\\w{8}[EW])(\\w)(\\d{5})(\\d{5})(.+)$")
        return lines
            .mapNotNull { regex.matchEntire(it) }
            .map { match ->
                IGCFile.BRecord(
                    time = LocalTime.parse(match.groups[1]!!.value, DateTimeFormatter.ofPattern("HHmmss")),
                    location = IGCFile.BRecord.Location(
                        latitude = match.groups[2]!!.value,
                        longitude = match.groups[3]!!.value,
                    ),
                    validity = match.groups[4]!!.value.single(),
                    pressureAlt = match.groups[5]!!.value.toInt(),
                    gnssAlt = match.groups[6]!!.value.toInt(),
                    extra = match.groups[7]!!.value,
                )
            }
            .toList()
    }

    companion object {
        internal val TAG = logTag("Flights", "Ingest", "Parser", "IGC")
    }
}