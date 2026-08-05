package ao.consuma.aqui.feature.search

import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy
import ao.consuma.aqui.feature.discovery.domain.model.FulfillmentOption

sealed interface SearchUiEvent {
    data object Load : SearchUiEvent
    data object Retry : SearchUiEvent
    data object Refresh : SearchUiEvent
    data object LoadNextPage : SearchUiEvent
    data object ClearFilters : SearchUiEvent
    data object ClearQuery : SearchUiEvent
    data class QueryChanged(val value: String) : SearchUiEvent
    data class CategorySelected(val id: String?) : SearchUiEvent
    data class OpenNowChanged(val enabled: Boolean) : SearchUiEvent
    data class FulfillmentToggled(val value: FulfillmentOption) : SearchUiEvent
    data class SortSelected(val value: DiscoveryOrderBy) : SearchUiEvent
    data class MerchantSelected(val merchantId: String) : SearchUiEvent
}
