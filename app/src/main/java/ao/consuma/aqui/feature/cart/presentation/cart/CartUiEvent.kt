package ao.consuma.aqui.feature.cart.presentation.cart

import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText

sealed interface CartUiEvent {
    data object Load : CartUiEvent
    data class IncreaseQuantity(val cartItemId: String) : CartUiEvent
    data class DecreaseQuantity(val cartItemId: String) : CartUiEvent
    data class EditItem(val cartItemId: String) : CartUiEvent
    data class RequestRemoveItem(val cartItemId: String) : CartUiEvent
    data object ConfirmRemoveItem : CartUiEvent
    data object CancelRemoveItem : CartUiEvent
    data object RequestClearCart : CartUiEvent
    data object ConfirmClearCart : CartUiEvent
    data object CancelClearCart : CartUiEvent
    data object ExploreMerchants : CartUiEvent
    data object ContinueShopping : CartUiEvent
    data object Retry : CartUiEvent
}

sealed interface CartUiEffect {
    data class EditItem(val merchantId: String, val productId: String, val cartItemId: String) : CartUiEffect
    data class ContinueShopping(val merchantId: String) : CartUiEffect
    data object ExploreMerchants : CartUiEffect
    data class Message(val text: CartUiText) : CartUiEffect
}
