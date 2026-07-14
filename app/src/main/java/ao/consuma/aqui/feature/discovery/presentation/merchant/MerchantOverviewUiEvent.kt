package ao.consuma.aqui.feature.discovery.presentation.merchant

sealed interface MerchantOverviewUiEvent {
    data object Retry : MerchantOverviewUiEvent
    data object OpenCatalog : MerchantOverviewUiEvent
}
