package ao.consuma.aqui.feature.catalog.presentation.product

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult

@Composable
fun ProductDetailRoute(
    merchantId: String,
    productId: String,
    onNavigateBack: () -> Unit,
    onCartResult: (AddCartItemResult) -> Unit,
    onShowMessage: (String) -> Unit,
    onSuccess: () -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(merchantId, productId) { viewModel.onEvent(ProductDetailUiEvent.Load) }
    DisposableEffect(viewModel) {
        onDispose { viewModel.dismissTransientConflict() }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProductDetailUiEffect.Success -> {
                    onShowMessage(context.getString(effect.messageRes))
                    onSuccess()
                }
                is ProductDetailUiEffect.Message -> onShowMessage(context.getString(effect.messageRes))
            }
        }
    }
    ProductDetailScreen(
        uiState = state,
        onNavigateBack = onNavigateBack,
        onEvent = { viewModel.onEvent(it, onCartResult) },
        modifier = modifier,
        cartAction = {
            ao.consuma.aqui.feature.cart.presentation.components.CartActionRoute(onNavigateToCart)
        }
    )
}
