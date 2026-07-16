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
    val isOffline: Boolean
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
}

internal object ProductDetailSavedStateKeys {
    const val MERCHANT_ID = "merchantId"
    const val PRODUCT_ID = "productId"
    const val SELECTIONS = "product_selections"
    const val QUANTITY = "product_quantity"
    const val NOTE = "product_note"
    const val VALIDATION_ATTEMPTED = "product_validation_attempted"
}
