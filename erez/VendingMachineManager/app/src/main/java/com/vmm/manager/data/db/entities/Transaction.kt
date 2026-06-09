package com.vmm.manager.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentMethod { CASH, CARD, NFC, APP }
enum class TransactionStatus { PENDING, APPROVED, COMPLETED, FAILED, REFUNDED }

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val machineId: Long,
    val productId: Long,
    val slotId: Int,
    val productName: String,
    val quantity: Int = 1,
    val unitPrice: Double,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val customerId: Long? = null,
    val vmcTransactionId: String = "",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val fcmToken: String = "",
    val totalPurchases: Int = 0,
    val totalSpent: Double = 0.0,
    val registeredAt: Long = System.currentTimeMillis(),
    val lastPurchaseAt: Long? = null,
    val smsEnabled: Boolean = true,
    val pushEnabled: Boolean = true
)
