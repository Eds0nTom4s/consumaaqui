package ao.consuma.aqui.feature.cart.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.addCommand
import ao.consuma.aqui.feature.cart.domain.command.CartOptionSnapshot
import ao.consuma.aqui.feature.cart.domain.model.CartMerchant
import ao.consuma.aqui.feature.cart.domain.result.CartError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CartItemFactoryTest {
    private val factory = CartItemFactory(CartItemIdentityFactory())

    @Test fun `valid configured product creates presentation snapshot without mutation`() {
        val command = addCommand(note = "  sem cebola  ", quantity = 2)
        val before = command.configuredProduct
        val item = (factory.create(command) as CartItemFactoryResult.Success).item
        assertEquals("Product One", item.productName)
        assertEquals(2, item.quantity)
        assertEquals("sem cebola", item.note)
        assertEquals("Large", item.selections.single().optionName)
        assertEquals(2_400L, item.totalPrice.amountMinor)
        assertEquals(before, command.configuredProduct)
    }

    @Test fun `ids are unique while equal configurations have equal fingerprint`() {
        val first = (factory.create(addCommand()) as CartItemFactoryResult.Success).item
        val second = (factory.create(addCommand(quantity = 2)) as CartItemFactoryResult.Success).item
        assertNotEquals(first.id, second.id)
        assertEquals(first.configurationFingerprint, second.configurationFingerprint)
    }

    @Test fun `factory snapshots do not retain mutable command selections`() {
        val base = addCommand()
        val mutableSelections = base.snapshot.selectedOptions.toMutableList()
        val command = base.copy(snapshot = base.snapshot.copy(selectedOptions = mutableSelections))
        val item = (factory.create(command) as CartItemFactoryResult.Success).item

        mutableSelections.clear()

        assertEquals(1, item.selections.size)
        assertThrows(UnsupportedOperationException::class.java) {
            (item.selections as MutableList).clear()
        }
    }

    @Test fun `selection snapshots are sorted and names and optional image are preserved`() {
        val base = addCommand()
        val sauce = CartOptionSnapshot(
            "sauce", "Sauce", "hot", "Hot", MoneyAmount(0, "AOA")
        )
        val command = base.copy(
            configuredProduct = base.configuredProduct.copy(
                selectedOptionIds = mapOf("size" to setOf("large"), "sauce" to setOf("hot"))
            ),
            snapshot = base.snapshot.copy(
                imageUrl = null,
                selectedOptions = listOf(base.snapshot.selectedOptions.single(), sauce).reversed()
            )
        )
        val item = (factory.create(command) as CartItemFactoryResult.Success).item
        assertEquals(listOf("sauce", "size"), item.selections.map { it.groupId })
        assertNull(item.productImageUrl)
        assertEquals(listOf("Hot", "Large"), item.selections.map { it.optionName })
    }

    @Test fun `blank note becomes absent`() {
        val item = (factory.create(addCommand(note = "   ")) as CartItemFactoryResult.Success).item
        assertNull(item.note)
    }

    @Test fun `merchant mismatch missing snapshot and blank name are rejected`() {
        val base = addCommand()
        assertEquals(
            CartError.MerchantMismatch,
            (factory.create(base.copy(merchant = CartMerchant("other", "Other"))) as
                CartItemFactoryResult.Failure).error
        )
        assertEquals(
            CartError.InvalidItem,
            (factory.create(base.copy(snapshot = base.snapshot.copy(selectedOptions = emptyList()))) as
                CartItemFactoryResult.Failure).error
        )
        assertEquals(
            CartError.InvalidItem,
            (factory.create(base.copy(snapshot = base.snapshot.copy(productName = " "))) as
                CartItemFactoryResult.Failure).error
        )
    }

    @Test fun `currency and price inconsistencies are explicit`() {
        val base = addCommand()
        val usdSnapshot = base.snapshot.copy(
            selectedOptions = listOf(
                base.snapshot.selectedOptions.single().copy(
                    additionalPrice = MoneyAmount(200, "USD")
                )
            )
        )
        assertEquals(
            CartError.CurrencyMismatch,
            (factory.create(base.copy(snapshot = usdSnapshot)) as CartItemFactoryResult.Failure).error
        )

        val invalidPrice = base.copy(
            configuredProduct = base.configuredProduct.copy(
                optionsPrice = MoneyAmount(300, "AOA"),
                totalUnitPrice = MoneyAmount(1_300, "AOA"),
                totalPrice = MoneyAmount(1_300, "AOA")
            )
        )
        val result = factory.create(invalidPrice)
        assertTrue(result is CartItemFactoryResult.Failure)
        assertEquals(CartError.InvalidItem, (result as CartItemFactoryResult.Failure).error)
    }
}
