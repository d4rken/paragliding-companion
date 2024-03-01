package eu.darken.pgc.importer.core

import android.hardware.usb.UsbDevice

val UsbDevice.label: String
    get() = (manufacturerName ?: productName) ?: deviceName