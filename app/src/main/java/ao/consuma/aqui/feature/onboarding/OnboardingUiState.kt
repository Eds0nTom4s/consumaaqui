package ao.consuma.aqui.feature.onboarding

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 3
) {
    val isLastPage: Boolean = currentPage == totalPages - 1
}
