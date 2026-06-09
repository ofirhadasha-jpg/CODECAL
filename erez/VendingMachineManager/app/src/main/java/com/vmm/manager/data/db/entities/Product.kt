package com.vmm.manager.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "",
    val costPrice: Double,          // מחיר עלות
    val salePrice: Double,          // מחיר מכירה
    val imageUri: String = "",
    val productionDate: Long? = null,   // epoch ms
    val warrantyDate: Long? = null,     // epoch ms
    val vmcProductId: Int = 0,          // ID in VMC
    val stockCount: Int = 0,
    val minStock: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
