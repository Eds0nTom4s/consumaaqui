package ao.consuma.aqui.feature.cart.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount

/**
 * Local snapshot of a validated product configuration.
 *
 * Prices and subtotal derived from this snapshot are local estimates from the mock catalog, not
 * confirmed financial values. A future backend must revalidate product availability, stock,
 * options, promotions and prices, and reconcile the cart during checkout.
 */
data class CartItem(
    val id: String,
    val merchantId: String,
    val productId: String,
    val productName: String,
    val productImageUrl: String?,
    val quantity: Int,
    val note: String?,
    val selections: List<CartItemSelection>,
    val unitPrice: MoneyAmount,
    val optionsPrice: MoneyAmount,
    val totalUnitPrice: MoneyAmount,
    val totalPrice: MoneyAmount,
    val configurationFingerprint: String
) {
    init {
        require(id.isNotBlank()) { "Cart item id is required" }
        require(merchantId.isNotBlank()) { "Merchant id is required" }
        require(productId.isNotBlank()) { "Product id is required" }
        require(productName.isNotBlank()) { "Product name is required" }
        require(quantity in MIN_QUANTITY..MAX_QUANTITY) {
            "Cart item quantity must be between 1 and 99"
        }
        require(note == null || note == note.trim().takeIf(String::isNotEmpty)) {
            "Cart item note must be normalized"
        }
        require(configurationFingerprint.isNotBlank()) { "Configuration fingerprint is required" }
        require(selections == selections.sortedWith(cartSelectionComparator)) {
            "Cart item selections must have deterministic order"
        }
        require(selections.distinctBy { it.groupId to it.optionId }.size == selections.size) {
            "Cart item selections must be unique"
        }
        val currency = unitPrice.currencyCode
        require(listOf(optionsPrice, totalUnitPrice, totalPrice).all { it.currencyCode == currency }) {
            "Cart item prices must use one currency"
        }
        require(selections.all { it.additionalPrice?.currencyCode in setOf(null, currency) }) {
            "Selection prices must use the item currency"
        }
        val expectedOptions = selections.mapNotNull(CartItemSelection::additionalPrice)
            .fold(MoneyAmount(0, currency), MoneyAmount::add)
        require(optionsPrice == expectedOptions) { "Options price must match selection prices" }
        require(totalUnitPrice == unitPrice.add(optionsPrice)) {
            "Total unit price must match base and options prices"
        }
        require(totalPrice == totalUnitPrice.multiply(quantity)) {
            "Total price must match unit price and quantity"
        }
    }

    companion object {
        const val MIN_QUANTITY = 1
        const val MAX_QUANTITY = 99
    }
}
