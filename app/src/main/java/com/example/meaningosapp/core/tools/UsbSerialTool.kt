package com.example.meaningosapp.core.tools

import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber

class UsbSerialTool(private val context: Context) {

    private var port: UsbSerialPort? = null

    fun connect(): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isEmpty()) {
            return false
        }

        val driver: UsbSerialDriver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device) ?: return false

        port = driver.ports[0]
        port?.open(connection)
        port?.setParameters(
            115200,
            8,
            UsbSerialPort.STOPBITS_1,
            UsbSerialPort.PARITY_NONE
        )

        return true
    }

    fun sendCommand(command: String): Boolean {
        val p = port ?: return false
        val data = (command + "\n").toByteArray()
        p.write(data, 1000)
        return true
    }

    fun readResponse(): String {
        val p = port ?: return ""
        val buffer = ByteArray(128)
        val len = p.read(buffer, 1000)
        return if (len > 0) String(buffer, 0, len) else ""
    }

    fun disconnect() {
        try {
            port?.close()
        } catch (_: Exception) {
        }
        port = null
    }
}
