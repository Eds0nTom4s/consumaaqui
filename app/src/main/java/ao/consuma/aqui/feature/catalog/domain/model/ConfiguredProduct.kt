package ao.consuma.aqui.feature.catalog.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount

/**
 * Validated client-side purchase intent. It is not a cart or order line.
 * The server must recalculate every price before treating it as financial truth.
 */
data class ConfiguredProduct(
    val merchantId: String,
    val productId: String,
    val quantity: Int,
    val selectedOptionIds: Map<String, Set<String>>,
    val note: String?,
    val unitPrice: MoneyAmount,
    val optionsPrice: MoneyAmount,
    val totalUnitPrice: MoneyAmount,
    val totalPrice: MoneyAmount
) {
    init {
        require(merchantId.isNotBlank()) { "Merchant id is required" }
        require(productId.isNotBlank()) { "Product id is required" }
        require(quantity in ProductConfiguration.MIN_QUANTITY..ProductConfiguration.MAX_QUANTITY)
        val currency = unitPrice.currencyCode
        require(listOf(optionsPrice, totalUnitPrice, totalPrice).all { it.currencyCode == currency }) {
            "Configured prices must use one currency"
        }
    }
}
