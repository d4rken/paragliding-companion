package eu.darken.pgc.importer.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.pgc.common.coroutine.AppScope
import eu.darken.pgc.common.debug.logging.Logging.Priority.ERROR
import eu.darken.pgc.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.pgc.common.debug.logging.Logging.Priority.WARN
import eu.darken.pgc.common.debug.logging.asLog
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.flow.replayingShare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbDevicesProvider @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    private val usbManager: UsbManager,
) {

    val devices: SharedFlow<Set<UsbDevice>> = callbackFlow {
        fun callbackRefresh() {
            appScope.launch {
                val devices = usbManager.deviceList.values.toSet()
                log(TAG) { "Current devices $devices" }
                send(devices)
            }
        }

        callbackRefresh()

        val eventReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                log(TAG) { "onReceive($context,$intent)" }
                callbackRefresh()
            }
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        try {
            ContextCompat.registerReceiver(context, eventReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to register usbReceiver: ${e.asLog()}" }
        }

        awaitClose {
            log(TAG, VERBOSE) { "unregisterReceiver($eventReceiver)" }
            try {
                context.unregisterReceiver(eventReceiver)
            } catch (e: Exception) {
                log(TAG, WARN) { "Failed to unregister event receiver $eventReceiver" }
            }
        }
    }
        .distinctUntilChanged()
        .replayingShare(scope = appScope)

    companion object {
        private val TAG = logTag("Usb", "UsbManager2")
    }
}