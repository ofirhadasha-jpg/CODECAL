package com.vmm.manager.vmc.protocol

/**
 * Represents a single VMC protocol frame.
 * Structure: [STX][LEN][CMD][DATA...][CHK][ETX]
 * CHK = XOR of LEN+CMD+DATA bytes.
 */
data class VMCFrame(
    val command: Byte,
    val data: ByteArray = ByteArray(0),
    val machineId: Byte = 0x00
) {
    fun toBytes(): ByteArray {
        val payload = ByteArray(2 + data.size)
        payload[0] = machineId
        payload[1] = command
        data.copyInto(payload, 2)

        val len = payload.size.toByte()
        val chk = checksum(len, payload)

        return byteArrayOf(VMCProtocol.STX, len, *payload, chk, VMCProtocol.ETX)
    }

    override fun equals(other: Any?) = other is VMCFrame &&
        command == other.command && data.contentEquals(other.data)

    override fun hashCode() = 31 * command.hashCode() + data.contentHashCode()

    companion object {
        fun checksum(len: Byte, payload: ByteArray): Byte {
            var xor = len.toInt()
            payload.forEach { xor = xor xor it.toInt() }
            return (xor and 0xFF).toByte()
        }

        fun parse(raw: ByteArray): VMCFrame? {
            if (raw.size < 5) return null
            if (raw[0] != VMCProtocol.STX) return null
            if (raw.last() != VMCProtocol.ETX) return null

            val len = raw[1].toInt() and 0xFF
            if (raw.size < len + 4) return null

            val payload = raw.slice(2 until 2 + len).toByteArray()
            val receivedChk = raw[2 + len]
            val calcChk = checksum(raw[1], payload)
            if (receivedChk != calcChk) return null

            val machineId = payload[0]
            val command   = payload[1]
            val data      = payload.drop(2).toByteArray()
            return VMCFrame(command, data, machineId)
        }
    }
}
