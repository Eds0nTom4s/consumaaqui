package ao.consuma.aqui.feature.catalog.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.catalog.data.modules.CatalogDispatcher
import ao.consuma.aqui.feature.catalog.domain.model.MerchantCatalog
import ao.consuma.aqui.feature.catalog.domain.repository.CatalogRepository
import ao.consuma.aqui.feature.catalog.domain.request.CatalogRequest
import ao.consuma.aqui.feature.catalog.domain.request.CatalogSearchRequest
import ao.consuma.aqui.feature.catalog.domain.result.CatalogError
import ao.consuma.aqui.feature.catalog.domain.result.CatalogResult
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiMapper
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val mapper: CatalogUiMapper,
    private val savedStateHandle: SavedStateHandle,
    @CatalogDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val merchantId: String = savedStateHandle[CatalogSavedStateKeys.MERCHANT_ID] ?: ""
    private var query: String = savedStateHandle[CatalogSavedStateKeys.QUERY] ?: ""
    private var selectedCategoryId: String? = savedStateHandle[CatalogSavedStateKeys.CATEGORY_ID]
    private var catalog: MerchantCatalog? = null
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    fun onEvent(event: CatalogUiEvent, onProductSelected: (String, String) -> Unit = { _, _ -> }) {
        when (event) {
            CatalogUiEvent.Load -> if (_uiState.value is CatalogUiState.Loading) loadCatalog(initial = true)
            CatalogUiEvent.Refresh -> loadCatalog(refreshing = true, forceRefresh = true)
            CatalogUiEvent.Retry -> loadCatalog(initial = true)
            is CatalogUiEvent.QueryChanged -> {
                query = event.value
                persistCriteria()
                search()
            }
            CatalogUiEvent.ClearQuery -> {
                query = ""
                persistCriteria()
                search()
            }
            is CatalogUiEvent.CategorySelected -> {
                val availableIds = catalog?.categories?.filter { it.available }?.map { it.id }?.toSet().orEmpty()
                selectedCategoryId = event.categoryId?.takeIf { it in availableIds }
                persistCriteria()
                search()
            }
            is CatalogUiEvent.ProductSelected -> {
                if (event.productId.isNotBlank() && merchantId.isNotBlank()) {
                    onProductSelected(merchantId, event.productId)
                }
            }
        }
    }

    private fun loadCatalog(initial: Boolean = false, refreshing: Boolean = false, forceRefresh: Boolean = false) {
        if (merchantId.isBlank()) {
            _uiState.value = CatalogUiState.InvalidMerchant
            return
        }
        if (initial) _uiState.value = CatalogUiState.Loading
        if (refreshing) markRefreshing()
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            when (val result = repository.getCatalog(CatalogRequest(merchantId, forceRefresh))) {
                is CatalogResult.Success -> acceptCatalog(result.data, offline = false, forceRefresh = forceRefresh)
                is CatalogResult.Offline -> result.cachedData?.let {
                    acceptCatalog(it, offline = true, forceRefresh = forceRefresh)
                } ?: showError(CatalogError.NetworkUnavailable)
                is CatalogResult.Empty -> result.data?.let {
                    catalog = it
                    normalizeCategory(it)
                    _uiState.value = CatalogUiState.Empty(
                        mapper.catalog(it, emptyList(), query, selectedCategoryId, false, false),
                        CatalogEmptyReason.CATALOG_EMPTY
                    )
                } ?: run { _uiState.value = CatalogUiState.CatalogNotFound }
                is CatalogResult.Error -> showError(result.reason)
            }
        }
    }

    private suspend fun acceptCatalog(
        value: MerchantCatalog,
        offline: Boolean,
        forceRefresh: Boolean
    ) {
        catalog = value
        normalizeCategory(value)
        if (value.products.isEmpty()) {
            _uiState.value = CatalogUiState.Empty(
                mapper.catalog(value, emptyList(), query, selectedCategoryId, false, offline),
                CatalogEmptyReason.CATALOG_EMPTY
            )
        } else {
            searchNow(offline, forceRefresh)
        }
    }

    private fun search() {
        val value = catalog ?: return loadCatalog(initial = true)
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            searchNow(offline = false, forceRefresh = false, value)
        }
    }

    private suspend fun searchNow(
        offline: Boolean,
        forceRefresh: Boolean,
        value: MerchantCatalog = requireNotNull(catalog)
    ) {
        val request = CatalogSearchRequest(
            merchantId = merchantId,
            query = query,
            categoryId = selectedCategoryId,
            onlyAvailable = false,
            page = 1,
            pageSize = 100,
            forceRefresh = forceRefresh
        )
        when (val result = repository.searchProducts(request)) {
            is CatalogResult.Success -> showContent(value, result.data.products, offline, refreshing = false)
            is CatalogResult.Offline -> result.cachedData?.let {
                showContent(value, it.products, offline = true, refreshing = false)
            } ?: showError(CatalogError.NetworkUnavailable)
            is CatalogResult.Empty -> {
                val products = result.data?.products.orEmpty()
                if (products.isNotEmpty()) showContent(value, products, offline, refreshing = false)
                else showSearchEmpty(value, offline)
            }
            is CatalogResult.Error -> showError(result.reason)
        }
    }

    private fun showContent(
        value: MerchantCatalog,
        products: List<ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct>,
        offline: Boolean,
        refreshing: Boolean
    ) {
        _uiState.value = CatalogUiState.Content(
            mapper.catalog(value, products, query, selectedCategoryId, refreshing, offline)
        )
    }

    private fun showSearchEmpty(value: MerchantCatalog, offline: Boolean) {
        val reason = when {
            query.trim().isNotEmpty() -> CatalogEmptyReason.SEARCH_EMPTY
            selectedCategoryId != null -> CatalogEmptyReason.CATEGORY_EMPTY
            else -> CatalogEmptyReason.CATALOG_EMPTY
        }
        _uiState.value = CatalogUiState.Empty(
            mapper.catalog(value, emptyList(), query, selectedCategoryId, false, offline),
            reason
        )
    }

    private fun showError(error: CatalogError) {
        _uiState.value = when (error) {
            CatalogError.CatalogNotFound, CatalogError.MerchantNotFound -> CatalogUiState.CatalogNotFound
            CatalogError.CatalogUnavailable -> CatalogUiState.Empty(null, CatalogEmptyReason.CATALOG_UNAVAILABLE)
            else -> CatalogUiState.Error(
                CatalogUiText.Resource(
                    if (error == CatalogError.NetworkUnavailable) R.string.catalog_error_offline
                    else R.string.catalog_error_generic
                ),
                canRetry = true
            )
        }
    }

    private fun normalizeCategory(value: MerchantCatalog) {
        val valid = value.categories.any { it.id == selectedCategoryId && it.available }
        if (!valid) selectedCategoryId = null
        persistCriteria()
    }

    private fun markRefreshing() {
        val state = _uiState.value
        if (state is CatalogUiState.Content) {
            _uiState.value = state.copy(data = state.data.copy(isRefreshing = true))
        }
    }

    private fun persistCriteria() {
        savedStateHandle[CatalogSavedStateKeys.QUERY] = query
        savedStateHandle[CatalogSavedStateKeys.CATEGORY_ID] = selectedCategoryId
    }
}
