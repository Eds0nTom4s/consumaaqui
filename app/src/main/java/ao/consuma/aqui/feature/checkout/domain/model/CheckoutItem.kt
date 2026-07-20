package ao.consuma.aqui.feature.checkout.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount

/** Stable checkout-owned snapshot of a cart line. */
data class CheckoutItem(
    val cartItemId: String,
    val merchantId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val selections: List<CheckoutItemSelection>,
    val note: String?,
    val unitPrice: MoneyAmount,
    val totalPrice: MoneyAmount,
    val configurationFingerprint: String
) {
    init {
        require(cartItemId.isNotBlank()) { "Cart item id is required" }
        require(merchantId.isNotBlank()) { "Merchant id is required" }
        require(productId.isNotBlank()) { "Product id is required" }
        require(productName.isNotBlank()) { "Product name is required" }
        require(quantity > 0) { "Quantity must be positive" }
        require(configurationFingerprint.isNotBlank()) { "Configuration fingerprint is required" }
        require(unitPrice.currencyCode == totalPrice.currencyCode) { "Item currency must be consistent" }
        require(totalPrice == unitPrice.multiply(quantity)) { "Item total must match unit price" }
        require(selections.all { it.additionalPrice?.currencyCode in setOf(null, unitPrice.currencyCode) }) {
            "Selection currency must match item currency"
        }
    }
}

data class CheckoutItemSelection(
    val groupId: String,
    val groupName: String,
    val optionId: String,
    val optionName: String,
    val additionalPrice: MoneyAmount?
) {
    init {
        require(groupId.isNotBlank() && optionId.isNotBlank()) { "Selection ids are required" }
        require(groupName.isNotBlank() && optionName.isNotBlank()) { "Selection names are required" }
    }
}
