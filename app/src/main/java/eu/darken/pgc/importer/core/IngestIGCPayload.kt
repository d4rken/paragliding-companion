package eu.darken.pgc.importer.core

import okio.Source

data class IngestIGCPayload(
    val sourceProvider: suspend () -> Source,
    val sourceType: SourceType,
    val originalPath: String,
) {
    enum class SourceType {
        XCTRACK,
        SKYTRAXX,
        UNKNOWN,
    }
}
