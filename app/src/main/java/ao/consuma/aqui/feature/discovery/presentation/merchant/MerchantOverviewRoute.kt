package ao.consuma.aqui.feature.discovery.presentation.merchant

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MerchantOverviewRoute(
    onNavigateBack: () -> Unit,
    onNavigateToCatalog: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MerchantOverviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    MerchantOverviewScreen(
        uiState = state,
        onRetry = { viewModel.onEvent(MerchantOverviewUiEvent.Retry) },
        onNavigateBack = onNavigateBack,
        onNavigateToCatalog = {
            viewModel.onEvent(MerchantOverviewUiEvent.OpenCatalog, onNavigateToCatalog)
        },
        modifier = modifier
    )
}
