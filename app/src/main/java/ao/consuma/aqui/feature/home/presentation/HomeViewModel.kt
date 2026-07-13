package ao.consuma.aqui.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.home.domain.model.DiscoveryLocation
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryError
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryRepository
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryRequest
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryResult
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.LocationPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeDiscoveryRepository,
    private val launchStateRepository: AppLaunchStateRepository,
    private val mapper: HomeUiMapper
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val locationPreference: StateFlow<LocationPreference> = launchStateRepository.locationPreference

    private var query = ""
    private var selectedCategoryId: String? = null
    private var lastLocationPreference: LocationPreference? = null

    fun onLocationChanged(preference: LocationPreference) {
        if (lastLocationPreference == preference && _uiState.value !is HomeUiState.Loading) return
        lastLocationPreference = preference
        load(forceRefresh = false)
    }

    fun onEvent(
        event: HomeUiEvent,
        onMerchantSelected: (String) -> Unit = {},
        onLocationSelected: () -> Unit = {}
    ) {
        when (event) {
            HomeUiEvent.Load -> load(false)
            HomeUiEvent.Refresh -> load(true)
            is HomeUiEvent.SearchChanged -> {
                query = event.value
                load(false)
            }
            is HomeUiEvent.CategorySelected -> {
                selectedCategoryId = event.categoryId
                load(false)
            }
            is HomeUiEvent.MerchantSelected -> onMerchantSelected(event.merchantId)
            HomeUiEvent.LocationSelected -> onLocationSelected()
            HomeUiEvent.Retry -> load(true)
        }
    }

    private fun load(forceRefresh: Boolean) {
        val previous = _uiState.value
        if (forceRefresh && previous is HomeUiState.Content) {
            _uiState.value = previous.copy(isRefreshing = true)
        } else if (previous !is HomeUiState.Content) {
            _uiState.value = HomeUiState.Loading
        }
        val location = (lastLocationPreference ?: locationPreference.value).toDiscoveryLocation()
        viewModelScope.launch(Dispatchers.Unconfined) {
        when (val result = repository.getHomeContent(HomeDiscoveryRequest(location, selectedCategoryId, query, forceRefresh))) {
            is HomeDiscoveryResult.Success -> {
                val categories = mapper.categories(result.content)
                val (nearby, featured) = mapper.merchants(result.content, location != null)
                _uiState.value = if (nearby.isEmpty()) {
                    HomeUiState.Empty(location?.let { LocationUiModel(it.displayName) }, categories, query, selectedCategoryId)
                } else {
                    HomeUiState.Content(
                        location = location?.let { LocationUiModel(it.displayName) },
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        nearbyMerchants = nearby,
                        featuredMerchants = featured,
                        query = query,
                        isRefreshing = false,
                        isOffline = result.isOffline
                    )
                }
            }
            is HomeDiscoveryResult.Failure -> _uiState.value = HomeUiState.Error(
                message = UiText.Resource(
                    when (result.error) {
                        HomeDiscoveryError.NetworkUnavailable -> R.string.home_error_offline
                        HomeDiscoveryError.LocationRequired -> R.string.home_error_location
                        else -> R.string.home_error_generic
                    }
                ),
                canRetry = true
            )
        }
        }
    }

    private fun LocationPreference.toDiscoveryLocation(): DiscoveryLocation? =
        (this as? LocationPreference.Selected)?.location?.let {
            DiscoveryLocation(it.id, it.city, it.area, it.displayName)
        }
}
