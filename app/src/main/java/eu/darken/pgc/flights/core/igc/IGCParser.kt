package eu.darken.pgc.flights.core.igc

import dagger.Reusable
import eu.darken.pgc.common.debug.logging.Logging.Priority.ERROR
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.asLog
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import okio.Source
import okio.buffer
import java.time.Instant
import javax.inject.Inject

@Reusable
class IGCParser @Inject constructor() {

    suspend fun parse(raw: Source): IGCFile {
        log(TAG) { "ingest(${raw} bytes)" }

        var aRecord = ARecord()
        var headerRecord = HRecord()

        val rawText = raw.buffer().readUtf8().lineSequence()

        rawText
            .forEachIndexed { index, line ->
                if (line.isBlank()) {
                    log(TAG, WARN) { "Blank line at #$index" }
                    return@forEachIndexed
                }
                if (line.length > 76) {
                    log(TAG, WARN) { "Oversized line (${line.length}) at #$index" }
                    return@forEachIndexed
                }

                when (line.first().uppercaseChar()) {
                    ARecord.PREFIX -> {
                        aRecord = try {
                            ARecord.parse(line)
                        } catch (e: Exception) {
                            log(TAG, ERROR) { "failed to parse ARecord: ${e.asLog()}" }
                            aRecord
                        }
                    }

                    HRecord.PREFIX -> {
                        if (headerRecord == null) headerRecord = HRecord()

                    }

                    else -> {
                        log(TAG, WARN) { "Unknown prefix (#$index): $line" }
                    }
                }
            }

        return IGCFile(
            aRecord = aRecord,
        )
    }

    data class ARecord(
        val manufacturerCode: String? = null,
        val loggerCode: String? = null,
        val idExtension: String? = null,
    ) {

        fun isValid() = manufacturerCode != null && loggerCode != null

        companion object {
            const val PREFIX = 'A'
            fun parse(line: String) = line.run {
                ARecord(
                    manufacturerCode = drop(1).take(3),
                    loggerCode = drop(4).take(3),
                    idExtension = drop(7).takeIf { it.isNotBlank() },
                )
            }
        }
    }

    class HRecord(
        val recordedAt: Instant? = null,
        val fixAccuraceMeters: Int? = null,
    ) {
        companion object {
            const val PREFIX = 'H'
            fun parse(line: String) = line.run {
                HRecord(
//                    manufacturerCode = drop(1).take(3),
//                    loggerCode = drop(4).take(3),
//                    idExtension = drop(7).takeIf { it.isNotBlank() },
                )
            }
        }
    }

    companion object {
        internal val TAG = logTag("Flights", "Ingest", "Parser", "IGC")
    }
}