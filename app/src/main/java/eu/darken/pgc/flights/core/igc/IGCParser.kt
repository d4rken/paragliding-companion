package eu.darken.pgc.flights.core.igc

import dagger.Reusable
import eu.darken.pgc.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import okio.ByteString
import javax.inject.Inject

@Reusable
class IGCParser @Inject constructor() {

    suspend fun parse(raw: ByteString): IGCFile {
        log(TAG) { "ingest(${raw.size} bytes)" }
        log(TAG, VERBOSE) { "ingest(...): ${raw.substring(0, 100).utf8()}" }

        var aRecord: ARecord? = null

        raw
            .utf8()
            .lineSequence()
            .filterNot { it.isBlank() }
            .forEachIndexed { index, line ->
                when {
                    line.isARecord() -> {
                        aRecord = line.parseARecord()
                    }

                    else -> {
                        log(TAG, WARN) { "Unknown line (#$index): $line" }
                    }
                }
            }

        return IGCFile(
            aRecord = aRecord,
        )
    }

    private fun String.isARecord(): Boolean = first().uppercaseChar() == 'A'

    data class ARecord(
        val manufacturerCode: String,
        val loggerCode: String,
        val idExtension: String? = null,
    )

    private fun String.parseARecord() = ARecord(
        manufacturerCode = drop(1).take(3),
        loggerCode = drop(4).take(3),
        idExtension = drop(7).takeIf { it.isNotBlank() },
    )

    companion object {
        internal val TAG = logTag("Flights", "Ingest", "Parser", "IGC")
    }
}