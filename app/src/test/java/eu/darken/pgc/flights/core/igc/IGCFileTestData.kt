package eu.darken.pgc.flights.core.igc

import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.File

object IGCFileTestData {

    private fun String.readAsset() = File(this).readBytes().toByteString()

    val smallNormalFile: ByteString
        get() = "./src/test/assets/testflights/SimpleIGC.igc".readAsset()

    val largeComplexFile: ByteString
        get() = "./src/test/assets/testflights/ComplexIGC.igc".readAsset()
}