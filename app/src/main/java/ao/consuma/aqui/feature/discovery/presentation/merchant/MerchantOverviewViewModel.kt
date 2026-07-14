package ao.consuma.aqui.feature.discovery.presentation.merchant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.request.MerchantRequest
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryError
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult
import ao.consuma.aqui.feature.discovery.presentation.mapper.DiscoveryUiMapper
import ao.consuma.aqui.feature.discovery.presentation.mapper.UiText
import ao.consuma.aqui.feature.discovery.data.modules.DiscoveryDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MerchantOverviewViewModel @Inject constructor(
    private val repository: DiscoveryRepository,
    private val mapper: DiscoveryUiMapper,
    savedStateHandle: SavedStateHandle,
    @DiscoveryDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val merchantId: String = savedStateHandle["merchantId"] ?: ""
    private val _uiState = MutableStateFlow<MerchantOverviewUiState>(MerchantOverviewUiState.Loading)
    val uiState: StateFlow<MerchantOverviewUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun onEvent(event: MerchantOverviewUiEvent, onOpenCatalog: (String) -> Unit = {}) {
        when (event) {
            MerchantOverviewUiEvent.Retry -> load()
            MerchantOverviewUiEvent.OpenCatalog -> {
                val merchant = when (val state = _uiState.value) {
                    is MerchantOverviewUiState.Content -> state.merchant
                    is MerchantOverviewUiState.OfflineContent -> state.merchant
                    else -> null
                }
                if (merchant?.catalogAvailable == true) onOpenCatalog(merchant.id)
            }
        }
    }

    fun load() {
        if (merchantId.isBlank()) {
            _uiState.value = MerchantOverviewUiState.NotFound
            return
        }
        _uiState.value = MerchantOverviewUiState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            when (val result = repository.merchant(MerchantRequest(merchantId))) {
                is DiscoveryResult.Success -> _uiState.value = MerchantOverviewUiState.Content(mapper.overview(result.data))
                is DiscoveryResult.Offline -> result.cachedData?.let {
                    _uiState.value = MerchantOverviewUiState.OfflineContent(mapper.overview(it))
                } ?: showError(DiscoveryError.NetworkUnavailable)
                is DiscoveryResult.Empty -> _uiState.value = MerchantOverviewUiState.NotFound
                is DiscoveryResult.Error -> showError(result.reason)
            }
        }
    }

    private fun showError(error: DiscoveryError) {
        if (error == DiscoveryError.NotFound) {
            _uiState.value = MerchantOverviewUiState.NotFound
        } else {
            val message = if (error == DiscoveryError.NetworkUnavailable) R.string.merchant_error_offline else R.string.merchant_error_generic
            _uiState.value = MerchantOverviewUiState.Error(UiText.Resource(message), canRetry = true)
        }
    }
}
