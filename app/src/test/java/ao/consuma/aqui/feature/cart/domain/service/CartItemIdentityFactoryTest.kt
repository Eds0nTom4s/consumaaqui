package ao.consuma.aqui.feature.cart.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.domain.model.CartItemSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartItemIdentityFactoryTest {
    private val factory = CartItemIdentityFactory()
    private val size = CartItemSelection("size", "Size", "large", "Large", MoneyAmount(200, "AOA"))
    private val sauce = CartItemSelection("sauce", "Sauce", "hot", "Hot", MoneyAmount(0, "AOA"))

    @Test fun `same configuration and different quantity concepts produce same fingerprint`() {
        val first = fingerprint(listOf(size, sauce), " note ")
        val second = fingerprint(listOf(size, sauce), "note")
        assertEquals(first, second)
        assertEquals(64, first.length)
        assertTrue(first.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(fingerprint(listOf(size), null), fingerprint(listOf(size), "   "))
    }

    @Test fun `group and option order do not affect fingerprint`() {
        val extraSize = size.copy(optionId = "medium", optionName = "Medium")
        assertEquals(
            fingerprint(listOf(size, extraSize, sauce), null),
            fingerprint(listOf(sauce, extraSize, size), null)
        )
    }

    @Test fun `note product merchant and option identity affect fingerprint`() {
        val base = fingerprint(listOf(size), null)
        assertNotEquals(base, fingerprint(listOf(size), "sem cebola"))
        assertNotEquals(base, factory.configurationFingerprint("merchant-two", "product", listOf(size), null))
        assertNotEquals(base, factory.configurationFingerprint("merchant", "product-two", listOf(size), null))
        assertNotEquals(
            base,
            fingerprint(listOf(size.copy(optionId = "small")), null)
        )
    }

    @Test fun `unicode is deterministic between factory instances`() {
        val selection = size.copy(groupId = "porção", optionId = "grande-ação")
        val first = factory.configurationFingerprint("comerciante-á", "produto-ç", listOf(selection), "sem açúcar")
        val second = CartItemIdentityFactory().configurationFingerprint(
            "comerciante-á", "produto-ç", listOf(selection), "sem açúcar"
        )
        assertEquals(first, second)
        assertFalse(first == listOf(selection).hashCode().toString())
    }

    @Test fun `line and cart ids are UUID identities separate from fingerprint`() {
        val itemIds = List(20) { factory.createItemId() }
        val cartIds = List(20) { factory.createCartId() }
        assertEquals(itemIds.size, itemIds.distinct().size)
        assertEquals(cartIds.size, cartIds.distinct().size)
        assertTrue(itemIds.none { it == fingerprint(listOf(size), null) })
    }

    @Test fun `group identity internal spaces and reserved characters are deterministic inputs`() {
        val base = fingerprint(listOf(size), "sem  cebola")
        assertNotEquals(base, fingerprint(listOf(size), "sem cebola"))
        assertNotEquals(base, fingerprint(listOf(size.copy(groupId = "portion")), "sem  cebola"))
        val reserved = factory.configurationFingerprint(
            "merchant/?#",
            "product/ação?x=1",
            listOf(size.copy(groupId = "grupo/#?", optionId = "opção &=+")),
            "nota\ncom\ttabs"
        )
        assertEquals(64, reserved.length)
        assertTrue(reserved.matches(Regex("[0-9a-f]{64}")))
        assertEquals(
            reserved,
            CartItemIdentityFactory().configurationFingerprint(
                "merchant/?#",
                "product/ação?x=1",
                listOf(size.copy(groupId = "grupo/#?", optionId = "opção &=+")),
                "nota\ncom\ttabs"
            )
        )
    }

    private fun fingerprint(selections: List<CartItemSelection>, note: String?) =
        factory.configurationFingerprint("merchant", "product", selections, note)
}
