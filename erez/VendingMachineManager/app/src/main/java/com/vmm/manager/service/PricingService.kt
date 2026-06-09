package com.vmm.manager.service

import com.vmm.manager.data.remote.PricezApiService
import com.vmm.manager.data.remote.SnaplistApiService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class PricingSuggestion(
    val minMarketPrice: Double,
    val maxMarketPrice: Double,
    val avgMarketPrice: Double,
    val suggestedSalePrice: Double,     // avg + 15% margin
    val sources: List<String>
)

@Singleton
class PricingService @Inject constructor(
    private val pricez: PricezApiService,
    private val snaplist: SnaplistApiService
) {
    suspend fun getSuggestion(
        productName: String,
        barcode: String? = null,
        costPrice: Double = 0.0
    ): PricingSuggestion? {
        val prices = mutableListOf<Double>()
        val sources = mutableListOf<String>()

        try {
            val pricezResult = pricez.searchProduct(productName, barcode)
            pricezResult.results.forEach {
                prices.add(it.price)
                sources.add("Pricez (${it.store})")
            }
        } catch (e: Exception) {
            Timber.w(e, "Pricez API failed")
        }

        try {
            val snapResult = snaplist.getPrices(productName, barcode)
            snapResult.forEach {
                prices.add(it.avgPrice)
                sources.add("Snaplist")
            }
        } catch (e: Exception) {
            Timber.w(e, "Snaplist API failed")
        }

        if (prices.isEmpty()) return null

        val min = prices.min()
        val max = prices.max()
        val avg = prices.average()
        // Suggest: max of (cost + 20%) and (market avg + 15%)
        val suggested = maxOf(costPrice * 1.20, avg * 1.15)

        return PricingSuggestion(
            minMarketPrice = min,
            maxMarketPrice = max,
            avgMarketPrice = avg,
            suggestedSalePrice = Math.round(suggested * 100.0) / 100.0,
            sources = sources.distinct()
        )
    }
}
