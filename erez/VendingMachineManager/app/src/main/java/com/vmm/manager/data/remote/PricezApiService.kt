package com.vmm.manager.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class PricezProduct(
    val name: String,
    val barcode: String?,
    val price: Double,
    val store: String,
    val updatedAt: String?
)

data class PricezResponse(
    val results: List<PricezProduct>,
    val total: Int
)

interface PricezApiService {
    @GET("api/search")
    suspend fun searchProduct(
        @Query("q") query: String,
        @Query("barcode") barcode: String? = null
    ): PricezResponse
}
