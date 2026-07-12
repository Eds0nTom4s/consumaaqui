package ao.consuma.aqui.feature.location

import androidx.lifecycle.ViewModel
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LocationSettingsViewModel @Inject constructor(
    private val appLaunchStateRepository: AppLaunchStateRepository
) : ViewModel() {

    val locationPreference: StateFlow<ao.consuma.aqui.feature.launch.domain.LocationPreference> =
        appLaunchStateRepository.locationPreference

    fun removeLocation() {
        appLaunchStateRepository.clearLocation()
    }
}
