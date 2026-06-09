package com.vmm.manager.vmc.protocol

/**
 * VMC Protocol constants per SDK specification.
 * RS232 @ 57600 baud, 8N1.
 * Frame: [STX][LEN][CMD][DATA...][CHK]
 */
object VMCProtocol {

    // Frame delimiters
    const val STX: Byte = 0x02
    const val ETX: Byte = 0x03
    const val ACK: Byte = 0x06.toByte()
    const val NAK: Byte = 0x15.toByte()
    const val POLL: Byte = 0x10.toByte()

    // ── Dispensing Commands ──────────────────────────────────────────────────
    object Dispense {
        const val VEND_REQUEST: Byte         = 0x01
        const val VEND_CANCEL: Byte          = 0x03
        const val VEND_SUCCESS: Byte         = 0x04
        const val VEND_FAILURE: Byte         = 0x05
        const val SESSION_COMPLETE: Byte     = 0x06
    }

    // ── Inventory / Product Commands ────────────────────────────────────────
    object Inventory {
        const val SET_PRODUCT_INFO: Byte     = 0x11
        const val GET_PRODUCT_INFO: Byte     = 0x12
        const val SET_PRODUCT_PRICE: Byte    = 0x13
        const val GET_PRODUCT_PRICE: Byte    = 0x14
        const val SET_STOCK_COUNT: Byte      = 0x15
        const val GET_STOCK_COUNT: Byte      = 0x16
        const val SYNC_INVENTORY: Byte       = 0x17
    }

    // ── Payment System Commands ─────────────────────────────────────────────
    object Payment {
        const val RESET: Byte                = 0x21.toByte()
        const val SETUP: Byte                = 0x23.toByte()
        const val STATUS: Byte               = 0x24.toByte()
        const val VEND: Byte                 = 0x25.toByte()
        const val READER_ENABLE: Byte        = 0x26.toByte()
        const val CANCEL_VEND: Byte          = 0x27.toByte()
        const val READER_DISABLE: Byte       = 0x28.toByte()
    }

    // ── Machine Status Commands ─────────────────────────────────────────────
    object Status {
        const val GET_MACHINE_STATUS: Byte   = 0x30.toByte()
        const val GET_TEMPERATURE: Byte      = 0x31.toByte()
        const val GET_DOOR_STATUS: Byte      = 0x32.toByte()
        const val GET_MICROWAVE_STATUS: Byte = 0x33.toByte()
        const val SET_MASTER_SLAVE: Byte     = 0x40.toByte()
        const val GET_SLAVE_LIST: Byte       = 0x41.toByte()
    }

    // ── Response Codes ──────────────────────────────────────────────────────
    object Response {
        const val OK: Byte                   = 0x00
        const val BUSY: Byte                 = 0x01
        const val ERROR: Byte                = 0x02.toByte()
        const val VEND_APPROVED: Byte        = 0x05
        const val VEND_DENIED: Byte          = 0x06
        const val OUT_OF_STOCK: Byte         = 0x07
        const val INVALID_SELECTION: Byte    = 0x08.toByte()
    }

    // ── Serial Config ───────────────────────────────────────────────────────
    const val BAUD_RATE = 57600
    const val DATA_BITS = 8
    const val STOP_BITS = 1
    const val PARITY   = 0    // None

    const val POLL_INTERVAL_MS = 200L
    const val ACK_TIMEOUT_MS   = 500L
    const val CMD_TIMEOUT_MS   = 2000L
    const val MAX_RETRIES       = 3
}
