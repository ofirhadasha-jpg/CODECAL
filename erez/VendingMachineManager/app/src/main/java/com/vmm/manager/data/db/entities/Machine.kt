package com.vmm.manager.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MachineType { MASTER, SLAVE, STANDALONE }
enum class MachineStatus { ONLINE, OFFLINE, ERROR, MAINTENANCE }

@Entity(tableName = "machines")
data class Machine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serialNumber: String,
    val name: String,
    val location: String = "",
    val machineType: MachineType = MachineType.STANDALONE,
    val masterId: Long? = null,         // for SLAVE type
    val vmcAddress: Byte = 0x00,        // RS232 address
    val status: MachineStatus = MachineStatus.OFFLINE,
    val temperature: Float = 0f,
    val doorOpen: Boolean = false,
    val microwaveActive: Boolean = false,
    val lastSyncAt: Long? = null,
    val totalSlots: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "machine_slots",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Machine::class,
            parentColumns = ["id"],
            childColumns = ["machineId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        ),
        androidx.room.ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = androidx.room.ForeignKey.SET_NULL
        )
    ],
    indices = [
        androidx.room.Index("machineId"),
        androidx.room.Index("productId")
    ]
)
data class MachineSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val machineId: Long,
    val slotNumber: Int,
    val productId: Long? = null,
    val quantity: Int = 0,
    val maxQuantity: Int = 10,
    val price: Double = 0.0,        // overrides product.salePrice if set
    val vmcSlotId: Int = 0
)
