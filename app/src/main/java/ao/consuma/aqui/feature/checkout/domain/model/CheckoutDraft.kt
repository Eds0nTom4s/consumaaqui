package ao.consuma.aqui.feature.checkout.domain.model

import java.time.Instant

/** Transient validated checkout intention. This is not an order or payment intent. */
data class CheckoutDraft(
    val id: String,
    val sessionId: String,
    val cartId: String,
    val cartVersion: Long,
    val merchantId: String,
    val merchantName: String,
    val items: List<CheckoutItem>,
    val customer: CheckoutCustomer,
    val fulfillment: CheckoutFulfillment,
    val quote: CheckoutQuote,
    val clientReference: String,
    val createdAt: Instant
) {
    init {
        require(id.isNotBlank() && sessionId.isNotBlank() && clientReference.isNotBlank())
        require(cartId.isNotBlank() && cartVersion >= 0)
        require(merchantId.isNotBlank() && merchantName.isNotBlank() && items.isNotEmpty())
    }
}
