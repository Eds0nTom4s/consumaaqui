package ao.consuma.aqui.feature.home

import androidx.lifecycle.ViewModel
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.LocationPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    appLaunchStateRepository: AppLaunchStateRepository
) : ViewModel() {

    val locationPreference: StateFlow<LocationPreference> =
        appLaunchStateRepository.locationPreference
}
