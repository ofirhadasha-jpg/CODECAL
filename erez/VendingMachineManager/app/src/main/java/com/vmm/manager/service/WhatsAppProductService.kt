package com.vmm.manager.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.scale
import com.vmm.manager.data.db.dao.ProductDao
import com.vmm.manager.data.db.entities.Product
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes product images received via WhatsApp share intent.
 * Resizes to 800x560, white background, centered product.
 */
@Singleton
class WhatsAppProductService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productDao: ProductDao,
    private val pricingService: PricingService
) {
    companion object {
        const val TARGET_WIDTH  = 800
        const val TARGET_HEIGHT = 560
        const val IMAGE_DIR     = "product_images"
    }

    data class IncomingProductData(
        val imageUri: Uri,
        val name: String,
        val costPrice: Double,
        val salePrice: Double? = null,
        val barcode: String = "",
        val productionDate: Long? = null,
        val warrantyDate: Long? = null
    )

    data class ProductCreationResult(
        val productId: Long,
        val processedImagePath: String,
        val suggestedPrice: Double?,
        val finalSalePrice: Double
    )

    suspend fun processIncomingProduct(data: IncomingProductData): Result<ProductCreationResult> {
        return runCatching {
            val processedPath = processImage(data.imageUri, data.name)
            val suggestion    = pricingService.getSuggestion(data.name, data.barcode, data.costPrice)
            val finalPrice    = data.salePrice
                ?: suggestion?.suggestedSalePrice
                ?: (data.costPrice * 1.30)

            val existing = if (data.barcode.isNotBlank())
                productDao.getByBarcode(data.barcode) else null

            val product = existing?.copy(
                name           = data.name,
                costPrice      = data.costPrice,
                salePrice      = finalPrice,
                imageUri       = processedPath,
                productionDate = data.productionDate ?: existing.productionDate,
                warrantyDate   = data.warrantyDate   ?: existing.warrantyDate,
                updatedAt      = System.currentTimeMillis()
            ) ?: Product(
                name           = data.name,
                barcode        = data.barcode,
                costPrice      = data.costPrice,
                salePrice      = finalPrice,
                imageUri       = processedPath,
                productionDate = data.productionDate,
                warrantyDate   = data.warrantyDate
            )

            val productId = productDao.insert(product)
            Timber.i("Product saved id=$productId name=${data.name} price=$finalPrice")

            ProductCreationResult(
                productId          = productId,
                processedImagePath = processedPath,
                suggestedPrice     = suggestion?.suggestedSalePrice,
                finalSalePrice     = finalPrice
            )
        }
    }

    private fun processImage(uri: Uri, productName: String): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open image URI")   // internal error, no Hebrew needed
        val original = BitmapFactory.decodeStream(inputStream)
            ?: error("Cannot decode image")

        val canvas = Bitmap.createBitmap(TARGET_WIDTH, TARGET_HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(canvas)
        c.drawColor(Color.WHITE)

        val maxW    = (TARGET_WIDTH  * 0.9).toInt()
        val maxH    = (TARGET_HEIGHT * 0.9).toInt()
        val scale   = minOf(maxW.toFloat() / original.width, maxH.toFloat() / original.height)
        val scaledW = (original.width  * scale).toInt()
        val scaledH = (original.height * scale).toInt()
        val scaled  = original.scale(scaledW, scaledH)
        c.drawBitmap(scaled, (TARGET_WIDTH - scaledW) / 2f, (TARGET_HEIGHT - scaledH) / 2f, null)

        val dir  = File(context.filesDir, IMAGE_DIR).also { it.mkdirs() }
        val safe = productName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = File(dir, "${safe}_${System.currentTimeMillis()}.jpg")

        FileOutputStream(file).use { out -> canvas.compress(Bitmap.CompressFormat.JPEG, 90, out) }

        original.recycle(); scaled.recycle(); canvas.recycle()
        return file.absolutePath
    }
}
