package ao.consuma.aqui.feature.search

import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy
import ao.consuma.aqui.feature.discovery.domain.model.FulfillmentOption
import ao.consuma.aqui.feature.discovery.presentation.mapper.*

data class SearchFiltersUiModel(
    val categoryId: String? = null,
    val onlyOpen: Boolean = false,
    val fulfillmentOptions: Set<FulfillmentOption> = emptySet()
) {
    val activeCount: Int get() = (if (categoryId != null) 1 else 0) + (if (onlyOpen) 1 else 0) + fulfillmentOptions.size
}

data class SearchCriteriaUiState(
    val query: String = "",
    val filters: SearchFiltersUiModel = SearchFiltersUiModel(),
    val orderBy: DiscoveryOrderBy = DiscoveryOrderBy.FEATURED,
    val categories: List<CategoryUiModel> = emptyList(),
    val sortOptions: List<SearchSortOptionUiModel> = emptyList(),
    val hasLocation: Boolean = false
)

data class SearchResultsUiModel(
    val criteria: SearchCriteriaUiState,
    val merchants: List<MerchantListItemUiModel>,
    val totalResults: Int,
    val resultContext: UiText,
    val isRefreshing: Boolean,
    val isExplorationMode: Boolean
)

sealed interface SearchUiState {
    data object Loading : SearchUiState
    data class Content(val data: SearchResultsUiModel) : SearchUiState
    data class OfflineContent(val data: SearchResultsUiModel) : SearchUiState
    data class Empty(val criteria: SearchCriteriaUiState, val message: UiText) : SearchUiState
    data class Error(val message: UiText, val canRetry: Boolean) : SearchUiState
}
