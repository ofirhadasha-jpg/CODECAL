package com.vmm.manager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vmm.manager.data.db.dao.MachineDao
import com.vmm.manager.data.db.dao.ProductDao
import com.vmm.manager.data.db.dao.TransactionDao
import com.vmm.manager.data.db.entities.*

@Database(
    entities = [Product::class, Machine::class, MachineSlot::class, Transaction::class, Customer::class],
    version = 1,
    exportSchema = true
)
abstract class VmmDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun machineDao(): MachineDao
    abstract fun transactionDao(): TransactionDao
}
