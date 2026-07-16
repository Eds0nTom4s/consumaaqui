package ao.consuma.aqui.feature.catalog.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.catalog.domain.model.ProductConfiguration

data class ProductPriceBreakdown(
    val unitPrice: MoneyAmount,
    val optionsPrice: MoneyAmount,
    val totalUnitPrice: MoneyAmount,
    val totalPrice: MoneyAmount
)

sealed interface PriceCalculationResult {
    data class Success(val prices: ProductPriceBreakdown) : PriceCalculationResult
    data object CurrencyMismatch : PriceCalculationResult
    data object InvalidQuantity : PriceCalculationResult
    data object Overflow : PriceCalculationResult
}

/** Client estimate only. The backend remains the financial authority. */
object ProductPriceCalculator {
    fun calculate(
        basePrice: MoneyAmount,
        selectedOptionPrices: List<MoneyAmount>,
        quantity: Int
    ): PriceCalculationResult {
        if (quantity !in ProductConfiguration.MIN_QUANTITY..ProductConfiguration.MAX_QUANTITY) {
            return PriceCalculationResult.InvalidQuantity
        }
        if (selectedOptionPrices.any { !basePrice.hasSameCurrency(it) }) {
            return PriceCalculationResult.CurrencyMismatch
        }
        return try {
            val optionsPrice = selectedOptionPrices.fold(MoneyAmount(0, basePrice.currencyCode)) { total, price ->
                total.add(price)
            }
            val totalUnitPrice = basePrice.add(optionsPrice)
            val totalPrice = totalUnitPrice.multiply(quantity)
            PriceCalculationResult.Success(
                ProductPriceBreakdown(
                    unitPrice = basePrice,
                    optionsPrice = optionsPrice,
                    totalUnitPrice = totalUnitPrice,
                    totalPrice = totalPrice
                )
            )
        } catch (_: ArithmeticException) {
            PriceCalculationResult.Overflow
        }
    }
}
