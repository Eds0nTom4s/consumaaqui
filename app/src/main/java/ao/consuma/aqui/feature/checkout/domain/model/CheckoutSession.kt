package ao.consuma.aqui.feature.checkout.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount

sealed interface CheckoutSessionStatus {
    data object Started : CheckoutSessionStatus
    data object CustomerPending : CheckoutSessionStatus
    data object FulfillmentPending : CheckoutSessionStatus
    data object QuotePending : CheckoutSessionStatus
    data object ReadyForReview : CheckoutSessionStatus
    data object ConfirmedMock : CheckoutSessionStatus
    data object Invalidated : CheckoutSessionStatus
}

enum class CheckoutStep { FULFILLMENT, CUSTOMER, DETAILS, REVIEW, CONFIRMATION }

data class CheckoutSession(
    val id: String,
    val cartId: String,
    val cartVersion: Long,
    val cartItemCount: Int,
    val cartSubtotal: MoneyAmount,
    val currencyCode: String,
    val merchantId: String,
    val merchantName: String,
    val items: List<CheckoutItem>,
    val capabilities: CheckoutCapabilities,
    val selectedFulfillmentMethod: FulfillmentMethod?,
    val customer: CheckoutCustomer?,
    val fulfillment: CheckoutFulfillment?,
    val quote: CheckoutQuote?,
    val status: CheckoutSessionStatus,
    val version: Long
) {
    init {
        require(id.isNotBlank() && cartId.isNotBlank()) { "Session and cart ids are required" }
        require(cartVersion >= 0 && version >= 0) { "Versions cannot be negative" }
        require(merchantId.isNotBlank() && merchantName.isNotBlank()) { "Merchant is required" }
        require(items.isNotEmpty()) { "Checkout cannot start with an empty cart" }
        require(items.all { it.merchantId == merchantId }) { "All items must use one merchant" }
        require(items.all { it.totalPrice.currencyCode == currencyCode }) { "All items must use one currency" }
        require(cartSubtotal.currencyCode == currencyCode) { "Subtotal currency must match session" }
        require(cartItemCount == items.fold(0) { count, item -> Math.addExact(count, item.quantity) }) {
            "Cart item count must match snapshot"
        }
        require(items.fold(MoneyAmount(0, currencyCode)) { sum, item -> sum.add(item.totalPrice) } == cartSubtotal) {
            "Cart subtotal must match snapshot"
        }
        require(fulfillment == null || fulfillment.method == selectedFulfillmentMethod) {
            "Fulfillment details must match selected method"
        }
        require(selectedFulfillmentMethod == null || capabilities.supports(selectedFulfillmentMethod)) {
            "Selected fulfillment must be available"
        }
        quote?.let {
            require(it.sessionId == id && it.cartVersion == cartVersion) { "Quote must match session" }
            require(it.subtotal == cartSubtotal && it.currencyCode == currencyCode) {
                "Quote must match cart snapshot"
            }
        }
        require(status != CheckoutSessionStatus.ReadyForReview ||
            (customer != null && fulfillment != null && quote != null)) {
            "Ready session must be complete"
        }
        require(status != CheckoutSessionStatus.ConfirmedMock ||
            (customer != null && fulfillment != null && quote != null)) {
            "Confirmed session must be complete"
        }
    }
}
