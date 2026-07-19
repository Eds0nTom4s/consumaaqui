package ao.consuma.aqui.feature.cart.presentation.mapper

import ao.consuma.aqui.feature.cart.addCommand
import ao.consuma.aqui.feature.cart.data.InMemoryCartRepository
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.cart.domain.service.CartItemFactory
import ao.consuma.aqui.feature.cart.domain.service.CartItemIdentityFactory
import ao.consuma.aqui.feature.cart.domain.service.CartMerchantPolicy
import ao.consuma.aqui.feature.cart.domain.service.CartTotalsCalculator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartUiMapperTest {
    private val mapper = CartUiMapper()

    @Test fun `badge boundaries and accessibility are derived from item count`() {
        assertFalse(mapper.badge(0).visible)
        assertNull(mapper.badge(0).displayText)
        assertEquals("1", mapper.badge(1).displayText)
        assertEquals("10", mapper.badge(10).displayText)
        assertEquals("99", mapper.badge(99).displayText)
        assertEquals("99+", mapper.badge(100).displayText)
        assertTrue(mapper.badge(100).accessibilityDescription is CartUiText.Resource)
    }

    @Test fun `cart mapping preserves order snapshots note quantities and repository totals`() = runTest {
        val repository = repository()
        repository.addItem(addCommand(productId = "first", productName = "First", note = "sem sal", quantity = 2))
        repository.addItem(addCommand(productId = "second", productName = "Second", optionId = "small", optionName = "Small"))
        val cart = repository.cart.value
        val items = mapper.items(cart)

        assertEquals(listOf("First", "Second"), items.map { it.productName })
        assertEquals("Size: Large", items.first().configurationText.single())
        assertEquals("sem sal", items.first().noteText)
        assertEquals(2, items.first().quantity)
        assertEquals("12 Kz", items.first().unitPriceText)
        assertEquals("24 Kz", items.first().totalPriceText)
        assertEquals(cart.totals.itemCount, 3)
        assertEquals("36 Kz", mapper.subtotal(cart))
        assertEquals("Merchant One", mapper.merchant(cart)?.name)
    }

    @Test fun `quantity boundaries map selector capabilities`() = runTest {
        val repository = repository()
        repository.addItem(addCommand(quantity = 99)) as AddCartItemResult.Added
        val item = mapper.items(repository.cart.value).single()
        assertTrue(item.canDecrease)
        assertFalse(item.canIncrease)
    }

    private fun repository(): InMemoryCartRepository {
        val identity = CartItemIdentityFactory()
        return InMemoryCartRepository(
            CartItemFactory(identity), identity, CartTotalsCalculator(), CartMerchantPolicy()
        )
    }
}
