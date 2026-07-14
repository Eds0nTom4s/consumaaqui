package ao.consuma.aqui.feature.home.presentation

sealed interface HomeUiEvent {
    data object Load : HomeUiEvent
    data object Refresh : HomeUiEvent
    data class SearchChanged(val value: String) : HomeUiEvent
    data class CategorySelected(val categoryId: String?) : HomeUiEvent
    data class MerchantSelected(val merchantId: String) : HomeUiEvent
    data object ViewAll : HomeUiEvent
    data object LocationSelected : HomeUiEvent
    data object Retry : HomeUiEvent
}
