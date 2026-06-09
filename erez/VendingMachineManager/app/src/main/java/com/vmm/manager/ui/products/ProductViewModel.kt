package com.vmm.manager.ui.products

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vmm.manager.R
import com.vmm.manager.data.db.entities.Product
import com.vmm.manager.data.repository.ProductRepository
import com.vmm.manager.service.PricingService
import com.vmm.manager.service.PricingSuggestion
import com.vmm.manager.service.WhatsAppProductService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductFormState(
    val name: String = "",
    val barcode: String = "",
    val costPrice: String = "",
    val salePrice: String = "",
    val productionDate: Long? = null,
    val warrantyDate: Long? = null,
    val imageUri: Uri? = null,
    val pricingSuggestion: PricingSuggestion? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class ProductListState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    application: Application,
    private val repository: ProductRepository,
    private val pricingService: PricingService,
    private val whatsAppService: WhatsAppProductService
) : AndroidViewModel(application) {

    private val _listState = MutableStateFlow(ProductListState())
    val listState: StateFlow<ProductListState> = _listState.asStateFlow()

    private val _formState = MutableStateFlow(ProductFormState())
    val formState: StateFlow<ProductFormState> = _formState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    // All user-visible strings resolved from strings.xml (UTF-8 declared)
    private fun str(@StringRes id: Int) = getApplication<Application>().getString(id)
    private fun str(@StringRes id: Int, vararg args: Any) =
        getApplication<Application>().getString(id, *args)

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { q ->
                    if (q.isBlank()) repository.getAllProducts()
                    else repository.searchProducts(q)
                }
                .collect { products -> _listState.update { it.copy(products = products) } }
        }
    }

    fun setSearch(q: String) {
        _searchQuery.value = q
        _listState.update { it.copy(searchQuery = q) }
    }

    fun updateForm(update: ProductFormState.() -> ProductFormState) {
        _formState.update(update)
    }

    fun fetchPricingSuggestion() {
        val form = _formState.value
        if (form.name.isBlank()) return
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            val suggestion = pricingService.getSuggestion(
                productName = form.name,
                barcode     = form.barcode.takeIf { it.isNotBlank() },
                costPrice   = form.costPrice.toDoubleOrNull() ?: 0.0
            )
            _formState.update {
                it.copy(
                    isLoading         = false,
                    pricingSuggestion = suggestion,
                    salePrice         = suggestion?.suggestedSalePrice
                        ?.let { p -> "%.2f".format(p) } ?: it.salePrice
                )
            }
        }
    }

    fun saveProduct() {
        val form = _formState.value
        val cost = form.costPrice.toDoubleOrNull()
        val sale = form.salePrice.toDoubleOrNull()

        if (form.name.isBlank() || cost == null || sale == null) {
            _formState.update { it.copy(errorMessage = str(R.string.err_fill_required)) }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.saveProduct(
                    Product(
                        name           = form.name.trim(),
                        barcode        = form.barcode.trim(),
                        costPrice      = cost,
                        salePrice      = sale,
                        imageUri       = form.imageUri?.toString() ?: "",
                        productionDate = form.productionDate,
                        warrantyDate   = form.warrantyDate
                    )
                )
            }.fold(
                onSuccess = {
                    _formState.update { it.copy(isLoading = false, successMessage = str(R.string.product_saved)) }
                },
                onFailure = { e ->
                    _formState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            )
        }
    }

    fun processIncomingImage(
        uri: Uri, name: String, costPrice: Double, salePrice: Double? = null,
        barcode: String = "", productionDate: Long? = null, warrantyDate: Long? = null
    ) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            whatsAppService.processIncomingProduct(
                WhatsAppProductService.IncomingProductData(
                    imageUri = uri, name = name, costPrice = costPrice, salePrice = salePrice,
                    barcode = barcode, productionDate = productionDate, warrantyDate = warrantyDate
                )
            ).fold(
                onSuccess = { r ->
                    val priceStr = r.suggestedPrice?.let { "%.2f".format(it) } ?: "-"
                    _formState.update {
                        it.copy(
                            isLoading      = false,
                            successMessage = str(R.string.product_created, priceStr)
                        )
                    }
                },
                onFailure = { e ->
                    _formState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            )
        }
    }

    fun deleteProduct(id: Long) { viewModelScope.launch { repository.deleteProduct(id) } }
    fun clearMessages() { _formState.update { it.copy(errorMessage = null, successMessage = null) } }
}
