package com.vmm.manager.data.db.dao

import androidx.room.*
import com.vmm.manager.data.db.entities.Customer
import com.vmm.manager.data.db.entities.Transaction
import com.vmm.manager.data.db.entities.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 50): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE machineId = :machineId ORDER BY createdAt DESC")
    fun getTransactionsForMachine(machineId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE createdAt BETWEEN :from AND :to ORDER BY createdAt DESC")
    fun getTransactionsInRange(from: Long, to: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(totalAmount) FROM transactions WHERE status = 'COMPLETED' AND createdAt BETWEEN :from AND :to")
    suspend fun getTotalRevenue(from: Long, to: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'COMPLETED' AND createdAt BETWEEN :from AND :to")
    suspend fun getTransactionCount(from: Long, to: Long): Int

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Query("UPDATE transactions SET status = :status, completedAt = :ts WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TransactionStatus, ts: Long = System.currentTimeMillis())

    // Customers
    @Query("SELECT * FROM customers ORDER BY totalPurchases DESC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Query("UPDATE customers SET totalPurchases = totalPurchases + 1, totalSpent = totalSpent + :amount, lastPurchaseAt = :ts WHERE id = :id")
    suspend fun recordPurchase(id: Long, amount: Double, ts: Long = System.currentTimeMillis())
}
