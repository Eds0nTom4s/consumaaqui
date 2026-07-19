package ao.consuma.aqui.feature.cart.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.domain.model.CartItem
import ao.consuma.aqui.feature.cart.domain.model.CartTotals
import ao.consuma.aqui.feature.cart.domain.result.CartError
import javax.inject.Inject

sealed interface CartTotalsCalculationResult {
    data class Success(val totals: CartTotals) : CartTotalsCalculationResult
    data class Failure(val error: CartError) : CartTotalsCalculationResult
}

class CartTotalsCalculator @Inject constructor() {
    fun calculate(
        items: List<CartItem>,
        emptyCurrencyCode: String = CartTotals.DEFAULT_EMPTY_CURRENCY
    ): CartTotalsCalculationResult {
        if (items.isEmpty()) {
            return CartTotalsCalculationResult.Success(CartTotals.empty(emptyCurrencyCode))
        }
        val currency = items.first().totalPrice.currencyCode
        if (items.any { it.totalPrice.currencyCode != currency }) {
            return CartTotalsCalculationResult.Failure(CartError.CurrencyMismatch)
        }
        return try {
            val itemCount = items.fold(0) { count, item -> Math.addExact(count, item.quantity) }
            val subtotal = items.fold(MoneyAmount(0, currency)) { value, item ->
                value.add(item.totalPrice)
            }
            CartTotalsCalculationResult.Success(
                CartTotals(itemCount, items.size, subtotal)
            )
        } catch (_: ArithmeticException) {
            CartTotalsCalculationResult.Failure(CartError.PriceOverflow)
        }
    }
}
