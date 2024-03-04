package eu.darken.pgc.importer.core

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.ca.toCaString
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import okhttp3.internal.closeQuietly
import okio.source
import java.io.InputStream
import javax.inject.Inject

class UriImporter @Inject constructor(
    private val contentResolver: ContentResolver,
    private val ingester: Ingester,
) {

    suspend fun import(uris: Collection<Uri>, onProgress: (Int, Int, CaString) -> Unit): Result {

        val success = mutableListOf<Uri>()
        val skipped = mutableListOf<Uri>()
        val failed = mutableListOf<Pair<Uri, Exception>>()

        val validUris = uris
            .filter { MimeTypeMap.getFileExtensionFromUrl(it.path!!) == "igc" }
        var current = 0
        validUris.forEach { uri ->
            log(TAG) { "import(...): $uri" }
            onProgress(current++, validUris.size, (uri.path ?: uri.toString()).toCaString())

            val dangles = mutableSetOf<InputStream>()

            try {
                val added = ingester.ingest(
                    IngestIGCPayload(
                        sourceProvider = {
                            contentResolver.openInputStream(uri)!!.also { dangles.add(it) }.source()
                        },
                        originalSource = uri.toString(),
                        sourceType = when {
                            uri.authority?.contains("org.xcontest.XCTrack") == true -> IngestIGCPayload.SourceType.XCTRACK
                            else -> IngestIGCPayload.SourceType.UNKNOWN
                        },
                    )
                )

                if (added) success.add(uri) else skipped.add(uri)
            } catch (e: Exception) {
                failed.add(uri to e)
            } finally {
                dangles.forEach { it.closeQuietly() }
            }
        }

        (uris - (success + skipped).toSet()).forEach {
            failed.add(it to IllegalArgumentException("Unknown data type"))
        }

        return Result(success, skipped, failed)
    }

    data class Result(
        val success: List<Uri>,
        val skipped: List<Uri>,
        val failed: List<Pair<Uri, Exception>>,
    )


    companion object {
        internal val TAG = logTag("Importer", "Uri")
    }
}