package ao.consuma.aqui.feature.search

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SearchRoute(
    onNavigateToMerchant: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locationPreference by viewModel.locationPreference.collectAsStateWithLifecycle()
    LaunchedEffect(locationPreference) { viewModel.onLocationChanged(locationPreference) }
    SearchScreen(
        uiState = state,
        onEvent = { viewModel.onEvent(it, onNavigateToMerchant) },
        modifier = modifier,
        cartAction = {
            ao.consuma.aqui.feature.cart.presentation.components.CartActionRoute(onNavigateToCart)
        }
    )
}
