package ao.consuma.aqui.feature.location

import ao.consuma.aqui.feature.launch.domain.MockLocation

sealed interface LocationSetupUiState {
    data object Explanation : LocationSetupUiState
    data object Detecting : LocationSetupUiState
    data class Detected(val location: MockLocation) : LocationSetupUiState
    data class ManualSelection(
        val query: String,
        val allLocations: List<MockLocation>,
        val filteredLocations: List<MockLocation>,
        val selectedLocation: MockLocation?
    ) : LocationSetupUiState
}
