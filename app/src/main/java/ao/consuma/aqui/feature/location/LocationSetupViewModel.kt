package ao.consuma.aqui.feature.location

import androidx.lifecycle.ViewModel
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LocationSetupViewModel @Inject constructor(
    private val appLaunchStateRepository: AppLaunchStateRepository,
    private val mockLocationRepository: MockLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationSetupUiState>(LocationSetupUiState.Explanation)
    val uiState: StateFlow<LocationSetupUiState> = _uiState.asStateFlow()

    fun onEvent(event: LocationSetupEvent, onComplete: () -> Unit) {
        when (event) {
            is LocationSetupEvent.UseCurrentLocation -> detectLocation()
            is LocationSetupEvent.ChooseManually -> showManualSelection()
            is LocationSetupEvent.Skip -> skipLocation(onComplete)
            is LocationSetupEvent.SearchChanged -> updateSearch(event.value)
            is LocationSetupEvent.LocationSelected -> selectLocation(event.id)
            is LocationSetupEvent.Confirm -> confirmLocation(onComplete)
            is LocationSetupEvent.ChooseAnother -> showManualSelection()
            is LocationSetupEvent.BackToExplanation -> backToExplanation()
        }
    }

    private fun detectLocation() {
        _uiState.value = LocationSetupUiState.Detecting
        val detected = mockLocationRepository.getDefault()
        _uiState.value = LocationSetupUiState.Detected(detected)
    }

    private fun showManualSelection() {
        val allLocations = mockLocationRepository.getAll()
        _uiState.value = LocationSetupUiState.ManualSelection(
            query = "",
            allLocations = allLocations,
            filteredLocations = allLocations,
            selectedLocation = null
        )
    }

    private fun updateSearch(query: String) {
        val current = _uiState.value as? LocationSetupUiState.ManualSelection ?: return
        val filtered = mockLocationRepository.search(query)
        _uiState.value = current.copy(
            query = query,
            filteredLocations = filtered
        )
    }

    private fun selectLocation(id: String) {
        val current = _uiState.value as? LocationSetupUiState.ManualSelection ?: return
        val location = mockLocationRepository.findById(id)
        _uiState.value = current.copy(selectedLocation = location)
    }

    private fun confirmLocation(onComplete: () -> Unit) {
        val location = when (val state = _uiState.value) {
            is LocationSetupUiState.Detected -> state.location
            is LocationSetupUiState.ManualSelection -> state.selectedLocation
            else -> null
        }
        location?.let {
            appLaunchStateRepository.selectLocation(it)
            onComplete()
        }
    }

    private fun skipLocation(onComplete: () -> Unit) {
        appLaunchStateRepository.skipLocation()
        onComplete()
    }

    private fun backToExplanation() {
        _uiState.value = LocationSetupUiState.Explanation
    }
}
