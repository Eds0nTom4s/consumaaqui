package ao.consuma.aqui.feature.checkout.domain.result

sealed interface CheckoutError {
    data object EmptyCart : CheckoutError
    data object InvalidCart : CheckoutError
    data object CartChanged : CheckoutError
    data object InvalidCustomer : CheckoutError
    data object InvalidPhone : CheckoutError
    data object InvalidEmail : CheckoutError
    data object FulfillmentUnavailable : CheckoutError
    data object InvalidAddress : CheckoutError
    data object InvalidPickupDetails : CheckoutError
    data object QuoteUnavailable : CheckoutError
    data object QuoteExpired : CheckoutError
    data object CurrencyMismatch : CheckoutError
    data object PriceOverflow : CheckoutError
    data object SessionNotFound : CheckoutError
    data object SessionAlreadyConfirmed : CheckoutError
    data object InvalidState : CheckoutError
    data object Unknown : CheckoutError
}

sealed interface CheckoutConflict {
    data class CartChanged(
        val initialCartId: String,
        val currentCartId: String,
        val initialVersion: Long,
        val currentVersion: Long
    ) : CheckoutConflict

    data class CartCleared(val initialCartId: String, val initialVersion: Long) : CheckoutConflict

    data class MerchantChanged(val initialMerchantId: String, val currentMerchantId: String?) : CheckoutConflict

    data class CurrencyChanged(val initialCurrency: String, val currentCurrency: String) : CheckoutConflict
}

sealed interface CheckoutResult<out T> {
    data class Success<T>(val data: T) : CheckoutResult<T>
    data class Conflict(val conflict: CheckoutConflict) : CheckoutResult<Nothing>
    data class Failure(val error: CheckoutError) : CheckoutResult<Nothing>
}
