package ao.consuma.aqui.feature.catalog.presentation.product

import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiText
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductConfigurationErrorUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductDetailUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductPriceSummaryUiModel

data class ProductDetailContentUiModel(
    val product: ProductDetailUiModel,
    val selections: Map<String, Set<String>>,
    val quantity: Int,
    val note: String,
    val validationErrors: List<ProductConfigurationErrorUiModel>,
    val priceSummary: ProductPriceSummaryUiModel,
    val canAdd: Boolean,
    val isOffline: Boolean,
    val mode: ProductDetailMode = ProductDetailMode.ADD,
    val isSubmitting: Boolean = false,
    val conflict: ProductDetailConflictUiModel? = null,
    val configurationAdjusted: Boolean = false
)

enum class ProductDetailMode { ADD, EDIT }

data class ProductDetailConflictUiModel(
    val currentMerchantName: String,
    val requestedMerchantName: String,
    val processing: Boolean
)

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Content(val data: ProductDetailContentUiModel) : ProductDetailUiState
    data class Error(val message: CatalogUiText, val canRetry: Boolean) : ProductDetailUiState
    data object NotFound : ProductDetailUiState
    data object InvalidArguments : ProductDetailUiState
    data class Unavailable(val product: ProductDetailUiModel, val isOffline: Boolean) : ProductDetailUiState
}

sealed interface ProductDetailUiEvent {
    data object Load : ProductDetailUiEvent
    data object Retry : ProductDetailUiEvent
    data class OptionToggled(val groupId: String, val optionId: String) : ProductDetailUiEvent
    data object QuantityIncreased : ProductDetailUiEvent
    data object QuantityDecreased : ProductDetailUiEvent
    data class NoteChanged(val value: String) : ProductDetailUiEvent
    data object Add : ProductDetailUiEvent
    data object KeepCurrentCart : ProductDetailUiEvent
    data object ReplaceCart : ProductDetailUiEvent
}

sealed interface ProductDetailUiEffect {
    data class Success(val messageRes: Int) : ProductDetailUiEffect
    data class Message(val messageRes: Int) : ProductDetailUiEffect
}

internal object ProductDetailSavedStateKeys {
    const val MERCHANT_ID = "merchantId"
    const val PRODUCT_ID = "productId"
    const val CART_ITEM_ID = "cartItemId"
    const val SELECTIONS = "product_selections"
    const val QUANTITY = "product_quantity"
    const val NOTE = "product_note"
    const val VALIDATION_ATTEMPTED = "product_validation_attempted"
}
