package com.vmm.manager.data.db.dao

import androidx.room.*
import com.vmm.manager.data.db.entities.Machine
import com.vmm.manager.data.db.entities.MachineSlot
import com.vmm.manager.data.db.entities.MachineStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineDao {
    @Query("SELECT * FROM machines ORDER BY name ASC")
    fun getAllMachines(): Flow<List<Machine>>

    @Query("SELECT * FROM machines WHERE id = :id")
    suspend fun getMachineById(id: Long): Machine?

    @Query("SELECT * FROM machines WHERE status = :status")
    fun getMachinesByStatus(status: MachineStatus): Flow<List<Machine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachine(machine: Machine): Long

    @Update
    suspend fun updateMachine(machine: Machine)

    @Query("UPDATE machines SET status = :status, lastSyncAt = :ts WHERE id = :id")
    suspend fun updateStatus(id: Long, status: MachineStatus, ts: Long = System.currentTimeMillis())

    @Query("UPDATE machines SET temperature = :temp WHERE id = :id")
    suspend fun updateTemperature(id: Long, temp: Float)

    // Slots
    @Query("SELECT * FROM machine_slots WHERE machineId = :machineId ORDER BY slotNumber ASC")
    fun getSlotsForMachine(machineId: Long): Flow<List<MachineSlot>>

    @Query("SELECT * FROM machine_slots WHERE machineId = :machineId AND slotNumber = :slot LIMIT 1")
    suspend fun getSlot(machineId: Long, slot: Int): MachineSlot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: MachineSlot): Long

    @Update
    suspend fun updateSlot(slot: MachineSlot)

    @Query("UPDATE machine_slots SET quantity = :qty WHERE machineId = :machineId AND slotNumber = :slot")
    suspend fun updateSlotQuantity(machineId: Long, slot: Int, qty: Int)
}
