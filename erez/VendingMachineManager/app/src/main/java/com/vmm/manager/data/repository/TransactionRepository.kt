package com.vmm.manager.data.repository

import com.vmm.manager.data.db.dao.ProductDao
import com.vmm.manager.data.db.dao.TransactionDao
import com.vmm.manager.data.db.entities.*
import com.vmm.manager.service.NotificationService
import com.vmm.manager.vmc.VMCManager
import com.vmm.manager.vmc.VMCResult
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val txDao: TransactionDao,
    private val productDao: ProductDao,
    private val vmcManager: VMCManager,
    private val notificationService: NotificationService
) {
    fun getRecentTransactions(limit: Int = 50): Flow<List<Transaction>> =
        txDao.getRecentTransactions(limit)

    fun getTransactionsForMachine(machineId: Long): Flow<List<Transaction>> =
        txDao.getTransactionsForMachine(machineId)

    suspend fun getTotalRevenue(from: Long, to: Long): Double =
        txDao.getTotalRevenue(from, to) ?: 0.0

    suspend fun getTransactionCount(from: Long, to: Long): Int =
        txDao.getTransactionCount(from, to)

    /**
     * Full vend flow:
     *  1. Request vend from VMC
     *  2. Wait for approval
     *  3. Confirm / cancel
     *  4. Save transaction
     *  5. Decrement stock
     *  6. Notify customer
     */
    suspend fun processVend(
        machine: Machine,
        slot: MachineSlot,
        product: Product,
        customer: Customer? = null,
        paymentMethod: PaymentMethod = PaymentMethod.CASH
    ): Result<Transaction> = runCatching {
        val priceCents = (slot.price.takeIf { it > 0 } ?: product.salePrice) * 100

        val vendResult = vmcManager.requestVend(
            slotId    = slot.vmcSlotId,
            amount    = priceCents.toInt(),
            machineId = machine.vmcAddress
        )

        if (vendResult is VMCResult.Error || vendResult is VMCResult.Timeout)
            error("Vend request failed: ${(vendResult as? VMCResult.Error)?.message ?: "timeout"}")

        val tx = Transaction(
            machineId     = machine.id,
            productId     = product.id,
            slotId        = slot.slotNumber,
            productName   = product.name,
            unitPrice     = slot.price.takeIf { it > 0 } ?: product.salePrice,
            totalAmount   = slot.price.takeIf { it > 0 } ?: product.salePrice,
            paymentMethod = paymentMethod,
            status        = TransactionStatus.APPROVED,
            customerId    = customer?.id
        )
        val txId = txDao.insert(tx)

        // Confirm dispense
        val confirmResult = vmcManager.confirmVend(slot.vmcSlotId, machine.vmcAddress)
        if (confirmResult is VMCResult.Error) {
            txDao.updateStatus(txId, TransactionStatus.FAILED)
            error("Dispense confirmation failed")
        }

        txDao.updateStatus(txId, TransactionStatus.COMPLETED)
        productDao.decrementStock(product.id)

        customer?.let {
            txDao.recordPurchase(it.id, tx.totalAmount)
            notificationService.notifyCustomerPurchase(it, product.name, tx.totalAmount)
        }

        if (slot.quantity - 1 <= product.minStock) {
            notificationService.notifyLowStock(machine.name, product.name, slot.quantity - 1)
        }

        Timber.i("Vend completed: txId=$txId product=${product.name}")
        tx.copy(id = txId, status = TransactionStatus.COMPLETED)
    }
}
