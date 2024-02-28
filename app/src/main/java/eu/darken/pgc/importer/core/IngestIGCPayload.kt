package eu.darken.pgc.importer.core

import okio.ByteString

data class IngestIGCPayload(
    val file: ByteString,
    val sourceType: SourceType,
    val originalSource: String,
) {
    enum class SourceType {
        XCTRACK,
        UNKNOWN,
    }
}
