package ao.consuma.aqui.feature.discovery.presentation.merchant

import ao.consuma.aqui.feature.discovery.presentation.mapper.MerchantOverviewUiModel
import ao.consuma.aqui.feature.discovery.presentation.mapper.UiText

sealed interface MerchantOverviewUiState {
    data object Loading : MerchantOverviewUiState
    data class Content(val merchant: MerchantOverviewUiModel) : MerchantOverviewUiState
    data class OfflineContent(val merchant: MerchantOverviewUiModel) : MerchantOverviewUiState
    data class Error(val message: UiText, val canRetry: Boolean) : MerchantOverviewUiState
    data object NotFound : MerchantOverviewUiState
}
