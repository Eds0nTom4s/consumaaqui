package ao.consuma.aqui.feature.checkout.presentation.confirmation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.feature.checkout.domain.repository.CheckoutRepository
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutConfirmationUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CheckoutConfirmationUiState {
    data object Loading : CheckoutConfirmationUiState
    data class Content(val confirmation: CheckoutConfirmationUiModel) : CheckoutConfirmationUiState
    data object Missing : CheckoutConfirmationUiState
}

@HiltViewModel
class CheckoutConfirmationViewModel @Inject constructor(
    repository: CheckoutRepository,
    mapper: CheckoutUiMapper
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<CheckoutConfirmationUiState>(
        repository.draft.value?.let { CheckoutConfirmationUiState.Content(mapper.confirmation(it)) }
            ?: CheckoutConfirmationUiState.Loading
    )
    val uiState: StateFlow<CheckoutConfirmationUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.draft.collect { draft ->
                mutableUiState.value = draft?.let {
                    CheckoutConfirmationUiState.Content(mapper.confirmation(it))
                } ?: CheckoutConfirmationUiState.Missing
            }
        }
    }
}

@Composable
fun CheckoutConfirmationRoute(
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckoutConfirmationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CheckoutConfirmationScreen(state, onNavigateToCart, modifier)
}
