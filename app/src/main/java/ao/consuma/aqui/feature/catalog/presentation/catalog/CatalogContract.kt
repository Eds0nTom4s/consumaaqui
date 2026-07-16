package ao.consuma.aqui.feature.catalog.presentation.catalog

import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogContentUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiText

enum class CatalogEmptyReason {
    CATALOG_EMPTY,
    CATEGORY_EMPTY,
    SEARCH_EMPTY,
    CATALOG_UNAVAILABLE
}

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data class Content(val data: CatalogContentUiModel) : CatalogUiState
    data class Empty(val data: CatalogContentUiModel?, val reason: CatalogEmptyReason) : CatalogUiState
    data class Error(val message: CatalogUiText, val canRetry: Boolean) : CatalogUiState
    data object CatalogNotFound : CatalogUiState
    data object InvalidMerchant : CatalogUiState
}

sealed interface CatalogUiEvent {
    data object Load : CatalogUiEvent
    data object Refresh : CatalogUiEvent
    data object Retry : CatalogUiEvent
    data class QueryChanged(val value: String) : CatalogUiEvent
    data object ClearQuery : CatalogUiEvent
    data class CategorySelected(val categoryId: String?) : CatalogUiEvent
    data class ProductSelected(val productId: String) : CatalogUiEvent
}

internal object CatalogSavedStateKeys {
    const val MERCHANT_ID = "merchantId"
    const val QUERY = "catalog_query"
    const val CATEGORY_ID = "catalog_category_id"
}
