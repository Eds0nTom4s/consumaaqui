package ao.consuma.aqui.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.discovery.domain.model.*
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.request.DiscoverySearchRequest
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryError
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult
import ao.consuma.aqui.feature.discovery.presentation.mapper.DiscoveryUiMapper
import ao.consuma.aqui.feature.discovery.presentation.mapper.UiText
import ao.consuma.aqui.feature.discovery.data.modules.DiscoveryDispatcher
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.LocationPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.ArrayList
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal object SearchSavedStateKeys {
    const val QUERY = "discovery_query"
    const val CATEGORY = "discovery_category"
    const val OPEN_NOW = "discovery_open_now"
    const val FULFILLMENT = "discovery_fulfillment_options"
    const val ORDER = "discovery_order"
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: DiscoveryRepository,
    private val launchStateRepository: AppLaunchStateRepository,
    private val mapper: DiscoveryUiMapper,
    private val savedStateHandle: SavedStateHandle,
    @DiscoveryDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    val locationPreference: StateFlow<LocationPreference> = launchStateRepository.locationPreference

    private var query: String = savedStateHandle[SearchSavedStateKeys.QUERY] ?: ""
    private var categoryId: String? = savedStateHandle[SearchSavedStateKeys.CATEGORY]
    private var onlyOpen: Boolean = savedStateHandle[SearchSavedStateKeys.OPEN_NOW] ?: false
    private var fulfillmentOptions: Set<FulfillmentOption> =
        savedStateHandle.get<ArrayList<String>>(SearchSavedStateKeys.FULFILLMENT)
            ?.mapNotNull { runCatching { FulfillmentOption.valueOf(it) }.getOrNull() }
            ?.toSet().orEmpty()
    private var orderBy: DiscoveryOrderBy = savedStateHandle.get<String>(SearchSavedStateKeys.ORDER)
        ?.let { runCatching { DiscoveryOrderBy.valueOf(it) }.getOrNull() }
        ?: DiscoveryOrderBy.FEATURED
    private var defaultSortResolved = savedStateHandle.contains(SearchSavedStateKeys.ORDER)
    private var lastCategories: List<MerchantCategory> = emptyList()
    private var lastLocationPreference: LocationPreference? = null
    private var loadJob: Job? = null

    fun onLocationChanged(preference: LocationPreference) {
        if (lastLocationPreference == preference && _uiState.value !is SearchUiState.Loading) return
        lastLocationPreference = preference
        load(initial = _uiState.value is SearchUiState.Loading)
    }

    fun onEvent(event: SearchUiEvent, onMerchantSelected: (String) -> Unit = {}) {
        when (event) {
            SearchUiEvent.Load -> load(initial = _uiState.value is SearchUiState.Loading)
            SearchUiEvent.Retry -> load(initial = true)
            SearchUiEvent.Refresh -> load(refreshing = true)
            // Clear keeps query and the user's sort choice; only filtering dimensions are reset.
            SearchUiEvent.ClearFilters -> { categoryId = null; onlyOpen = false; fulfillmentOptions = emptySet(); persist(); load() }
            SearchUiEvent.ClearQuery -> { query = ""; persist(); load() }
            is SearchUiEvent.QueryChanged -> { query = event.value; persist(); load() }
            is SearchUiEvent.CategorySelected -> { categoryId = event.id; persist(); load() }
            is SearchUiEvent.OpenNowChanged -> { onlyOpen = event.enabled; persist(); load() }
            is SearchUiEvent.FulfillmentToggled -> {
                fulfillmentOptions = if (event.value in fulfillmentOptions) fulfillmentOptions - event.value else fulfillmentOptions + event.value
                persist(); load()
            }
            is SearchUiEvent.SortSelected -> { if (event.value != DiscoveryOrderBy.NEAREST || hasLocation()) { orderBy = event.value; persist(); load() } }
            is SearchUiEvent.MerchantSelected -> onMerchantSelected(event.merchantId)
        }
    }

    private fun load(initial: Boolean = false, refreshing: Boolean = false) {
        val location = currentLocation()
        if (!defaultSortResolved) {
            orderBy = if (location == null) DiscoveryOrderBy.FEATURED else DiscoveryOrderBy.NEAREST
            defaultSortResolved = true
            persist()
        } else if (location == null && orderBy == DiscoveryOrderBy.NEAREST) {
            orderBy = DiscoveryOrderBy.FEATURED
            persist()
        }
        if (initial) _uiState.value = SearchUiState.Loading
        if (refreshing) markRefreshing()
        val request = DiscoverySearchRequest(
            query = query,
            categoryId = categoryId,
            onlyOpen = onlyOpen,
            fulfillmentOptions = fulfillmentOptions,
            orderBy = orderBy,
            location = location
        )
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            when (val result = repository.search(request)) {
                is DiscoveryResult.Success -> show(result.data, location != null, offline = false)
                is DiscoveryResult.Offline -> result.cachedData?.let { show(it, location != null, offline = true) }
                    ?: showError(DiscoveryError.NetworkUnavailable)
                is DiscoveryResult.Empty -> showEmpty(result.data, location != null)
                is DiscoveryResult.Error -> showError(result.reason)
            }
        }
    }

    private fun show(content: MerchantSearchContent, hasLocation: Boolean, offline: Boolean) {
        lastCategories = content.categories
        val exploration = query.trim().isEmpty()
        val data = SearchResultsUiModel(
            criteria = criteria(hasLocation),
            merchants = mapper.search(content, hasLocation),
            totalResults = content.totalCount,
            resultContext = mapper.resultContext(content.totalCount, exploration),
            isRefreshing = false,
            isExplorationMode = exploration
        )
        _uiState.value = if (offline) SearchUiState.OfflineContent(data) else SearchUiState.Content(data)
    }

    private fun showEmpty(content: MerchantSearchContent?, hasLocation: Boolean) {
        content?.categories?.let { lastCategories = it }
        val hasQuery = query.trim().isNotEmpty()
        val message = UiText.Resource(
            if (hasQuery) R.string.search_empty_query_description else R.string.search_empty_filters_description
        )
        _uiState.value = SearchUiState.Empty(criteria(hasLocation), message)
    }

    private fun showError(error: DiscoveryError) {
        val message = when (error) {
            DiscoveryError.LocationRequired -> R.string.search_error_location
            DiscoveryError.NetworkUnavailable -> R.string.search_error_offline
            DiscoveryError.Server -> R.string.search_error_server
            else -> R.string.search_error_generic
        }
        _uiState.value = SearchUiState.Error(UiText.Resource(message), canRetry = true)
    }

    private fun markRefreshing() {
        _uiState.value = when (val state = _uiState.value) {
            is SearchUiState.Content -> state.copy(data = state.data.copy(isRefreshing = true))
            is SearchUiState.OfflineContent -> state.copy(data = state.data.copy(isRefreshing = true))
            else -> state
        }
    }

    private fun criteria(hasLocation: Boolean = hasLocation()) = SearchCriteriaUiState(
        query = query,
        filters = SearchFiltersUiModel(categoryId, onlyOpen, fulfillmentOptions),
        orderBy = orderBy,
        categories = mapper.categories(lastCategories),
        sortOptions = mapper.sortOptions(hasLocation),
        hasLocation = hasLocation
    )

    private fun persist() {
        savedStateHandle[SearchSavedStateKeys.QUERY] = query
        savedStateHandle[SearchSavedStateKeys.CATEGORY] = categoryId
        savedStateHandle[SearchSavedStateKeys.OPEN_NOW] = onlyOpen
        savedStateHandle[SearchSavedStateKeys.FULFILLMENT] = ArrayList(fulfillmentOptions.map { it.name })
        savedStateHandle[SearchSavedStateKeys.ORDER] = orderBy.name
    }

    private fun hasLocation() = currentLocation() != null
    private fun currentLocation(): DiscoveryLocation? =
        (lastLocationPreference ?: launchStateRepository.locationPreference.value).toDiscoveryLocation()
    private fun LocationPreference.toDiscoveryLocation(): DiscoveryLocation? =
        (this as? LocationPreference.Selected)?.location?.let { DiscoveryLocation(it.id, it.city, it.area, it.displayName) }
}
