package com.vmm.manager.data.db.dao

import androidx.room.*
import com.vmm.manager.data.db.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' AND isActive = 1")
    fun search(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stockCount <= minStock AND isActive = 1")
    fun getLowStock(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Query("UPDATE products SET stockCount = stockCount - :qty WHERE id = :id")
    suspend fun decrementStock(id: Long, qty: Int = 1)

    @Query("UPDATE products SET stockCount = :count, updatedAt = :ts WHERE id = :id")
    suspend fun updateStock(id: Long, count: Int, ts: Long = System.currentTimeMillis())

    @Query("UPDATE products SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
