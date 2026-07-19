package ao.consuma.aqui.feature.cart.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount

data class Cart(
    val id: String,
    val merchant: CartMerchant?,
    val items: List<CartItem>,
    val totals: CartTotals,
    val version: Long
) {
    init {
        require(id.isNotBlank()) { "Cart id is required" }
        require(version >= 0) { "Cart version cannot be negative" }
        require((items.isEmpty() && merchant == null) || (items.isNotEmpty() && merchant != null)) {
            "Cart merchant must exist exactly when the cart has items"
        }
        require(items.distinctBy(CartItem::id).size == items.size) { "Cart item ids must be unique" }
        require(items.distinctBy(CartItem::configurationFingerprint).size == items.size) {
            "Equivalent configurations must be aggregated"
        }
        require(merchant == null || items.all { it.merchantId == merchant.id }) {
            "Every cart item must belong to the cart merchant"
        }
        require(totals.itemCount == items.fold(0) { count, item ->
            Math.addExact(count, item.quantity)
        }) { "Cart item count must match items" }
        require(totals.distinctItemCount == items.size) { "Distinct item count must match lines" }
        if (items.isEmpty()) {
            require(totals.subtotal.amountMinor == 0L) { "Empty cart subtotal must be zero" }
        } else {
            val currency = items.first().totalPrice.currencyCode
            require(items.all { it.totalPrice.currencyCode == currency }) {
                "Every item must use the cart currency"
            }
            val expectedSubtotal = items.fold(MoneyAmount(0, currency)) { subtotal, item ->
                subtotal.add(item.totalPrice)
            }
            require(totals.subtotal == expectedSubtotal) { "Cart subtotal must match items" }
        }
    }

    companion object {
        fun empty(
            id: String,
            version: Long = 0,
            currencyCode: String = CartTotals.DEFAULT_EMPTY_CURRENCY
        ) = Cart(id, null, emptyList(), CartTotals.empty(currencyCode), version)
    }
}
