package ao.consuma.aqui.core.domain.model

/** Currency-safe monetary value in the currency's minor unit. */
data class MoneyAmount(
    val amountMinor: Long,
    val currencyCode: String
) {
    init {
        require(amountMinor >= 0) { "Money amount cannot be negative" }
        require(CURRENCY_CODE.matches(currencyCode)) {
            "Currency code must be a three-letter uppercase ISO-style code"
        }
    }

    fun hasSameCurrency(other: MoneyAmount): Boolean = currencyCode == other.currencyCode

    fun add(other: MoneyAmount): MoneyAmount {
        require(hasSameCurrency(other)) { "Money amounts must use the same currency" }
        return MoneyAmount(Math.addExact(amountMinor, other.amountMinor), currencyCode)
    }

    fun multiply(quantity: Int): MoneyAmount {
        require(quantity > 0) { "Quantity must be positive" }
        return MoneyAmount(Math.multiplyExact(amountMinor, quantity.toLong()), currencyCode)
    }

    private companion object {
        val CURRENCY_CODE = Regex("[A-Z]{3}")
    }
}
