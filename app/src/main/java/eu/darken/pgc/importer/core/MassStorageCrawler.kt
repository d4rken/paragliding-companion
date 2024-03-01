package eu.darken.pgc.importer.core

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.pgc.common.debug.logging.Logging.Priority.ERROR
import eu.darken.pgc.common.debug.logging.Logging.Priority.INFO
import eu.darken.pgc.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.asLog
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import me.jahnen.libaums.core.UsbMassStorageDevice.Companion.getMassStorageDevices
import me.jahnen.libaums.core.fs.UsbFile
import java.io.IOException
import java.util.LinkedList
import javax.inject.Inject

class MassStorageCrawler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbManager: UsbManager,
) {

    fun crawl(device: UsbDevice): Flow<UsbFile> = flow {
        device.getMassStorageDevices(context).forEach { storageDevice ->
            log(TAG, INFO) { "Checking storage device $storageDevice" }
            try {
                storageDevice.init()
            } catch (e: Exception) {
                log(TAG, WARN) { "init failed ${e.asLog()}" }
                return@forEach
            }
            storageDevice.partitions.forEach { partition ->
                log(TAG, INFO) { "Checking partition ${partition.volumeLabel}" }

                partition.fileSystem.rootDirectory.listFiles().forEach { rootFolder ->
                    log(TAG) { "Walking rootDirectory $rootFolder" }
                    UsbFileWalker(
                        start = rootFolder,
                        onFilter = { !SKIP_DIRS.contains(it.name) }
                    ).collect { emit(it) }
                }
            }
        }
    }

    class UsbFileWalker constructor(
        private val start: UsbFile,
        private val onFilter: suspend (UsbFile) -> Boolean = { true },
        private val onError: suspend (UsbFile, Exception) -> Boolean = { _, _ -> true },
    ) : AbstractFlow<UsbFile>() {
        private val tag = "$TAG#${hashCode()}"

        override suspend fun collectSafely(collector: FlowCollector<UsbFile>) {
            if (!start.isDirectory) {
                collector.emit(start)
                return
            }

            val queue = LinkedList(listOf(start))

            while (!queue.isEmpty()) {

                val lookUp = queue.removeFirst()

                val newBatch = try {
                    lookUp.listFiles().toList()
                } catch (e: IOException) {
                    log(TAG, ERROR) { "Failed to read $lookUp: $e" }
                    if (onError(lookUp, e)) {
                        emptyList()
                    } else {
                        throw e
                    }
                }

                newBatch
                    .filter {
                        val allowed = onFilter(it)
                        if (!allowed) log(tag, VERBOSE) { "Skipping (filter): $it" }
                        allowed
                    }
                    .forEach { child ->
                        if (child.isDirectory) {
                            log(tag, VERBOSE) { "Walking: $child" }
                            queue.addFirst(child)
                        }
                        log(tag, VERBOSE) { "Emitting: $child" }
                        collector.emit(child)
                    }
            }
        }
    }

    companion object {
        private val SKIP_DIRS = setOf(
            "LOST.DIR",
            ".Trash-1000",
        )
        private val TAG = logTag("Usb", "MassStorageCrawler")
    }
}