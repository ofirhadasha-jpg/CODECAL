package com.vmm.manager.vmc.serial

import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.vmm.manager.vmc.protocol.VMCProtocol
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SerialPortManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var port: UsbSerialPort? = null
    private val _incomingBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incomingBytes: SharedFlow<ByteArray> = _incomingBytes

    val isConnected: Boolean get() = port?.isOpen == true

    fun connect(): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val drivers: List<UsbSerialDriver> = UsbSerialProber.getDefaultProber()
            .findAllDrivers(usbManager)

        if (drivers.isEmpty()) {
            Timber.w("No USB serial drivers found")
            return false
        }

        val driver = drivers.first()
        val connection = usbManager.openDevice(driver.device) ?: run {
            Timber.e("USB permission not granted")
            return false
        }

        port = driver.ports.first().also { p ->
            p.open(connection)
            p.setParameters(
                VMCProtocol.BAUD_RATE,
                VMCProtocol.DATA_BITS,
                VMCProtocol.STOP_BITS,
                UsbSerialPort.PARITY_NONE
            )
        }

        Timber.i("Serial port connected @ ${VMCProtocol.BAUD_RATE} baud")
        return true
    }

    fun disconnect() {
        runCatching { port?.close() }
        port = null
        Timber.i("Serial port disconnected")
    }

    suspend fun write(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            port?.write(bytes, VMCProtocol.CMD_TIMEOUT_MS.toInt()) ?: run {
                Timber.e("Port not open for write")
                return@withContext false
            }
            Timber.d("TX [${bytes.size}]: ${bytes.toHexString()}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Serial write failed")
            false
        }
    }

    suspend fun read(timeoutMs: Int = VMCProtocol.CMD_TIMEOUT_MS.toInt()): ByteArray =
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(256)
            try {
                val len = port?.read(buffer, timeoutMs) ?: 0
                val result = buffer.copyOf(len)
                if (result.isNotEmpty()) {
                    Timber.d("RX [${result.size}]: ${result.toHexString()}")
                    _incomingBytes.tryEmit(result)
                }
                result
            } catch (e: Exception) {
                Timber.e(e, "Serial read failed")
                ByteArray(0)
            }
        }

    private fun ByteArray.toHexString() =
        joinToString(" ") { "0x%02X".format(it) }
}
