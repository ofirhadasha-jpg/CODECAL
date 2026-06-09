package com.vmm.manager.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class SnaplistPrice(
    val productName: String,
    val barcode: String?,
    val minPrice: Double,
    val maxPrice: Double,
    val avgPrice: Double,
    val retailerCount: Int
)

interface SnaplistApiService {
    @GET("api/v1/prices")
    suspend fun getPrices(
        @Query("name") name: String? = null,
        @Query("barcode") barcode: String? = null
    ): List<SnaplistPrice>
}
