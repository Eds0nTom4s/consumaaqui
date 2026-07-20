package ao.consuma.aqui.feature.cart.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.cart.domain.command.RemoveCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.UpdateCartItemQuantityCommand
import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.cart.domain.model.CartItem
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.cart.domain.result.CartError
import ao.consuma.aqui.feature.cart.domain.result.CartResult
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiMapper
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repository: CartRepository,
    private val mapper: CartUiMapper
) : ViewModel() {
    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<CartUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<CartUiEffect> = _effects.asSharedFlow()

    private var pendingOperation: CartPendingOperation? = null
    private var pendingRemovalItemId: String? = null
    private var showClearConfirmation = false

    init {
        viewModelScope.launch { repository.cart.collect(::render) }
    }

    fun onEvent(event: CartUiEvent) {
        when (event) {
            CartUiEvent.Load, CartUiEvent.Retry -> render(repository.cart.value)
            is CartUiEvent.IncreaseQuantity -> changeQuantity(event.cartItemId, 1)
            is CartUiEvent.DecreaseQuantity -> changeQuantity(event.cartItemId, -1)
            is CartUiEvent.EditItem -> edit(event.cartItemId)
            is CartUiEvent.RequestRemoveItem -> {
                if (pendingOperation == null) pendingRemovalItemId = event.cartItemId
                render(repository.cart.value)
            }
            CartUiEvent.ConfirmRemoveItem -> remove()
            CartUiEvent.CancelRemoveItem -> { pendingRemovalItemId = null; render(repository.cart.value) }
            CartUiEvent.RequestClearCart -> {
                if (pendingOperation == null && repository.cart.value.items.isNotEmpty()) showClearConfirmation = true
                render(repository.cart.value)
            }
            CartUiEvent.ConfirmClearCart -> clear()
            CartUiEvent.CancelClearCart -> { showClearConfirmation = false; render(repository.cart.value) }
            CartUiEvent.ExploreMerchants -> _effects.tryEmit(CartUiEffect.ExploreMerchants)
            CartUiEvent.ContinueShopping -> repository.cart.value.merchant?.id?.let {
                _effects.tryEmit(CartUiEffect.ContinueShopping(it))
            }
            CartUiEvent.StartCheckout -> if (repository.cart.value.items.isNotEmpty()) {
                _effects.tryEmit(CartUiEffect.OpenCheckout)
            }
        }
    }

    private fun edit(itemId: String) {
        val item = repository.cart.value.items.find { it.id == itemId } ?: return message(CartError.ItemNotFound)
        _effects.tryEmit(CartUiEffect.EditItem(item.merchantId, item.productId, item.id))
    }

    private fun changeQuantity(itemId: String, delta: Int) {
        if (pendingOperation != null) return
        val item = repository.cart.value.items.find { it.id == itemId } ?: return message(CartError.ItemNotFound)
        val quantity = item.quantity + delta
        if (quantity !in CartItem.MIN_QUANTITY..CartItem.MAX_QUANTITY) return
        mutate(CartPendingOperation.UpdatingQuantity(itemId)) {
            repository.updateQuantity(UpdateCartItemQuantityCommand(itemId, quantity))
        }
    }

    private fun remove() {
        if (pendingOperation != null) return
        val itemId = pendingRemovalItemId ?: return
        pendingRemovalItemId = null
        mutate(CartPendingOperation.Removing(itemId)) {
            repository.removeItem(RemoveCartItemCommand(itemId))
        }
    }

    private fun clear() {
        if (pendingOperation != null || !showClearConfirmation) return
        showClearConfirmation = false
        mutate(CartPendingOperation.Clearing) { repository.clearCart() }
    }

    private fun mutate(operation: CartPendingOperation, block: suspend () -> CartResult<Cart>) {
        pendingOperation = operation
        render(repository.cart.value)
        viewModelScope.launch {
            when (val result = block()) {
                is CartResult.Success -> Unit
                is CartResult.Failure -> message(result.error)
            }
            pendingOperation = null
            render(repository.cart.value)
        }
    }

    private fun render(cart: Cart) {
        _uiState.value = if (cart.items.isEmpty()) {
            CartUiState.Empty(CartUiText.Resource(R.string.cart_empty_description))
        } else {
            val merchant = mapper.merchant(cart) ?: return
            CartUiState.Content(
                cart.id, merchant, mapper.items(cart), cart.totals.itemCount,
                cart.totals.distinctItemCount, mapper.subtotal(cart),
                mapper.subtotal(cart), cart.version,
                pendingOperation, pendingRemovalItemId, showClearConfirmation
            )
        }
    }

    private fun message(error: CartError) {
        _effects.tryEmit(CartUiEffect.Message(errorText(error)))
    }

    private fun errorText(error: CartError) = CartUiText.Resource(when (error) {
        CartError.ItemNotFound -> R.string.cart_error_item_not_found
        CartError.InvalidQuantity -> R.string.cart_error_invalid_quantity
        CartError.QuantityLimitExceeded -> R.string.cart_error_quantity_limit
        CartError.CurrencyMismatch -> R.string.cart_error_currency
        CartError.InvalidItem -> R.string.cart_error_invalid_item
        CartError.MerchantMismatch -> R.string.cart_error_merchant
        CartError.PriceOverflow -> R.string.cart_error_price
        else -> R.string.cart_error_generic
    })
}
