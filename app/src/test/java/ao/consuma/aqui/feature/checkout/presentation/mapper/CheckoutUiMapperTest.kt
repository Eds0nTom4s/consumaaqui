package ao.consuma.aqui.feature.checkout.presentation.mapper

import ao.consuma.aqui.R
import ao.consuma.aqui.feature.checkout.SequentialCheckoutIdGenerator
import ao.consuma.aqui.feature.checkout.calculator
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.domain.model.DeliveryAddress
import ao.consuma.aqui.feature.checkout.domain.model.DeliveryDetails
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutResult
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutCustomerValidator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutDraftFactory
import ao.consuma.aqui.feature.checkout.domain.service.QuoteCalculationResult
import ao.consuma.aqui.feature.checkout.fixedClock
import ao.consuma.aqui.feature.checkout.readySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutUiMapperTest {
    private val mapper = CheckoutUiMapper(fixedClock)

    @Test fun `steps expose ordered current completed and future states`() {
        val steps = mapper.steps(CheckoutStep.DETAILS)
        assertEquals(
            listOf(CheckoutStep.FULFILLMENT, CheckoutStep.CUSTOMER, CheckoutStep.DETAILS, CheckoutStep.REVIEW),
            steps.map { it.step }
        )
        assertTrue(steps[0].completed)
        assertTrue(steps[1].completed)
        assertTrue(steps[2].current)
        assertFalse(steps[3].completed)
    }

    @Test fun `capabilities cart items selections notes and money map from checkout snapshot`() {
        val session = readySession()
        val capabilities = mapper.capabilities(session)
        val cart = mapper.cart(session)
        assertTrue(capabilities.pickupAvailable)
        assertTrue(capabilities.deliveryAvailable)
        assertEquals("Sabor da Maianga", cart.merchantName)
        assertEquals(1, cart.itemCount)
        assertEquals("12 Kz", cart.subtotal)
        assertEquals("large", session.items.single().selections.single().optionId)
        assertEquals("Grande", cart.items.single().selections.single().optionName)
        assertNull(cart.items.single().note)
    }

    @Test fun `pickup quote omits charges and maps estimates expiration and total`() {
        val pending = readySession()
        val quote = (calculator().calculate(pending) as QuoteCalculationResult.Success).quote
        val value = mapper.quote(readySession(quote))!!
        assertTrue(value.charges.isEmpty())
        assertEquals(value.subtotal, value.total)
        assertEquals(20, value.preparationMinutes)
        assertNull(value.deliveryMinutes)
        assertEquals("10:10", value.expiresAtText)
        assertFalse(value.expired)
    }

    @Test fun `delivery quote maps charge address and delivery estimate`() {
        val delivery = readySession().copy(
            selectedFulfillmentMethod = FulfillmentMethod.DELIVERY_MOCK,
            fulfillment = CheckoutFulfillment.Delivery(
                DeliveryDetails(
                    DeliveryAddress(
                        "Luanda", "Talatona", "Benfica", "Rua 10", "Casa azul",
                        "Banco BIC", null, null
                    ),
                    "Ligar ao chegar", "Ana Silva", "+244923456789"
                )
            )
        )
        val quote = (calculator().calculate(delivery) as QuoteCalculationResult.Success).quote
        val session = delivery.copy(
            quote = quote,
            status = CheckoutSessionStatus.ReadyForReview
        )
        val mappedQuote = mapper.quote(session)!!
        assertEquals(1, mappedQuote.charges.size)
        assertEquals(
            R.string.checkout_delivery_charge,
            (mappedQuote.charges.single().label as CheckoutUiText.Resource).id
        )
        assertEquals(35, mappedQuote.deliveryMinutes)
        val fulfillment = mapper.fulfillment(session) as CheckoutFulfillmentReviewUiModel.Delivery
        assertTrue(fulfillment.addressLines.contains("Banco BIC"))
        assertEquals("Ligar ao chegar", fulfillment.instructions)
    }

    @Test fun `forced quote expiration is explicit without changing domain quote`() {
        val pending = readySession()
        val quote = (calculator().calculate(pending) as QuoteCalculationResult.Success).quote
        val session = readySession(quote)
        assertTrue(mapper.quote(session, forceExpired = true)!!.expired)
        assertFalse(mapper.quote(session)!!.expired)
    }

    @Test fun `confirmation maps simulation reference merchant total and method`() {
        val pending = readySession()
        val quote = (calculator().calculate(pending) as QuoteCalculationResult.Success).quote
        val session = readySession(quote)
        val ids = SequentialCheckoutIdGenerator()
        val draft = (CheckoutDraftFactory(fixedClock, ids, CheckoutCustomerValidator()).create(
            session,
            session.cartId,
            session.cartVersion
        ) as CheckoutResult.Success).data
        val confirmation = mapper.confirmation(draft)
        assertEquals(8, confirmation.clientReference.length)
        assertEquals("Sabor da Maianga", confirmation.merchantName)
        assertEquals("12 Kz", confirmation.total)
        assertEquals(FulfillmentMethod.PICKUP, confirmation.method)
    }

    @Test fun `domain errors map only to user-facing resources`() {
        listOf(
            CheckoutError.EmptyCart,
            CheckoutError.InvalidCart,
            CheckoutError.InvalidCustomer,
            CheckoutError.InvalidAddress,
            CheckoutError.QuoteUnavailable,
            CheckoutError.CurrencyMismatch,
            CheckoutError.Unknown
        ).forEach { assertTrue(mapper.error(it).id > 0) }
        assertEquals(R.string.checkout_error_empty_cart, mapper.error(CheckoutError.EmptyCart).id)
        assertEquals(R.string.checkout_quote_expired, mapper.error(CheckoutError.QuoteExpired).id)
    }
}
