package ao.consuma.aqui.feature.catalog.presentation.product

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.consuma.aqui.feature.catalog.domain.model.ConfiguredProduct

@Composable
fun ProductDetailRoute(
    merchantId: String,
    productId: String,
    onNavigateBack: () -> Unit,
    onProductConfigured: (ConfiguredProduct) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(merchantId, productId) { viewModel.onEvent(ProductDetailUiEvent.Load) }
    ProductDetailScreen(
        uiState = state,
        onNavigateBack = onNavigateBack,
        onEvent = { viewModel.onEvent(it, onProductConfigured) },
        modifier = modifier
    )
}
