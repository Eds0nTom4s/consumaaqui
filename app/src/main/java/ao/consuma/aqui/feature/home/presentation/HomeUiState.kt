package ao.consuma.aqui.feature.home.presentation

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val location: LocationUiModel?,
        val categories: List<CategoryUiModel>,
        val selectedCategoryId: String?,
        val nearbyMerchants: List<MerchantCompactUiModel>,
        val recommendedMerchants: List<MerchantCompactUiModel>,
        val featuredMerchants: List<MerchantCompactUiModel>,
        val query: String,
        val isRefreshing: Boolean,
        val isOffline: Boolean
    ) : HomeUiState

    data class Empty(
        val location: LocationUiModel?,
        val categories: List<CategoryUiModel>,
        val query: String,
        val selectedCategoryId: String?
    ) : HomeUiState

    data class Error(val message: UiText, val canRetry: Boolean) : HomeUiState
}
