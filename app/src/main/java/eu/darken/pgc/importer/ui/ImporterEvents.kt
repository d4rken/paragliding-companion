package eu.darken.pgc.importer.ui

import android.content.Intent
import android.hardware.usb.UsbDevice

sealed interface ImporterEvents {
    data class ShowPicker(val intent: Intent) : ImporterEvents
    data class RequestUsbPermission(val device: UsbDevice) : ImporterEvents
}