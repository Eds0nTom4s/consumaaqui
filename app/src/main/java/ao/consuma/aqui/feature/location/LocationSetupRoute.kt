package ao.consuma.aqui.feature.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LocationSetupRoute(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentOnSetupComplete by rememberUpdatedState(onSetupComplete)

    LocationSetupContent(
        uiState = uiState,
        onEvent = { event ->
            viewModel.onEvent(event, currentOnSetupComplete)
        },
        modifier = modifier
    )
}

@Composable
private fun LocationSetupContent(
    uiState: LocationSetupUiState,
    onEvent: (LocationSetupEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LocationSetupUiState.Explanation -> {
            LocationExplanationScreen(
                onUseCurrentLocation = { onEvent(LocationSetupEvent.UseCurrentLocation) },
                onChooseManually = { onEvent(LocationSetupEvent.ChooseManually) },
                onSkip = { onEvent(LocationSetupEvent.Skip) },
                modifier = modifier
            )
        }
        is LocationSetupUiState.Detecting -> {
            LocationDetectingScreen(modifier = modifier)
        }
        is LocationSetupUiState.Detected -> {
            LocationDetectedScreen(
                location = uiState.location,
                onConfirm = { onEvent(LocationSetupEvent.Confirm) },
                onChooseAnother = { onEvent(LocationSetupEvent.ChooseAnother) },
                modifier = modifier
            )
        }
        is LocationSetupUiState.ManualSelection -> {
            ManualLocationSelectionScreen(
                query = uiState.query,
                locations = uiState.filteredLocations,
                selectedLocation = uiState.selectedLocation,
                onQueryChange = { onEvent(LocationSetupEvent.SearchChanged(it)) },
                onLocationSelected = { onEvent(LocationSetupEvent.LocationSelected(it)) },
                onConfirm = { onEvent(LocationSetupEvent.Confirm) },
                onNavigateBack = {
                    onEvent(LocationSetupEvent.BackToExplanation)
                },
                modifier = modifier
            )
        }
    }
}
