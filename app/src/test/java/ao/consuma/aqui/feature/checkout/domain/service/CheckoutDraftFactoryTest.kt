package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.checkout.SequentialCheckoutIdGenerator
import ao.consuma.aqui.feature.checkout.calculator
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutResult
import ao.consuma.aqui.feature.checkout.fixedClock
import ao.consuma.aqui.feature.checkout.fixedInstant
import ao.consuma.aqui.feature.checkout.readySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutDraftFactoryTest {
    private fun ready() = readySession().let { pending ->
        val quote = (calculator().calculate(pending) as QuoteCalculationResult.Success).quote
        readySession(quote)
    }

    @Test fun `ready session produces transient draft with complete snapshot`() {
        val factory = CheckoutDraftFactory(
            fixedClock, SequentialCheckoutIdGenerator(), CheckoutCustomerValidator()
        )
        val session = ready()
        val draft = (factory.create(session, "cart-one", 4) as CheckoutResult.Success).data
        assertEquals(session.id, draft.sessionId)
        assertEquals(session.cartId, draft.cartId)
        assertEquals(session.cartVersion, draft.cartVersion)
        assertEquals(session.merchantId, draft.merchantId)
        assertEquals(session.items, draft.items)
        assertEquals(session.customer, draft.customer)
        assertEquals(session.fulfillment, draft.fulfillment)
        assertEquals(session.quote, draft.quote)
        assertEquals(fixedInstant, draft.createdAt)
        assertTrue(draft.id.isNotBlank())
        assertTrue(draft.clientReference.isNotBlank())
        assertNotEquals(draft.id, draft.clientReference)
    }

    @Test fun `incomplete session and mismatched cart are rejected`() {
        val factory = CheckoutDraftFactory(
            fixedClock, SequentialCheckoutIdGenerator(), CheckoutCustomerValidator()
        )
        val incomplete = readySession()
        assertEquals(
            CheckoutError.InvalidState,
            (factory.create(incomplete, "cart-one", 4) as CheckoutResult.Failure).error
        )
        assertEquals(
            CheckoutError.InvalidState,
            (factory.create(ready(), "other", 4) as CheckoutResult.Failure).error
        )
    }

    @Test fun `expired quote is rejected`() {
        val expired = ready().let {
            it.copy(quote = it.quote?.copy(expiresAt = fixedInstant.minusSeconds(1)))
        }
        val factory = CheckoutDraftFactory(
            fixedClock, SequentialCheckoutIdGenerator(), CheckoutCustomerValidator()
        )
        assertEquals(
            CheckoutError.QuoteExpired,
            (factory.create(expired, "cart-one", 4) as CheckoutResult.Failure).error
        )
    }

    @Test fun `quote expires exactly at clock instant while null expiration remains valid`() {
        val factory = CheckoutDraftFactory(
            fixedClock, SequentialCheckoutIdGenerator(), CheckoutCustomerValidator()
        )
        val exact = ready().let { session ->
            session.copy(quote = session.quote?.copy(expiresAt = fixedInstant))
        }
        assertEquals(
            CheckoutError.QuoteExpired,
            (factory.create(exact, "cart-one", 4) as CheckoutResult.Failure).error
        )
        val withoutExpiration = ready().let { session ->
            session.copy(quote = session.quote?.copy(expiresAt = null))
        }
        assertTrue(factory.create(withoutExpiration, "cart-one", 4) is CheckoutResult.Success)
    }

    @Test fun `each independent factory call generates unique local identities`() {
        val ids = SequentialCheckoutIdGenerator()
        val factory = CheckoutDraftFactory(fixedClock, ids, CheckoutCustomerValidator())
        val first = (factory.create(ready(), "cart-one", 4) as CheckoutResult.Success).data
        val second = (factory.create(ready(), "cart-one", 4) as CheckoutResult.Success).data
        assertNotEquals(first.id, second.id)
        assertNotEquals(first.clientReference, second.clientReference)
        assertEquals(CheckoutSessionStatus.ReadyForReview, ready().status)
    }
}
