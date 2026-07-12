package ao.consuma.aqui.feature.onboarding

import androidx.lifecycle.ViewModel
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appLaunchStateRepository: AppLaunchStateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.Continue -> advancePage()
            is OnboardingEvent.PageChanged -> updatePage(event.page)
            is OnboardingEvent.Skip,
            is OnboardingEvent.Finish -> completeOnboarding()
        }
    }

    private fun advancePage() {
        _uiState.update { current ->
            val nextPage = (current.currentPage + 1).coerceAtMost(current.totalPages - 1)
            current.copy(currentPage = nextPage)
        }
    }

    private fun updatePage(page: Int) {
        _uiState.update { current ->
            current.copy(currentPage = page.coerceIn(0, current.totalPages - 1))
        }
    }

    private fun completeOnboarding() {
        appLaunchStateRepository.completeOnboarding()
    }
}
