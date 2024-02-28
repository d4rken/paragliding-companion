package eu.darken.pgc.flights.core.igc

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.ByteString
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import java.time.LocalDate
import java.time.LocalTime

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
                loggerHardware = "Google Pixel 5 13",
                loggerVersion = "0.9.8.7.1",
            )
            fixes[0] shouldBe IGCParser.BRecord(
                time = LocalTime.of(16, 28, 18),
                location = IGCParser.BRecord.Location(
                    latitude = "4733226N",
                    longitude = "01005399E",
                ),
                validity = 'A',
                pressureAlt = 0,
                gnssAlt = 754,
                extra = "32",
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
                loggerHardware = "CUBOT CUBOT DINOSAUR 6.0",
                loggerVersion = "0.8.2-beta",
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
                loggerHardware = "LXNAV,LX9070PF",
                loggerVersion = "9.0",
            )
        }
    }
}