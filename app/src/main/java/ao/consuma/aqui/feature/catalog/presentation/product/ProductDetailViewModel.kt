package ao.consuma.aqui.feature.catalog.presentation.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.cart.domain.command.CartOptionSnapshot
import ao.consuma.aqui.feature.cart.domain.command.ProductCartSnapshot
import ao.consuma.aqui.feature.cart.domain.command.AddCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.ReplaceCartItemCommand
import ao.consuma.aqui.feature.cart.domain.model.CartMerchant
import ao.consuma.aqui.feature.cart.domain.model.CartConflict
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.cart.domain.result.CartResult
import ao.consuma.aqui.feature.cart.domain.service.AddConfiguredProductToCart
import ao.consuma.aqui.feature.catalog.data.modules.CatalogDispatcher
import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.catalog.domain.model.ProductConfiguration
import ao.consuma.aqui.feature.catalog.domain.repository.CatalogRepository
import ao.consuma.aqui.feature.catalog.domain.request.CatalogRequest
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val addConfiguredProductToCart: AddConfiguredProductToCart,
    private val cartRepository: CartRepository,
    private val validator: ProductConfigurationValidator,
    private val mapper: CatalogUiMapper,
    private val savedStateHandle: SavedStateHandle,
    @CatalogDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val merchantId: String = savedStateHandle[ProductDetailSavedStateKeys.MERCHANT_ID] ?: ""
    private val productId: String = savedStateHandle[ProductDetailSavedStateKeys.PRODUCT_ID] ?: ""
    private val cartItemId: String? = savedStateHandle.get<String>(ProductDetailSavedStateKeys.CART_ITEM_ID)
        ?.takeIf(String::isNotBlank)
    private val mode = if (cartItemId == null) ProductDetailMode.ADD else ProductDetailMode.EDIT
    private var product: CatalogProduct? = null
    private var merchantName: String = merchantId
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
    private var pendingConflictCommand: AddCartItemCommand? = null
    private var conflict: ProductDetailConflictUiModel? = null
    private var submitting = false
    private var configurationAdjusted = false

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<ProductDetailUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ProductDetailUiEffect> = _effects.asSharedFlow()

    fun onEvent(
        event: ProductDetailUiEvent,
        onCartResult: (AddCartItemResult) -> Unit = {}
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
            ProductDetailUiEvent.Add -> add(onCartResult)
            ProductDetailUiEvent.KeepCurrentCart -> keepCurrentCart()
            ProductDetailUiEvent.ReplaceCart -> replaceCart()
        }
    }

    fun dismissTransientConflict() {
        if (conflict != null && !submitting) keepCurrentCart()
    }

    private fun load() {
        if (merchantId.isBlank() || productId.isBlank()) {
            _uiState.value = ProductDetailUiState.InvalidArguments
            return
        }
        _uiState.value = ProductDetailUiState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            merchantName = resolveMerchantName()
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
        val initializedFromCart = mode == ProductDetailMode.EDIT && !configurationInitialized
        if (initializedFromCart && !restoreCartItem(value)) return
        if (!initializedFromCart) selections = when {
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

    private fun add(onCartResult: (AddCartItemResult) -> Unit) {
        if (configuredProductEmitted || submitting) return
        val value = product ?: return
        val result = validator.validate(value, configuration())
        if (result is ProductConfigurationResult.Valid) {
            showValidationErrors = false
            savedStateHandle[ProductDetailSavedStateKeys.VALIDATION_ATTEMPTED] = false
            configuredProductEmitted = true
            submitting = true
            render()
            viewModelScope.launch {
                val merchant = CartMerchant(merchantId, merchantName.ifBlank { merchantId })
                if (mode == ProductDetailMode.EDIT) {
                    val replaceResult = cartRepository.replaceItem(
                        ReplaceCartItemCommand(cartItemId.orEmpty(), merchant, result.configuredProduct, cartSnapshot(value))
                    )
                    submitting = false
                    if (replaceResult is CartResult.Success) {
                        _effects.tryEmit(ProductDetailUiEffect.Success(R.string.product_updated_message))
                    } else {
                        configuredProductEmitted = false
                        _effects.tryEmit(ProductDetailUiEffect.Message(cartErrorMessage((replaceResult as CartResult.Failure).error)))
                        render()
                    }
                } else {
                    val cartResult = addConfiguredProductToCart(merchant, result.configuredProduct, cartSnapshot(value))
                    submitting = false
                    when (cartResult) {
                        is AddCartItemResult.Added -> _effects.tryEmit(ProductDetailUiEffect.Success(
                            if (cartResult.merged) R.string.product_merged_message else R.string.product_added_message
                        ))
                        is AddCartItemResult.Conflict -> {
                            configuredProductEmitted = false
                            val different = cartResult.conflict as CartConflict.DifferentMerchant
                            pendingConflictCommand = AddCartItemCommand(merchant, result.configuredProduct, cartSnapshot(value))
                            conflict = ProductDetailConflictUiModel(
                                different.currentMerchant.name, different.requestedMerchant.name, false
                            )
                            render()
                        }
                        is AddCartItemResult.Failure -> {
                            configuredProductEmitted = false
                            _effects.tryEmit(ProductDetailUiEffect.Message(cartErrorMessage(cartResult.error)))
                            render()
                        }
                    }
                    onCartResult(cartResult)
                }
            }
        } else if (result is ProductConfigurationResult.Invalid) {
            showValidationErrors = true
            savedStateHandle[ProductDetailSavedStateKeys.VALIDATION_ATTEMPTED] = true
            render(result)
        }
    }

    private fun configurationChanged() {
        configuredProductEmitted = false
    }

    private fun restoreCartItem(value: CatalogProduct): Boolean {
        val item = cartRepository.cart.value.items.find { it.id == cartItemId }
        if (item == null) {
            _uiState.value = ProductDetailUiState.NotFound
            return false
        }
        if (item.merchantId != merchantId || item.productId != productId) {
            _uiState.value = ProductDetailUiState.InvalidArguments
            return false
        }
        val itemEncoded = item.selections.map { "${it.groupId}$SELECTION_SEPARATOR${it.optionId}" }
        val reconciledItem = restoreSelections(value, itemEncoded)
        configurationAdjusted = reconciledItem.values.flatten().size != item.selections.size
        selections = if (hasRestoredSelectionState) {
            restoreSelections(value, restoredSelections)
        } else {
            quantity = item.quantity.coerceIn(ProductConfiguration.MIN_QUANTITY, ProductConfiguration.MAX_QUANTITY)
            note = item.note.orEmpty().take(ProductConfiguration.MAX_NOTE_LENGTH)
            reconciledItem
        }
        configurationInitialized = true
        return true
    }

    private fun keepCurrentCart() {
        if (submitting) return
        pendingConflictCommand = null
        conflict = null
        render()
    }

    private fun replaceCart() {
        if (submitting) return
        val command = pendingConflictCommand ?: return
        submitting = true
        conflict = conflict?.copy(processing = true)
        render()
        viewModelScope.launch {
            when (val result = cartRepository.replaceCartWithItem(command)) {
                is CartResult.Success -> {
                    pendingConflictCommand = null
                    conflict = null
                    submitting = false
                    _effects.tryEmit(ProductDetailUiEffect.Success(R.string.product_replaced_cart_message))
                }
                is CartResult.Failure -> {
                    submitting = false
                    conflict = conflict?.copy(processing = false)
                    _effects.tryEmit(ProductDetailUiEffect.Message(cartErrorMessage(result.error)))
                    render()
                }
            }
        }
    }

    private fun cartErrorMessage(error: ao.consuma.aqui.feature.cart.domain.result.CartError): Int = when (error) {
        ao.consuma.aqui.feature.cart.domain.result.CartError.ItemNotFound -> R.string.cart_error_item_not_found
        ao.consuma.aqui.feature.cart.domain.result.CartError.InvalidQuantity -> R.string.cart_error_invalid_quantity
        ao.consuma.aqui.feature.cart.domain.result.CartError.QuantityLimitExceeded -> R.string.cart_error_quantity_limit
        ao.consuma.aqui.feature.cart.domain.result.CartError.CurrencyMismatch -> R.string.cart_error_currency
        ao.consuma.aqui.feature.cart.domain.result.CartError.InvalidItem -> R.string.cart_error_invalid_item
        ao.consuma.aqui.feature.cart.domain.result.CartError.MerchantMismatch -> R.string.cart_error_merchant
        ao.consuma.aqui.feature.cart.domain.result.CartError.PriceOverflow -> R.string.cart_error_price
        else -> R.string.cart_error_generic
    }

    private suspend fun resolveMerchantName(): String = when (
        val result = repository.getCatalog(CatalogRequest(merchantId))
    ) {
        is CatalogResult.Success -> result.data.name
        is CatalogResult.Offline -> result.cachedData?.name
        is CatalogResult.Empty -> result.data?.name
        is CatalogResult.Error -> null
    }?.takeIf(String::isNotBlank) ?: merchantId

    private fun cartSnapshot(value: CatalogProduct) = ProductCartSnapshot(
        productName = value.name,
        imageUrl = value.imageUrl,
        selectedOptions = value.optionGroups
            .sortedBy { it.sortOrder }
            .flatMap { group ->
                val selectedIds = selections[group.id].orEmpty()
                group.options
                    .filter { it.id in selectedIds }
                    .sortedBy { it.sortOrder }
                    .map { option ->
                        CartOptionSnapshot(
                            groupId = group.id,
                            groupName = group.name,
                            optionId = option.id,
                            optionName = option.name,
                            additionalPrice = option.additionalPrice
                        )
                    }
            }
    )

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
                isOffline = offline,
                mode = mode,
                isSubmitting = submitting,
                conflict = conflict,
                configurationAdjusted = configurationAdjusted
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
