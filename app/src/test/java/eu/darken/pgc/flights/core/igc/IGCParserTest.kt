package eu.darken.pgc.flights.core.igc

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class IGCParserTest : BaseTest() {

    private fun create() = IGCParser()

    @Test
    fun `parse normal`() = runTest {
        val igcFile = create().parse(IGCFileTestData.smallNormalFile)
        igcFile.apply {
            aRecord shouldBe IGCParser.ARecord(
                manufacturerCode = "XCT",
                loggerCode = "3fe",
                idExtension = "9347b5065a2cd",
            )
        }
    }

    @Test
    fun `parse complex`() = runTest {
        val igcFile = create().parse(IGCFileTestData.largeComplexFile)
        igcFile.apply {
            aRecord shouldBe IGCParser.ARecord(
                manufacturerCode = "LXV",
                loggerCode = "6MS",
                idExtension = "FLIGHT:1",
            )
        }
    }
}