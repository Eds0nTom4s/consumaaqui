package ao.consuma.aqui.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryLocation
import ao.consuma.aqui.feature.discovery.domain.model.HomeDiscoveryContent
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.request.HomeDiscoveryRequest
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryError
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult
import ao.consuma.aqui.feature.discovery.data.modules.DiscoveryDispatcher
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.LocationPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DiscoveryRepository,
    private val launchStateRepository: AppLaunchStateRepository,
    private val mapper: HomeUiMapper,
    @DiscoveryDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val locationPreference: StateFlow<LocationPreference> = launchStateRepository.locationPreference

    private var query = ""
    private var selectedCategoryId: String? = null
    private var lastLocationPreference: LocationPreference? = null
    private var loadJob: Job? = null

    fun onLocationChanged(preference: LocationPreference) {
        if (lastLocationPreference == preference && _uiState.value !is HomeUiState.Loading) return
        lastLocationPreference = preference
        load(forceRefresh = false)
    }

    fun onEvent(
        event: HomeUiEvent,
        onMerchantSelected: (String) -> Unit = {},
        onLocationSelected: () -> Unit = {},
        onViewAll: () -> Unit = {}
    ) {
        when (event) {
            HomeUiEvent.Load -> load(false)
            HomeUiEvent.Refresh -> load(true)
            is HomeUiEvent.SearchChanged -> { query = event.value; load(false) }
            is HomeUiEvent.CategorySelected -> { selectedCategoryId = event.categoryId; load(false) }
            is HomeUiEvent.MerchantSelected -> onMerchantSelected(event.merchantId)
            HomeUiEvent.ViewAll -> onViewAll()
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
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            when (val result = repository.home(HomeDiscoveryRequest(location, selectedCategoryId, query, forceRefresh))) {
                is DiscoveryResult.Success -> showContent(result.data, location, false)
                is DiscoveryResult.Offline -> result.cachedData?.let { showContent(it, location, true) }
                    ?: showError(DiscoveryError.NetworkUnavailable)
                is DiscoveryResult.Empty -> showEmpty(result.data, location)
                is DiscoveryResult.Error -> showError(result.reason)
            }
        }
    }

    private fun showContent(content: HomeDiscoveryContent, location: DiscoveryLocation?, isOffline: Boolean) {
        val categories = mapper.categories(content)
        val sections = mapper.merchants(content, location != null)
        _uiState.value = if (sections.nearby.isEmpty() && sections.recommended.isEmpty() && sections.featured.isEmpty()) {
            HomeUiState.Empty(location?.let { LocationUiModel(it.displayName) }, categories, query, selectedCategoryId)
        } else {
            HomeUiState.Content(
                location = location?.let { LocationUiModel(it.displayName) },
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                nearbyMerchants = sections.nearby,
                recommendedMerchants = sections.recommended,
                featuredMerchants = sections.featured,
                query = query,
                isRefreshing = false,
                isOffline = isOffline
            )
        }
    }

    private fun showEmpty(content: HomeDiscoveryContent?, location: DiscoveryLocation?) {
        _uiState.value = HomeUiState.Empty(
            location = location?.let { LocationUiModel(it.displayName) },
            categories = content?.let(mapper::categories).orEmpty(),
            query = query,
            selectedCategoryId = selectedCategoryId
        )
    }

    private fun showError(error: DiscoveryError) {
        _uiState.value = HomeUiState.Error(
            UiText.Resource(
                when (error) {
                    DiscoveryError.NetworkUnavailable -> R.string.home_error_offline
                    DiscoveryError.LocationRequired -> R.string.home_error_location
                    else -> R.string.home_error_generic
                }
            ),
            canRetry = true
        )
    }

    private fun LocationPreference.toDiscoveryLocation(): DiscoveryLocation? =
        (this as? LocationPreference.Selected)?.location?.let {
            DiscoveryLocation(it.id, it.city, it.area, it.displayName)
        }
}
