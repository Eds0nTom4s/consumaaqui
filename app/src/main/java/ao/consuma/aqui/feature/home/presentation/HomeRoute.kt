package ao.consuma.aqui.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeRoute(
    onNavigateToLocationSettings: () -> Unit,
    onNavigateToMerchant: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val locationPreference by viewModel.locationPreference.collectAsState()

    LaunchedEffect(locationPreference) {
        viewModel.onLocationChanged(locationPreference)
    }

    HomeScreen(
        uiState = uiState,
        onEvent = { event ->
            viewModel.onEvent(event, onNavigateToMerchant, onNavigateToLocationSettings)
        },
        modifier = modifier
    )
}
