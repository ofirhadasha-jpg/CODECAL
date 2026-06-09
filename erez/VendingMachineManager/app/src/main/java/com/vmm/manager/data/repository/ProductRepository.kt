package com.vmm.manager.data.repository

import com.vmm.manager.data.db.dao.ProductDao
import com.vmm.manager.data.db.entities.Product
import com.vmm.manager.vmc.VMCManager
import com.vmm.manager.vmc.VMCResult
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val dao: ProductDao,
    private val vmcManager: VMCManager
) {
    fun getAllProducts(): Flow<List<Product>> = dao.getAllProducts()
    fun getLowStockProducts(): Flow<List<Product>> = dao.getLowStock()
    fun searchProducts(q: String): Flow<List<Product>> = dao.search(q)

    suspend fun getProduct(id: Long): Product? = dao.getById(id)

    suspend fun saveProduct(product: Product): Long {
        val id = dao.insert(product)
        // Sync to VMC if connected
        if (vmcManager.machineStatus.value.temperature >= 0) {
            syncProductToVMC(product.copy(id = id))
        }
        return id
    }

    suspend fun updateProduct(product: Product) {
        dao.update(product.copy(updatedAt = System.currentTimeMillis()))
        syncProductToVMC(product)
    }

    suspend fun deleteProduct(id: Long) = dao.softDelete(id)

    private suspend fun syncProductToVMC(product: Product, machineId: Byte = 0x00) {
        if (product.vmcProductId <= 0) return
        val priceResult = vmcManager.setProductPrice(
            slotId     = product.vmcProductId,
            priceCents = (product.salePrice * 100).toInt(),
            machineId  = machineId
        )
        if (priceResult is VMCResult.Error)
            Timber.w("VMC price sync failed for product ${product.id}: ${priceResult.message}")
    }

    suspend fun syncStockToVMC(product: Product, slotId: Int, machineId: Byte = 0x00) {
        val result = vmcManager.setStockCount(slotId, product.stockCount, machineId)
        if (result is VMCResult.Error)
            Timber.w("VMC stock sync failed: ${result.message}")
    }
}
