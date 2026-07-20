package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.checkout.calculator
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutChargeType
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutQuoteSource
import ao.consuma.aqui.feature.checkout.domain.model.DeliveryAddress
import ao.consuma.aqui.feature.checkout.domain.model.DeliveryDetails
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import ao.consuma.aqui.feature.checkout.fixedInstant
import ao.consuma.aqui.feature.checkout.readySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutQuoteCalculatorTest {
    @Test fun `pickup has no charges and total equals snapshot subtotal`() {
        val quote = (calculator().calculate(readySession()) as QuoteCalculationResult.Success).quote
        assertTrue(quote.charges.isEmpty())
        assertEquals(quote.subtotal, quote.total)
        assertEquals(MoneyAmount(1_200, "AOA"), quote.total)
        assertEquals("AOA", quote.currencyCode)
        assertEquals(20, quote.estimatedPreparationMinutes)
        assertNull(quote.estimatedDeliveryMinutes)
        assertEquals(CheckoutQuoteSource.MockFixedPolicy, quote.source)
    }

    @Test fun `delivery applies deterministic municipality fee and estimates`() {
        fun delivery(municipality: String) = readySession().copy(
            selectedFulfillmentMethod = FulfillmentMethod.DELIVERY_MOCK,
            fulfillment = CheckoutFulfillment.Delivery(
                DeliveryDetails(
                    DeliveryAddress("Luanda", municipality, null, "Rua", null, null, null, null),
                    null, "Ana Silva", "+244923456789"
                )
            )
        )
        val luanda = (calculator().calculate(delivery(" Luanda ")) as QuoteCalculationResult.Success).quote
        assertEquals(CheckoutChargeType.DELIVERY_ESTIMATE, luanda.charges.single().type)
        assertEquals(150_000L, luanda.charges.single().amount.amountMinor)
        assertEquals(151_200L, luanda.total.amountMinor)
        assertEquals(25, luanda.estimatedPreparationMinutes)
        assertEquals(35, luanda.estimatedDeliveryMinutes)

        val other = (calculator().calculate(delivery("Benguela")) as QuoteCalculationResult.Success).quote
        assertEquals(200_000L, other.charges.single().amount.amountMinor)
        assertEquals(201_200L, other.total.amountMinor)
    }

    @Test fun `quote expires in ten minutes using injected clock and is deterministic`() {
        val first = (calculator().calculate(readySession()) as QuoteCalculationResult.Success).quote
        val second = (calculator().calculate(readySession()) as QuoteCalculationResult.Success).quote
        assertEquals(fixedInstant.plusSeconds(600), first.expiresAt)
        assertEquals(first.total, second.total)
        assertEquals(first.expiresAt, second.expiresAt)
    }

    @Test fun `expired fixture is already expired`() {
        val quote = (calculator().calculate(readySession(), expiredFixture = true)
            as QuoteCalculationResult.Success).quote
        assertEquals(fixedInstant.minusSeconds(1), quote.expiresAt)
    }

    @Test fun `missing fulfillment and total overflow are explicit`() {
        val missing = readySession().copy(
            selectedFulfillmentMethod = null,
            fulfillment = null,
            status = ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus.Started
        )
        assertEquals(
            CheckoutError.InvalidState,
            (calculator().calculate(missing) as QuoteCalculationResult.Failure).error
        )
        val huge = readySession(subtotalMinor = Long.MAX_VALUE).copy(
            selectedFulfillmentMethod = FulfillmentMethod.DELIVERY_MOCK,
            fulfillment = CheckoutFulfillment.Delivery(
                DeliveryDetails(
                    DeliveryAddress("Luanda", "Luanda", null, "Rua", null, null, null, null),
                    null, "Ana Silva", "+244923456789"
                )
            )
        )
        assertEquals(
            CheckoutError.PriceOverflow,
            (calculator().calculate(huge) as QuoteCalculationResult.Failure).error
        )
    }
}
