package eu.darken.pgc.importer.core

import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.debug.logging.logTag
import javax.inject.Inject

class Reparser @Inject constructor(
    private val ingester: Ingester,
) {

    suspend fun reparse(onProgress: (Int, Int, CaString) -> Unit): Int {
        var current = 0
        val max = ingester.flightCount()
        return ingester.reingest { progress ->
            onProgress(current++, max, progress)
        }
    }

    companion object {
        internal val TAG = logTag("Importer", "Reparser")
    }
}