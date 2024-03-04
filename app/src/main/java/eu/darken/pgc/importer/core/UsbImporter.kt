package eu.darken.pgc.importer.core

import android.content.Context
import android.hardware.usb.UsbDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.ca.toCaString
import eu.darken.pgc.common.debug.logging.Logging.Priority.INFO
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.asLog
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import me.jahnen.libaums.core.UsbMassStorageDevice
import me.jahnen.libaums.core.UsbMassStorageDevice.Companion.getMassStorageDevices
import me.jahnen.libaums.core.fs.UsbFile
import me.jahnen.libaums.core.fs.UsbFileInputStream
import okhttp3.internal.closeQuietly
import okio.IOException
import okio.source
import java.io.InputStream
import javax.inject.Inject

class UsbImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crawler: MassStorageCrawler,
    private val ingester: Ingester,
) {

    suspend fun import(device: UsbDevice, onProgress: (Int, Int, CaString) -> Unit): Result =
        device.getMassStorageDevices(context)
            .map { dev ->
                try {
                    dev.init()
                    doImportOnDevice(dev, onProgress)
                } finally {

                    try {
                        dev.close()
                    } catch (e: IOException) {
                        log(TAG, WARN) { "Failed to close $dev, ${e.asLog()}" }
                    }
                }
            }
            .fold(Result()) { cur, next ->
                cur.copy(
                    success = cur.success + next.success,
                    skipped = cur.skipped + next.skipped,
                    failed = cur.failed + next.failed,
                )
            }

    private suspend fun doImportOnDevice(
        device: UsbMassStorageDevice,
        onProgress: (Int, Int, CaString) -> Unit
    ): Result {
        val igcFiles: List<UsbFile> = crawler.crawl(device)
            .filter { it.name.endsWith(".igc") }
            .toList()

        log(TAG, INFO) { "importUsb(...):  ${igcFiles.size} IGC files found!" }

        val success = mutableSetOf<UsbFile>()
        val skipped = mutableSetOf<UsbFile>()
        val failed = mutableSetOf<Pair<UsbFile, Exception>>()
        var current = 0

        igcFiles.forEach { file ->
            if (!currentCoroutineContext().isActive) {
                log(TAG, WARN) { "Context was cancelled!" }
                throw CancellationException()
            }
            log(TAG) { "importUsb(...): Ingesting $file" }
            onProgress(current++, igcFiles.size, file.absolutePath.toCaString())

            val dangles = mutableSetOf<InputStream>()

            try {
                val added = ingester.ingest(
                    IngestIGCPayload(
                        sourceProvider = {
                            UsbFileInputStream(file).also { dangles.add(it) }.source()
                        },
                        originalSource = file.toString(),
                        sourceType = when {
                            device.usbDevice.productName?.lowercase() == "skytraxx" -> IngestIGCPayload.SourceType.SKYTRAXX
                            else -> IngestIGCPayload.SourceType.UNKNOWN
                        },
                    )
                )

                if (added) success.add(file) else skipped.add(file)
            } catch (e: Exception) {
                failed.add(file to e)
            } finally {
                dangles.forEach { it.closeQuietly() }
            }
        }

        return Result(
            success = success,
            skipped = skipped,
            failed = failed,
        )
    }

    data class Result(
        val success: Set<UsbFile> = emptySet(),
        val skipped: Set<UsbFile> = emptySet(),
        val failed: Set<Pair<UsbFile, Exception>> = emptySet(),
    )

    companion object {
        internal val TAG = logTag("Importer", "Usb")
    }
}