package ao.consuma.aqui.feature.cart.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.cartItem
import ao.consuma.aqui.feature.cart.merchant
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CartModelsTest {
    @Test fun `empty cart is valid`() {
        val cart = Cart.empty("active-cart")
        assertNull(cart.merchant)
        assertTrue(cart.items.isEmpty())
        assertEquals(0, cart.totals.itemCount)
        assertEquals(0, cart.totals.distinctItemCount)
        assertEquals(0L, cart.totals.subtotal.amountMinor)
    }

    @Test fun `cart with merchant and coherent items is valid`() {
        val item = cartItem(quantity = 2)
        val cart = Cart(
            "cart", merchant(), listOf(item),
            CartTotals(2, 1, MoneyAmount(2_400, "AOA")), 1
        )
        assertEquals("merchant-one", cart.merchant?.id)
        assertEquals(2, cart.totals.itemCount)
    }

    @Test fun `cart requires merchant exactly when it has items`() {
        assertThrows(IllegalArgumentException::class.java) {
            Cart(
                "cart", null, listOf(cartItem()),
                CartTotals(1, 1, MoneyAmount(1_200, "AOA")), 0
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Cart("cart", merchant(), emptyList(), CartTotals.empty(), 0)
        }
    }

    @Test fun `cart rejects mixed merchants duplicate ids and fingerprints`() {
        val first = cartItem()
        assertThrows(IllegalArgumentException::class.java) {
            Cart(
                "cart", merchant(),
                listOf(first, cartItem(id = "two", merchantId = "merchant-two")),
                CartTotals(2, 2, MoneyAmount(2_400, "AOA")), 0
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Cart(
                "cart", merchant(), listOf(first, first),
                CartTotals(2, 2, MoneyAmount(2_400, "AOA")), 0
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Cart(
                "cart", merchant(), listOf(first, cartItem(id = "two")),
                CartTotals(2, 2, MoneyAmount(2_400, "AOA")), 0
            )
        }
    }

    @Test fun `cart rejects inconsistent totals and negative version`() {
        assertThrows(IllegalArgumentException::class.java) {
            Cart("cart", merchant(), listOf(cartItem()), CartTotals.empty(), 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Cart.empty("cart", version = -1)
        }
    }

    @Test fun `repository style lists cannot be mutated`() {
        val values = Collections.unmodifiableList(listOf(cartItem()))
        val cart = Cart(
            "cart", merchant(), values,
            CartTotals(1, 1, MoneyAmount(1_200, "AOA")), 0
        )
        assertThrows(UnsupportedOperationException::class.java) {
            (cart.items as MutableList).add(cartItem(id = "two", fingerprint = "two"))
        }
    }

    @Test fun `cart item accepts boundary quantities`() {
        assertEquals(1, cartItem(quantity = 1).quantity)
        assertEquals(99, cartItem(quantity = 99).quantity)
    }

    @Test fun `cart item rejects invalid quantity and mandatory blank fields`() {
        assertThrows(IllegalArgumentException::class.java) { cartItem(quantity = 0) }
        assertThrows(IllegalArgumentException::class.java) { cartItem(quantity = 100) }
        assertThrows(IllegalArgumentException::class.java) { cartItem(id = "") }
        assertThrows(IllegalArgumentException::class.java) { cartItem(merchantId = "") }
        assertThrows(IllegalArgumentException::class.java) { cartItem(productId = "") }
        val valid = cartItem()
        assertThrows(IllegalArgumentException::class.java) {
            valid.copy(productName = "")
        }
    }

    @Test fun `cart item requires normalized note deterministic selections and consistent prices`() {
        val item = cartItem(note = "sem cebola")
        assertEquals("sem cebola", item.note)
        assertThrows(IllegalArgumentException::class.java) { cartItem(note = " sem cebola ") }
        val sauce = CartItemSelection(
            "sauce", "Sauce", "hot", "Hot", MoneyAmount(0, "AOA")
        )
        assertThrows(IllegalArgumentException::class.java) {
            item.copy(selections = item.selections + sauce)
        }
        assertThrows(IllegalArgumentException::class.java) {
            item.copy(selections = item.selections + item.selections.first())
        }
        assertThrows(IllegalArgumentException::class.java) {
            item.copy(totalPrice = MoneyAmount(1, "AOA"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            item.copy(optionsPrice = MoneyAmount(200, "USD"))
        }
    }

    @Test fun `selection requires snapshot identities and names`() {
        val selection = cartItem().selections.single()
        assertThrows(IllegalArgumentException::class.java) { selection.copy(groupId = "") }
        assertThrows(IllegalArgumentException::class.java) { selection.copy(groupName = "") }
        assertThrows(IllegalArgumentException::class.java) { selection.copy(optionId = "") }
        assertThrows(IllegalArgumentException::class.java) { selection.copy(optionName = "") }
    }

    @Test fun `cart totals reject incoherent counts`() {
        assertThrows(IllegalArgumentException::class.java) {
            CartTotals(-1, 0, MoneyAmount(0, "AOA"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CartTotals(1, 0, MoneyAmount(0, "AOA"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CartTotals(1, 2, MoneyAmount(0, "AOA"))
        }
    }

    @Test fun `optional item snapshots and positive cart versions remain valid`() {
        val item = cartItem().copy(productImageUrl = null, note = null, selections = emptyList(), optionsPrice = MoneyAmount(0, "AOA"), totalUnitPrice = MoneyAmount(1_000, "AOA"), totalPrice = MoneyAmount(1_000, "AOA"))
        assertNull(item.productImageUrl)
        assertNull(item.note)
        assertTrue(item.selections.isEmpty())
        val cart = Cart("cart", merchant(), listOf(item), CartTotals(1, 1, MoneyAmount(1_000, "AOA")), 42)
        assertEquals(42L, cart.version)
        assertEquals("AOA", cart.totals.subtotal.currencyCode)
    }

    @Test fun `blank fingerprint and inconsistent subtotal currency are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            cartItem(fingerprint = "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            Cart(
                "cart", merchant(), listOf(cartItem()),
                CartTotals(1, 1, MoneyAmount(1_200, "USD")), 0
            )
        }
    }
}
