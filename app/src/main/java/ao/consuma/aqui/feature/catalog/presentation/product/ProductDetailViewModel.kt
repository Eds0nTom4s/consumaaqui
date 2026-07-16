package ao.consuma.aqui.feature.catalog.presentation.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.catalog.data.modules.CatalogDispatcher
import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.ConfiguredProduct
import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.catalog.domain.model.ProductConfiguration
import ao.consuma.aqui.feature.catalog.domain.repository.CatalogRepository
import ao.consuma.aqui.feature.catalog.domain.request.ProductRequest
import ao.consuma.aqui.feature.catalog.domain.result.CatalogError
import ao.consuma.aqui.feature.catalog.domain.result.CatalogResult
import ao.consuma.aqui.feature.catalog.domain.service.PriceCalculationResult
import ao.consuma.aqui.feature.catalog.domain.service.ProductConfigurationFactory
import ao.consuma.aqui.feature.catalog.domain.service.ProductConfigurationResult
import ao.consuma.aqui.feature.catalog.domain.service.ProductConfigurationValidator
import ao.consuma.aqui.feature.catalog.domain.service.ProductPriceCalculator
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiMapper
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.ArrayList
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val validator: ProductConfigurationValidator,
    private val mapper: CatalogUiMapper,
    private val savedStateHandle: SavedStateHandle,
    @CatalogDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val merchantId: String = savedStateHandle[ProductDetailSavedStateKeys.MERCHANT_ID] ?: ""
    private val productId: String = savedStateHandle[ProductDetailSavedStateKeys.PRODUCT_ID] ?: ""
    private var product: CatalogProduct? = null
    private var selections: Map<String, Set<String>> = emptyMap()
    private var quantity: Int = (savedStateHandle[ProductDetailSavedStateKeys.QUANTITY] ?: 1)
        .coerceIn(ProductConfiguration.MIN_QUANTITY, ProductConfiguration.MAX_QUANTITY)
    private var note: String = (savedStateHandle[ProductDetailSavedStateKeys.NOTE] ?: "")
        .take(ProductConfiguration.MAX_NOTE_LENGTH)
    private var restoredSelections: List<String> =
        savedStateHandle.get<ArrayList<String>>(ProductDetailSavedStateKeys.SELECTIONS).orEmpty()
    private var hasRestoredSelectionState: Boolean =
        savedStateHandle.contains(ProductDetailSavedStateKeys.SELECTIONS)
    private var configurationInitialized = false
    private var showValidationErrors: Boolean =
        savedStateHandle[ProductDetailSavedStateKeys.VALIDATION_ATTEMPTED] ?: false
    private var configuredProductEmitted = false
    private var offline = false
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun onEvent(
        event: ProductDetailUiEvent,
        onProductConfigured: (ConfiguredProduct) -> Unit = {}
    ) {
        when (event) {
            ProductDetailUiEvent.Load -> load()
            ProductDetailUiEvent.Retry -> load()
            is ProductDetailUiEvent.OptionToggled -> toggle(event.groupId, event.optionId)
            ProductDetailUiEvent.QuantityIncreased -> updateQuantity(quantity + 1)
            ProductDetailUiEvent.QuantityDecreased -> updateQuantity(quantity - 1)
            is ProductDetailUiEvent.NoteChanged -> {
                note = event.value.take(ProductConfiguration.MAX_NOTE_LENGTH)
                configurationChanged()
                persist()
                render()
            }
            ProductDetailUiEvent.Add -> add(onProductConfigured)
        }
    }

    private fun load() {
        if (merchantId.isBlank() || productId.isBlank()) {
            _uiState.value = ProductDetailUiState.InvalidArguments
            return
        }
        _uiState.value = ProductDetailUiState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            when (val result = repository.getProduct(ProductRequest(merchantId, productId))) {
                is CatalogResult.Success -> accept(result.data, isOffline = false)
                is CatalogResult.Offline -> result.cachedData?.let { accept(it, isOffline = true) }
                    ?: showError(CatalogError.NetworkUnavailable)
                is CatalogResult.Empty -> _uiState.value = ProductDetailUiState.NotFound
                is CatalogResult.Error -> showError(result.reason)
            }
        }
    }

    private fun accept(value: CatalogProduct, isOffline: Boolean) {
        if (value.merchantId != merchantId || value.id != productId) {
            _uiState.value = ProductDetailUiState.NotFound
            return
        }
        product = value
        offline = isOffline
        selections = when {
            configurationInitialized -> restoreSelections(
                value,
                selections.flatMap { (group, options) -> options.map { "$group$SELECTION_SEPARATOR$it" } }
            )
            hasRestoredSelectionState -> restoreSelections(value, restoredSelections)
            else -> ProductConfigurationFactory.create(value).selections
        }
        restoredSelections = emptyList()
        hasRestoredSelectionState = false
        configurationInitialized = true
        persist()
        if (value.availability != ProductAvailability.Available) {
            _uiState.value = ProductDetailUiState.Unavailable(mapper.productDetail(value), isOffline)
        } else {
            render()
        }
    }

    private fun toggle(groupId: String, optionId: String) {
        val value = product ?: return
        val group = value.optionGroups.find { it.id == groupId } ?: return
        val option = group.options.find { it.id == optionId && it.available } ?: return
        val current = selections[groupId].orEmpty()
        val updated = if (group.selectionRule.singleChoice) {
            if (option.id in current && group.minimumSelections == 0) emptySet()
            else setOf(option.id)
        } else if (option.id in current) {
            current - option.id
        } else if (current.size < group.maximumSelections) {
            current + option.id
        } else {
            current
        }
        selections = if (updated.isEmpty()) selections - groupId else selections + (groupId to updated)
        configurationChanged()
        persist()
        render()
    }

    private fun updateQuantity(value: Int) {
        val normalized = value.coerceIn(ProductConfiguration.MIN_QUANTITY, ProductConfiguration.MAX_QUANTITY)
        if (normalized == quantity) return
        quantity = normalized
        configurationChanged()
        persist()
        render()
    }

    private fun add(onProductConfigured: (ConfiguredProduct) -> Unit) {
        if (configuredProductEmitted) return
        val value = product ?: return
        val result = validator.validate(value, configuration())
        if (result is ProductConfigurationResult.Valid) {
            showValidationErrors = false
            savedStateHandle[ProductDetailSavedStateKeys.VALIDATION_ATTEMPTED] = false
            configuredProductEmitted = true
            render()
            onProductConfigured(result.configuredProduct)
        } else if (result is ProductConfigurationResult.Invalid) {
            showValidationErrors = true
            savedStateHandle[ProductDetailSavedStateKeys.VALIDATION_ATTEMPTED] = true
            render(result)
        }
    }

    private fun configurationChanged() {
        configuredProductEmitted = false
    }

    private fun render(validation: ProductConfigurationResult.Invalid? = null) {
        val value = product ?: return
        if (value.availability != ProductAvailability.Available) {
            _uiState.value = ProductDetailUiState.Unavailable(mapper.productDetail(value), offline)
            return
        }
        val result = validation ?: if (showValidationErrors) {
            validator.validate(value, configuration()) as? ProductConfigurationResult.Invalid
        } else null
        val selectedPrices = value.optionGroups.flatMap { group ->
            val ids = selections[group.id].orEmpty()
            group.options.filter { it.id in ids }.mapNotNull { it.additionalPrice }
        }
        val calculation = ProductPriceCalculator.calculate(value.basePrice, selectedPrices, quantity)
        val prices = (calculation as? PriceCalculationResult.Success)?.prices ?: return showPriceError()
        _uiState.value = ProductDetailUiState.Content(
            ProductDetailContentUiModel(
                product = mapper.productDetail(value),
                selections = selections.mapValues { it.value.toSet() },
                quantity = quantity,
                note = note,
                validationErrors = result?.errors?.let(mapper::configurationErrors).orEmpty(),
                priceSummary = mapper.priceSummary(prices),
                canAdd = true,
                isOffline = offline
            )
        )
    }

    private fun configuration() = ProductConfiguration(productId, selections, quantity, note)

    private fun restoreSelections(value: CatalogProduct, encoded: List<String>): Map<String, Set<String>> {
        val candidates = encoded.mapNotNull { entry ->
            val separator = entry.indexOf(SELECTION_SEPARATOR)
            if (separator <= 0 || separator >= entry.lastIndex) null
            else entry.substring(0, separator) to entry.substring(separator + 1)
        }.groupBy({ it.first }, { it.second })
        return value.optionGroups.mapNotNull { group ->
            val valid = candidates[group.id].orEmpty()
                .filter { id -> group.options.any { it.id == id && it.available } }
                .distinct()
                .take(group.maximumSelections)
                .toSet()
            valid.takeIf { it.isNotEmpty() }?.let { group.id to it }
        }.toMap()
    }

    private fun persist() {
        savedStateHandle[ProductDetailSavedStateKeys.QUANTITY] = quantity
        savedStateHandle[ProductDetailSavedStateKeys.NOTE] = note
        savedStateHandle[ProductDetailSavedStateKeys.SELECTIONS] = ArrayList(
            selections.flatMap { (group, options) -> options.map { "$group$SELECTION_SEPARATOR$it" } }
        )
    }

    private fun showError(error: CatalogError) {
        _uiState.value = when (error) {
            CatalogError.ProductNotFound, CatalogError.CatalogNotFound, CatalogError.MerchantNotFound -> ProductDetailUiState.NotFound
            CatalogError.ProductUnavailable -> product?.let {
                ProductDetailUiState.Unavailable(mapper.productDetail(it), offline)
            } ?: ProductDetailUiState.NotFound
            else -> ProductDetailUiState.Error(
                CatalogUiText.Resource(
                    if (error == CatalogError.NetworkUnavailable) R.string.product_error_offline
                    else R.string.product_error_generic
                ),
                canRetry = true
            )
        }
    }

    private fun showPriceError() {
        _uiState.value = ProductDetailUiState.Error(
            CatalogUiText.Resource(R.string.product_error_price),
            canRetry = false
        )
    }

    private companion object {
        const val SELECTION_SEPARATOR = '\u001F'
    }
}
