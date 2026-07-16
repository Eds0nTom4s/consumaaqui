package ao.consuma.aqui.feature.catalog.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductPriceCalculatorTest {
    @Test fun `base options unit and quantity totals use minor units`() {
        val result = ProductPriceCalculator.calculate(
            MoneyAmount(1_000, "AOA"),
            listOf(MoneyAmount(200, "AOA"), MoneyAmount(50, "AOA")),
            quantity = 3
        ) as PriceCalculationResult.Success
        assertEquals(1_000, result.prices.unitPrice.amountMinor)
        assertEquals(250, result.prices.optionsPrice.amountMinor)
        assertEquals(1_250, result.prices.totalUnitPrice.amountMinor)
        assertEquals(3_750, result.prices.totalPrice.amountMinor)
    }

    @Test fun `different option currency is rejected`() {
        assertTrue(
            ProductPriceCalculator.calculate(
                MoneyAmount(1_000, "AOA"),
                listOf(MoneyAmount(10, "USD")),
                1
            ) is PriceCalculationResult.CurrencyMismatch
        )
    }

    @Test fun `addition and multiplication overflow are explicit`() {
        assertTrue(
            ProductPriceCalculator.calculate(
                MoneyAmount(Long.MAX_VALUE, "AOA"),
                listOf(MoneyAmount(1, "AOA")),
                1
            ) is PriceCalculationResult.Overflow
        )
        assertTrue(
            ProductPriceCalculator.calculate(
                MoneyAmount(Long.MAX_VALUE, "AOA"),
                emptyList(),
                2
            ) is PriceCalculationResult.Overflow
        )
    }

    @Test fun `invalid quantity is explicit`() {
        assertTrue(
            ProductPriceCalculator.calculate(
                MoneyAmount(100, "AOA"),
                emptyList(),
                0
            ) is PriceCalculationResult.InvalidQuantity
        )
        assertTrue(
            ProductPriceCalculator.calculate(MoneyAmount(100, "AOA"), emptyList(), -1) is
                PriceCalculationResult.InvalidQuantity
        )
        assertTrue(
            ProductPriceCalculator.calculate(MoneyAmount(100, "AOA"), emptyList(), 100) is
                PriceCalculationResult.InvalidQuantity
        )
    }

    @Test fun `free options and no options keep base price unchanged`() {
        val none = ProductPriceCalculator.calculate(MoneyAmount(900, "AOA"), emptyList(), 1)
            as PriceCalculationResult.Success
        val free = ProductPriceCalculator.calculate(
            MoneyAmount(900, "AOA"), listOf(MoneyAmount(0, "AOA")), 1
        ) as PriceCalculationResult.Success
        assertEquals(900, none.prices.totalPrice.amountMinor)
        assertEquals(none.prices, free.prices)
        assertEquals(0, free.prices.optionsPrice.amountMinor)
    }

    @Test fun `calculation is deterministic and does not mutate base`() {
        val base = MoneyAmount(1_000, "AOA")
        val options = listOf(MoneyAmount(100, "AOA"), MoneyAmount(200, "AOA"))
        val first = ProductPriceCalculator.calculate(base, options, 2)
        val second = ProductPriceCalculator.calculate(base, options, 2)
        assertEquals(first, second)
        assertEquals(1_000, base.amountMinor)
    }
}
