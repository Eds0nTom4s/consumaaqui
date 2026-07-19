package ao.consuma.aqui.feature.cart.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.cartItem
import ao.consuma.aqui.feature.cart.domain.result.CartError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartTotalsCalculatorTest {
    private val calculator = CartTotalsCalculator()

    @Test fun `empty cart uses zero counts and requested currency`() {
        val result = calculator.calculate(emptyList(), "USD") as CartTotalsCalculationResult.Success
        assertEquals(0, result.totals.itemCount)
        assertEquals(0, result.totals.distinctItemCount)
        assertEquals(MoneyAmount(0, "USD"), result.totals.subtotal)
    }

    @Test fun `one and multiple items calculate quantities lines and subtotal`() {
        val one = calculator.calculate(listOf(cartItem(quantity = 2))) as CartTotalsCalculationResult.Success
        assertEquals(2, one.totals.itemCount)
        assertEquals(1, one.totals.distinctItemCount)
        assertEquals(2_400L, one.totals.subtotal.amountMinor)

        val multiple = calculator.calculate(
            listOf(
                cartItem(quantity = 2),
                cartItem(id = "two", productId = "two", quantity = 3, fingerprint = "two")
            )
        ) as CartTotalsCalculationResult.Success
        assertEquals(5, multiple.totals.itemCount)
        assertEquals(2, multiple.totals.distinctItemCount)
        assertEquals(6_000L, multiple.totals.subtotal.amountMinor)
    }

    @Test fun `currency mismatch is explicit`() {
        val result = calculator.calculate(
            listOf(
                cartItem(),
                cartItem(id = "two", productId = "two", currency = "USD", fingerprint = "two")
            )
        ) as CartTotalsCalculationResult.Failure
        assertEquals(CartError.CurrencyMismatch, result.error)
    }

    @Test fun `subtotal overflow is explicit`() {
        val huge = cartItem().copy(
            unitPrice = MoneyAmount(Long.MAX_VALUE, "AOA"),
            optionsPrice = MoneyAmount(0, "AOA"),
            selections = emptyList(),
            totalUnitPrice = MoneyAmount(Long.MAX_VALUE, "AOA"),
            totalPrice = MoneyAmount(Long.MAX_VALUE, "AOA")
        )
        val result = calculator.calculate(
            listOf(huge, huge.copy(id = "two", configurationFingerprint = "two"))
        )
        assertTrue(result is CartTotalsCalculationResult.Failure)
        assertEquals(CartError.PriceOverflow, (result as CartTotalsCalculationResult.Failure).error)
    }

    @Test fun `item order does not change totals`() {
        val first = cartItem(quantity = 2)
        val second = cartItem(id = "two", productId = "two", quantity = 3, fingerprint = "two")
        val forward = calculator.calculate(listOf(first, second)) as CartTotalsCalculationResult.Success
        val reverse = calculator.calculate(listOf(second, first)) as CartTotalsCalculationResult.Success
        assertEquals(forward.totals, reverse.totals)
    }
}
