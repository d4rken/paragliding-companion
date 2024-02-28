package eu.darken.pgc.flights.core.igc

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.ByteString
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import java.time.LocalDate

class IGCParserTest : BaseTest() {

    private fun create() = IGCParser()

    private suspend fun doParse(data: ByteString) = Buffer().write(data).use { source ->
        create().parse(source)
    }

    @Test
    fun `parse normal`() = runTest {
        val igcFile = doParse(IGCFileTestData.smallXCTrackerFile)
        igcFile.apply {
            aRecord shouldBe IGCParser.ARecord(
                manufacturerCode = "XCT",
                loggerCode = "3fe",
                idExtension = "9347b5065a2cd",
            )
            header shouldBe IGCParser.HRecord(
                flightDay = LocalDate.of(2023, 6, 9),
                flightDayNumber = 4,
                fixAccuraceMeters = null,
                flightSite = "?",
                pilotInCharge = "John Doe",
                gliderType = "ADVANCE Alpha 7",
            )
        }
    }

    @Test
    fun `parse modern`() = runTest {
        val igcFile = doParse(IGCFileTestData.smallModernFile)
        igcFile.apply {
            aRecord shouldBe IGCParser.ARecord(
                manufacturerCode = "XCT",
                loggerCode = "7ce",
                idExtension = "a4d3ae0df42a1",
            )
            header shouldBe IGCParser.HRecord(
                flightDay = LocalDate.of(2019, 3, 9),
                flightDayNumber = 1,
                fixAccuraceMeters = null,
                flightSite = "Norma",
                pilotInCharge = "Jane Doe",
                gliderType = "OZONE Zeno",
            )
        }
    }

    @Test
    fun `parse complex`() = runTest {
        val igcFile = doParse(IGCFileTestData.largeComplexFile)
        igcFile.apply {
            aRecord shouldBe IGCParser.ARecord(
                manufacturerCode = "LXV",
                loggerCode = "6MS",
                idExtension = "FLIGHT:1",
            )
            header shouldBe IGCParser.HRecord(
                flightDay = LocalDate.of(2022, 8, 5),
                flightDayNumber = null,
                fixAccuraceMeters = 15,
                pilotInCharge = "Max Mustermann",
                gliderType = "Ventus 3T",
            )
        }
    }
}