package eu.darken.pgc.flights.core.igc

import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.File

object IGCFileTestData {

    private fun String.readAsset() = File(this).readBytes().toByteString()

    val smallXCTrackerFile: ByteString
        get() = "./src/test/assets/testflights/SimpleXCTracker.igc".readAsset()

    val smallModernFile: ByteString
        get() = "./src/test/assets/testflights/SimpleSample2016.igc".readAsset()

    val largeComplexFile: ByteString
        get() = "./src/test/assets/testflights/ComplexXPSoaring.igc".readAsset()

    val smallSkytraxx21: ByteString
        get() = "./src/test/assets/testflights/SmallSkyTraxx2.1.igc".readAsset()
}