package ao.consuma.aqui.feature.cart.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount

data class CartTotals(
    val itemCount: Int,
    val distinctItemCount: Int,
    val subtotal: MoneyAmount
) {
    init {
        require(itemCount >= 0) { "Item count cannot be negative" }
        require(distinctItemCount >= 0) { "Distinct item count cannot be negative" }
        require(distinctItemCount <= itemCount) { "Distinct count cannot exceed item count" }
        require((itemCount == 0) == (distinctItemCount == 0)) {
            "Empty item and distinct counts must agree"
        }
    }

    companion object {
        const val DEFAULT_EMPTY_CURRENCY = "AOA"

        fun empty(currencyCode: String = DEFAULT_EMPTY_CURRENCY) = CartTotals(
            itemCount = 0,
            distinctItemCount = 0,
            subtotal = MoneyAmount(0, currencyCode)
        )
    }
}
