package com.vmm.manager.vmc

import com.vmm.manager.vmc.protocol.VMCFrame
import com.vmm.manager.vmc.protocol.VMCProtocol
import com.vmm.manager.vmc.serial.SerialPortManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed class VMCResult {
    data class Success(val frame: VMCFrame) : VMCResult()
    data class Error(val message: String, val code: Byte? = null) : VMCResult()
    object Timeout : VMCResult()
}

data class MachineStatus(
    val temperature: Float = 0f,
    val doorOpen: Boolean = false,
    val microwaveActive: Boolean = false,
    val outOfStock: Boolean = false,
    val errorCode: Byte? = null
)

@Singleton
class VMCManager @Inject constructor(
    private val serial: SerialPortManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null

    private val _machineStatus = MutableStateFlow(MachineStatus())
    val machineStatus: StateFlow<MachineStatus> = _machineStatus

    private val _events = MutableSharedFlow<VMCEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<VMCEvent> = _events

    // ── Connection ──────────────────────────────────────────────────────────

    fun connect(): Boolean {
        val connected = serial.connect()
        if (connected) startPolling()
        return connected
    }

    fun disconnect() {
        stopPolling()
        serial.disconnect()
    }

    // ── POLL Loop ───────────────────────────────────────────────────────────

    private fun startPolling() {
        pollJob = scope.launch {
            while (isActive) {
                sendPoll()
                delay(VMCProtocol.POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun sendPoll() {
        serial.write(byteArrayOf(VMCProtocol.POLL))
        val response = serial.read(VMCProtocol.ACK_TIMEOUT_MS.toInt())
        if (response.isNotEmpty()) handleIncoming(response)
    }

    private fun handleIncoming(raw: ByteArray) {
        when {
            raw.size == 1 && raw[0] == VMCProtocol.ACK -> Unit // normal ACK
            else -> VMCFrame.parse(raw)?.let { frame ->
                Timber.d("Received frame CMD=0x%02X".format(frame.command))
                processStatusFrame(frame)
                _events.tryEmit(VMCEvent.FrameReceived(frame))
            }
        }
    }

    private fun processStatusFrame(frame: VMCFrame) {
        when (frame.command) {
            VMCProtocol.Status.GET_TEMPERATURE ->
                _machineStatus.update {
                    it.copy(temperature = frame.data.firstOrNull()?.toFloat() ?: it.temperature)
                }
            VMCProtocol.Status.GET_DOOR_STATUS ->
                _machineStatus.update { it.copy(doorOpen = frame.data.firstOrNull() == 0x01.toByte()) }
            VMCProtocol.Status.GET_MICROWAVE_STATUS ->
                _machineStatus.update { it.copy(microwaveActive = frame.data.firstOrNull() == 0x01.toByte()) }
        }
    }

    // ── Command Execution ───────────────────────────────────────────────────

    suspend fun sendCommand(frame: VMCFrame, retries: Int = VMCProtocol.MAX_RETRIES): VMCResult {
        repeat(retries) { attempt ->
            val sent = serial.write(frame.toBytes())
            if (!sent) {
                Timber.w("Send failed attempt ${attempt + 1}")
                return@repeat
            }

            val ack = serial.read(VMCProtocol.ACK_TIMEOUT_MS.toInt())
            if (ack.isEmpty() || ack[0] != VMCProtocol.ACK) {
                Timber.w("No ACK on attempt ${attempt + 1}")
                delay(100)
                return@repeat
            }

            val response = serial.read(VMCProtocol.CMD_TIMEOUT_MS.toInt())
            if (response.isEmpty()) return VMCResult.Timeout

            val responseFrame = VMCFrame.parse(response)
                ?: return VMCResult.Error("Invalid frame response")

            serial.write(byteArrayOf(VMCProtocol.ACK))
            return VMCResult.Success(responseFrame)
        }
        return VMCResult.Error("Command failed after $retries retries")
    }

    // ── Payment Commands ────────────────────────────────────────────────────

    suspend fun resetPayment(machineId: Byte = 0x00) =
        sendCommand(VMCFrame(VMCProtocol.Payment.RESET, machineId = machineId))

    suspend fun enableReader(machineId: Byte = 0x00) =
        sendCommand(VMCFrame(VMCProtocol.Payment.READER_ENABLE, machineId = machineId))

    suspend fun disableReader(machineId: Byte = 0x00) =
        sendCommand(VMCFrame(VMCProtocol.Payment.READER_DISABLE, machineId = machineId))

    suspend fun requestVend(slotId: Int, amount: Int, machineId: Byte = 0x00): VMCResult {
        val data = byteArrayOf(
            (slotId shr 8).toByte(), slotId.toByte(),
            (amount shr 8).toByte(), amount.toByte()
        )
        return sendCommand(VMCFrame(VMCProtocol.Payment.VEND, data, machineId))
    }

    suspend fun confirmVend(slotId: Int, machineId: Byte = 0x00): VMCResult {
        val data = byteArrayOf((slotId shr 8).toByte(), slotId.toByte())
        return sendCommand(VMCFrame(VMCProtocol.Dispense.VEND_SUCCESS, data, machineId))
    }

    suspend fun cancelVend(machineId: Byte = 0x00) =
        sendCommand(VMCFrame(VMCProtocol.Payment.CANCEL_VEND, machineId = machineId))

    // ── Inventory Commands ──────────────────────────────────────────────────

    suspend fun setProductInfo(slotId: Int, productId: Int, name: String, machineId: Byte = 0x00): VMCResult {
        val nameBytes = name.take(16).toByteArray(Charsets.UTF_8)
        val data = byteArrayOf(
            (slotId shr 8).toByte(), slotId.toByte(),
            (productId shr 8).toByte(), productId.toByte(),
            nameBytes.size.toByte(), *nameBytes
        )
        return sendCommand(VMCFrame(VMCProtocol.Inventory.SET_PRODUCT_INFO, data, machineId))
    }

    suspend fun setProductPrice(slotId: Int, priceCents: Int, machineId: Byte = 0x00): VMCResult {
        val data = byteArrayOf(
            (slotId shr 8).toByte(), slotId.toByte(),
            (priceCents shr 8).toByte(), priceCents.toByte()
        )
        return sendCommand(VMCFrame(VMCProtocol.Inventory.SET_PRODUCT_PRICE, data, machineId))
    }

    suspend fun setStockCount(slotId: Int, count: Int, machineId: Byte = 0x00): VMCResult {
        val data = byteArrayOf(
            (slotId shr 8).toByte(), slotId.toByte(),
            count.toByte()
        )
        return sendCommand(VMCFrame(VMCProtocol.Inventory.SET_STOCK_COUNT, data, machineId))
    }

    suspend fun getStockCount(slotId: Int, machineId: Byte = 0x00) =
        sendCommand(VMCFrame(VMCProtocol.Inventory.GET_STOCK_COUNT,
            byteArrayOf((slotId shr 8).toByte(), slotId.toByte()), machineId))

    suspend fun syncInventory(machineId: Byte = 0x00) =
        sendCommand(VMCFrame(VMCProtocol.Inventory.SYNC_INVENTORY, machineId = machineId))

    // ── Status Commands ─────────────────────────────────────────────────────

    suspend fun getMachineStatus(machineId: Byte = 0x00) =
        sendCommand(VMCFrame(VMCProtocol.Status.GET_MACHINE_STATUS, machineId = machineId))

    suspend fun getTemperature(machineId: Byte = 0x00) =
        sendCommand(VMCFrame(VMCProtocol.Status.GET_TEMPERATURE, machineId = machineId))

    suspend fun getDoorStatus(machineId: Byte = 0x00) =
        sendCommand(VMCFrame(VMCProtocol.Status.GET_DOOR_STATUS, machineId = machineId))

    suspend fun setMasterSlave(isMaster: Boolean, slaveIds: List<Byte>, machineId: Byte = 0x00): VMCResult {
        val data = byteArrayOf(
            if (isMaster) 0x01 else 0x00,
            slaveIds.size.toByte(),
            *slaveIds.toByteArray()
        )
        return sendCommand(VMCFrame(VMCProtocol.Status.SET_MASTER_SLAVE, data, machineId))
    }

    private fun List<Byte>.toByteArray() = ByteArray(size) { this[it] }
}

sealed class VMCEvent {
    data class FrameReceived(val frame: VMCFrame) : VMCEvent()
    data class VendApproved(val slotId: Int) : VMCEvent()
    data class VendDenied(val slotId: Int, val reason: String) : VMCEvent()
    data class StockAlert(val slotId: Int, val remaining: Int) : VMCEvent()
    data class DoorOpened(val machineId: Byte) : VMCEvent()
    data class TemperatureAlert(val temp: Float, val machineId: Byte) : VMCEvent()
    data class MachineError(val code: Byte, val machineId: Byte) : VMCEvent()
}
