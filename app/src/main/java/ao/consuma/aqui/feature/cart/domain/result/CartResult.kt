package ao.consuma.aqui.feature.cart.domain.result

import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.cart.domain.model.CartConflict
import ao.consuma.aqui.feature.cart.domain.model.CartItem

sealed interface CartError {
    data object ItemNotFound : CartError
    data object InvalidQuantity : CartError
    data object QuantityLimitExceeded : CartError
    data object CurrencyMismatch : CartError
    data object InvalidItem : CartError
    data object MerchantMismatch : CartError
    data object PriceOverflow : CartError
    data object EmptyCart : CartError
    data object Unknown : CartError
}

sealed interface CartResult<out T> {
    data class Success<T>(val data: T) : CartResult<T>
    data class Failure(val error: CartError) : CartResult<Nothing>
}

sealed interface AddCartItemResult {
    data class Added(
        val cart: Cart,
        val item: CartItem,
        val merged: Boolean
    ) : AddCartItemResult

    data class Conflict(val conflict: CartConflict) : AddCartItemResult
    data class Failure(val error: CartError) : AddCartItemResult
}
