package ao.consuma.aqui.feature.catalog.presentation.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CatalogRoute(
    merchantId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProduct: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(merchantId) { viewModel.onEvent(CatalogUiEvent.Load) }
    CatalogScreen(
        uiState = state,
        onNavigateBack = onNavigateBack,
        onEvent = { viewModel.onEvent(it, onNavigateToProduct) },
        modifier = modifier
    )
}
