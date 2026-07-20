package ao.consuma.aqui.feature.checkout.presentation.checkout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.consuma.aqui.feature.checkout.presentation.mapper.resolve

@Composable
fun CheckoutRoute(
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToConfirmation: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.onEvent(CheckoutUiEvent.Initialize)
        viewModel.effects.collect { effect ->
            when (effect) {
                CheckoutUiEffect.NavigateToCart -> onNavigateToCart()
                CheckoutUiEffect.NavigateToConfirmation -> onNavigateToConfirmation()
                is CheckoutUiEffect.Message -> onShowMessage(effect.text.resolve(context))
            }
        }
    }
    CheckoutScreen(
        uiState = state,
        onNavigateBack = {
            viewModel.onEvent(CheckoutUiEvent.BackStep)
            if (state !is CheckoutUiState.Content) onNavigateBack()
        },
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}
