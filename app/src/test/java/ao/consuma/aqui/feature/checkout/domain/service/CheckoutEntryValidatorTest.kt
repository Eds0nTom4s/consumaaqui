package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.checkout.validCart
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutEntryValidatorTest {
    private val validator = CheckoutEntryValidator()

    @Test fun `empty cart is rejected`() {
        assertEquals(
            CheckoutEntryValidationResult.EmptyCart,
            validator.validate(Cart.empty("empty-cart"))
        )
    }

    @Test fun `valid cart creates complete checkout-owned snapshot`() {
        val cart = validCart()
        val result = validator.validate(cart) as CheckoutEntryValidationResult.Valid
        assertEquals(cart.id, result.snapshot.cartId)
        assertEquals(cart.version, result.snapshot.cartVersion)
        assertEquals(cart.merchant?.id, result.snapshot.merchantId)
        assertEquals(cart.totals.itemCount, result.snapshot.itemCount)
        assertEquals(cart.totals.subtotal, result.snapshot.subtotal)
        assertEquals("AOA", result.snapshot.currencyCode)
        assertEquals("item-one", result.snapshot.items.single().cartItemId)
    }

    @Test fun `snapshot preserves product configuration selections and prices`() {
        val cart = validCart()
        val source = cart.items.single()
        val item = (validator.validate(cart) as CheckoutEntryValidationResult.Valid)
            .snapshot.items.single()
        assertEquals(source.productId, item.productId)
        assertEquals(source.configurationFingerprint, item.configurationFingerprint)
        assertEquals(source.note, item.note)
        assertEquals(source.totalUnitPrice, item.unitPrice)
        assertEquals(source.totalPrice, item.totalPrice)
        assertEquals(source.selections.single().optionId, item.selections.single().optionId)
        assertNotSame(source.selections.single(), item.selections.single())
        assertFalse(item::class.qualifiedName.orEmpty().contains("cart.domain.model.CartItem"))
    }

    @Test fun `validation never changes cart`() {
        val cart = validCart()
        val itemsBefore = cart.items.toList()
        validator.validate(cart)
        assertEquals(itemsBefore, cart.items)
        assertEquals(4L, cart.version)
    }

    @Test fun `snapshot collections are defensive and unmodifiable`() {
        val mutableItems = validCart().items.toMutableList()
        val cart = validCart().copy(items = mutableItems)
        val snapshot = (validator.validate(cart) as CheckoutEntryValidationResult.Valid).snapshot
        mutableItems.clear()
        assertEquals(1, snapshot.items.size)
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (snapshot.items as MutableList).add(snapshot.items.single())
        }
        assertTrue(snapshot.items !== Collections.emptyList<Any>())
    }
}
