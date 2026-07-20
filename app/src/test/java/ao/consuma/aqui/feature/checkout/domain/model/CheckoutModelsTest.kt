package ao.consuma.aqui.feature.checkout.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.checkout.calculator
import ao.consuma.aqui.feature.checkout.checkoutItem
import ao.consuma.aqui.feature.checkout.domain.service.QuoteCalculationResult
import ao.consuma.aqui.feature.checkout.readySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class CheckoutModelsTest {
    @Test fun `checkout item is a stable exact-price snapshot`() {
        val item = checkoutItem(quantity = 2)
        assertEquals(2_400L, item.totalPrice.amountMinor)
        assertEquals("item-one", item.cartItemId)
        assertThrows(IllegalArgumentException::class.java) {
            item.copy(totalPrice = MoneyAmount(2_401, "AOA"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            item.copy(unitPrice = MoneyAmount(1_200, "USD"))
        }
    }

    @Test fun `checkout item accepts quantity boundaries and rejects invalid identity or quantity`() {
        assertEquals(1, checkoutItem(quantity = 1).quantity)
        assertEquals(99, checkoutItem(quantity = 99).quantity)
        assertNull(checkoutItem().note)
        listOf(0, -1).forEach { quantity ->
            assertThrows(IllegalArgumentException::class.java) { checkoutItem(quantity = quantity) }
        }
        assertThrows(IllegalArgumentException::class.java) { checkoutItem().copy(cartItemId = "") }
        assertThrows(IllegalArgumentException::class.java) { checkoutItem().copy(productId = "") }
        assertThrows(IllegalArgumentException::class.java) { checkoutItem().copy(productName = "") }
        assertThrows(IllegalArgumentException::class.java) {
            checkoutItem().copy(configurationFingerprint = "")
        }
    }

    @Test fun `session rejects empty mixed merchant currency and inconsistent totals`() {
        val session = readySession()
        assertThrows(IllegalArgumentException::class.java) { session.copy(items = emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            session.copy(items = listOf(checkoutItem().copy(merchantId = "other")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            session.copy(cartSubtotal = MoneyAmount(1_201, "AOA"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            session.copy(version = -1)
        }
        assertThrows(IllegalArgumentException::class.java) { session.copy(id = "") }
        assertThrows(IllegalArgumentException::class.java) { session.copy(cartId = "") }
        assertThrows(IllegalArgumentException::class.java) { session.copy(cartVersion = -1) }
        assertThrows(IllegalArgumentException::class.java) { session.copy(merchantId = "") }
        assertThrows(IllegalArgumentException::class.java) { session.copy(merchantName = "") }
        assertThrows(IllegalArgumentException::class.java) { session.copy(cartItemCount = 2) }
    }

    @Test fun `ready status and quote must correspond to session snapshot`() {
        val pending = readySession()
        assertThrows(IllegalArgumentException::class.java) {
            pending.copy(status = CheckoutSessionStatus.ReadyForReview)
        }
        val quote = (calculator().calculate(pending) as QuoteCalculationResult.Success).quote
        val ready = readySession(quote)
        assertEquals(CheckoutSessionStatus.ReadyForReview, ready.status)
        assertThrows(IllegalArgumentException::class.java) {
            ready.copy(quote = quote.copy(sessionId = "other"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ready.copy(quote = quote.copy(cartVersion = quote.cartVersion + 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ready.copy(quote = quote.copy(currencyCode = "USD"))
        }
    }

    @Test fun `checkout totals enforce currency and exact addition`() {
        val subtotal = MoneyAmount(1_000, "AOA")
        val charge = CheckoutCharge(CheckoutChargeType.DELIVERY_ESTIMATE, MoneyAmount(200, "AOA"))
        val totals = CheckoutTotals(subtotal, listOf(charge), MoneyAmount(1_200, "AOA"))
        assertEquals(1_200L, totals.total.amountMinor)
        assertThrows(IllegalArgumentException::class.java) {
            CheckoutTotals(subtotal, listOf(charge), MoneyAmount(1_201, "AOA"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CheckoutTotals(
                subtotal,
                listOf(charge.copy(amount = MoneyAmount(200, "USD"))),
                MoneyAmount(1_200, "AOA")
            )
        }
    }

    @Test fun `fulfillment modalities carry only their own details`() {
        val pickup = readySession().fulfillment
        assertTrue(pickup is CheckoutFulfillment.Pickup)
        assertFalse(pickup is CheckoutFulfillment.Delivery)
        assertEquals(FulfillmentMethod.PICKUP, pickup?.method)
    }

    @Test fun `draft contract contains no order payment or logistics identity`() {
        val propertyNames = CheckoutDraft::class.java.declaredFields.map { it.name.lowercase() }
        assertFalse(propertyNames.any { it == "orderid" || it == "paymentid" || it == "deliveryid" })
    }
}
