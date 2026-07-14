package ao.consuma.aqui.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    onNavigateToLocationSettings: () -> Unit,
    onNavigateToMerchant: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationPreference by viewModel.locationPreference.collectAsStateWithLifecycle()

    LaunchedEffect(locationPreference) {
        viewModel.onLocationChanged(locationPreference)
    }

    HomeScreen(
        uiState = uiState,
        onEvent = { event ->
            viewModel.onEvent(event, onNavigateToMerchant, onNavigateToLocationSettings, onNavigateToSearch)
        },
        modifier = modifier
    )
}
