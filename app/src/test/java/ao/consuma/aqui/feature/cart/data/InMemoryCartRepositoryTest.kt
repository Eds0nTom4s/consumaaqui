package ao.consuma.aqui.feature.cart.data

import ao.consuma.aqui.feature.cart.addCommand
import ao.consuma.aqui.feature.cart.domain.command.ClearCartCommand
import ao.consuma.aqui.feature.cart.domain.command.RemoveCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.ReplaceCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.UpdateCartItemQuantityCommand
import ao.consuma.aqui.feature.cart.domain.model.CartConflict
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.cart.domain.result.CartError
import ao.consuma.aqui.feature.cart.domain.result.CartResult
import ao.consuma.aqui.feature.cart.domain.service.CartItemFactory
import ao.consuma.aqui.feature.cart.domain.service.CartItemIdentityFactory
import ao.consuma.aqui.feature.cart.domain.service.CartMerchantPolicy
import ao.consuma.aqui.feature.cart.domain.service.CartTotalsCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryCartRepositoryTest {
    private lateinit var repository: InMemoryCartRepository

    @Before fun setUp() {
        val identity = CartItemIdentityFactory()
        repository = InMemoryCartRepository(
            CartItemFactory(identity),
            identity,
            CartTotalsCalculator(),
            CartMerchantPolicy()
        )
    }

    @Test fun `first item creates merchant and same configuration aggregates`() = runTest {
        val first = repository.addItem(addCommand()) as AddCartItemResult.Added
        assertFalse(first.merged)
        assertEquals("merchant-one", first.cart.merchant?.id)
        assertEquals(1, first.cart.items.size)
        assertEquals(1L, first.cart.version)

        val second = repository.addItem(addCommand(quantity = 2)) as AddCartItemResult.Added
        assertTrue(second.merged)
        assertEquals(first.item.id, second.item.id)
        assertEquals(3, second.item.quantity)
        assertEquals(3, second.cart.totals.itemCount)
        assertEquals(1, second.cart.totals.distinctItemCount)
        assertEquals(3_600L, second.cart.totals.subtotal.amountMinor)
        assertEquals(2L, second.cart.version)
    }

    @Test fun `different configuration creates another line`() = runTest {
        repository.addItem(addCommand())
        val added = repository.addItem(
            addCommand(optionId = "small", optionName = "Small", optionMinor = 0)
        ) as AddCartItemResult.Added
        assertFalse(added.merged)
        assertEquals(2, added.cart.items.size)
        assertEquals(2, added.cart.totals.distinctItemCount)
    }

    @Test fun `normalized note aggregates while a different note creates another line`() = runTest {
        repository.addItem(addCommand(note = "  sem cebola  "))
        val merged = repository.addItem(addCommand(note = "sem cebola")) as AddCartItemResult.Added
        val distinct = repository.addItem(addCommand(note = null)) as AddCartItemResult.Added

        assertTrue(merged.merged)
        assertFalse(distinct.merged)
        assertEquals(2, distinct.cart.items.size)
        assertEquals(3, distinct.cart.totals.itemCount)
    }

    @Test fun `different merchant is explicit conflict and state does not change`() = runTest {
        val initial = (repository.addItem(addCommand()) as AddCartItemResult.Added).cart
        val result = repository.addItem(
            addCommand(merchantId = "merchant-two", merchantName = "Merchant Two")
        ) as AddCartItemResult.Conflict
        val conflict = result.conflict as CartConflict.DifferentMerchant
        assertEquals("merchant-one", conflict.currentMerchant.id)
        assertEquals("merchant-two", conflict.requestedMerchant.id)
        assertEquals(initial, repository.cart.value)
    }

    @Test fun `currency mismatch and aggregation limit do not mutate state`() = runTest {
        repository.addItem(addCommand(quantity = 99))
        val beforeLimit = repository.cart.value
        val limit = repository.addItem(addCommand()) as AddCartItemResult.Failure
        assertEquals(CartError.QuantityLimitExceeded, limit.error)
        assertEquals(beforeLimit, repository.cart.value)

        repository.clearCart()
        repository.addItem(addCommand())
        val beforeCurrency = repository.cart.value
        val mismatch = repository.addItem(
            addCommand(productId = "usd-product", currency = "USD")
        ) as AddCartItemResult.Failure
        assertEquals(CartError.CurrencyMismatch, mismatch.error)
        assertEquals(beforeCurrency, repository.cart.value)
    }

    @Test fun `invalid item and price overflow do not mutate state`() = runTest {
        val beforeInvalid = repository.cart.value
        val invalid = repository.addItem(
            addCommand().let { it.copy(snapshot = it.snapshot.copy(productName = " ")) }
        ) as AddCartItemResult.Failure
        assertEquals(CartError.InvalidItem, invalid.error)
        assertEquals(beforeInvalid, repository.cart.value)

        repository.addItem(addCommand(unitMinor = Long.MAX_VALUE, optionMinor = 0))
        val beforeOverflow = repository.cart.value
        val overflow = repository.addItem(
            addCommand(unitMinor = Long.MAX_VALUE, optionMinor = 0)
        ) as AddCartItemResult.Failure
        assertEquals(CartError.PriceOverflow, overflow.error)
        assertEquals(beforeOverflow, repository.cart.value)
    }

    @Test fun `update validates identity quantity totals and effective version`() = runTest {
        val added = repository.addItem(addCommand()) as AddCartItemResult.Added
        val itemId = added.item.id
        val same = repository.updateQuantity(UpdateCartItemQuantityCommand(itemId, 1))
            as CartResult.Success
        assertEquals(1L, same.data.version)

        val updated = repository.updateQuantity(UpdateCartItemQuantityCommand(itemId, 99))
            as CartResult.Success
        assertEquals(99, updated.data.totals.itemCount)
        assertEquals(118_800L, updated.data.totals.subtotal.amountMinor)
        assertEquals(2L, updated.data.version)

        assertEquals(
            CartError.InvalidQuantity,
            (repository.updateQuantity(UpdateCartItemQuantityCommand(itemId, 0)) as
                CartResult.Failure).error
        )
        assertEquals(
            CartError.InvalidQuantity,
            (repository.updateQuantity(UpdateCartItemQuantityCommand(itemId, 100)) as
                CartResult.Failure).error
        )
        assertEquals(
            CartError.ItemNotFound,
            (repository.updateQuantity(UpdateCartItemQuantityCommand("missing", 2)) as
                CartResult.Failure).error
        )
        assertEquals(2L, repository.cart.value.version)
    }

    @Test fun `remove updates totals and last item leaves valid empty cart with same id`() = runTest {
        val added = repository.addItem(addCommand()) as AddCartItemResult.Added
        val cartId = added.cart.id
        val removed = repository.removeItem(RemoveCartItemCommand(added.item.id)) as CartResult.Success
        assertTrue(removed.data.items.isEmpty())
        assertNull(removed.data.merchant)
        assertEquals(0, removed.data.totals.itemCount)
        assertEquals(cartId, removed.data.id)
        assertEquals(2L, removed.data.version)
        assertEquals(
            CartError.ItemNotFound,
            (repository.removeItem(RemoveCartItemCommand(added.item.id)) as CartResult.Failure).error
        )
        assertEquals(2L, repository.cart.value.version)
    }

    @Test fun `remove one of multiple lines recalculates subtotal and keeps merchant`() = runTest {
        val first = repository.addItem(addCommand()) as AddCartItemResult.Added
        repository.addItem(addCommand(productId = "product-two", optionId = "small", optionMinor = 0))

        val removed = repository.removeItem(RemoveCartItemCommand(first.item.id)) as CartResult.Success

        assertEquals(1, removed.data.items.size)
        assertEquals("merchant-one", removed.data.merchant?.id)
        assertEquals(1_000L, removed.data.totals.subtotal.amountMinor)
        assertEquals(3L, removed.data.version)
    }

    @Test fun `clear is explicit logical reset with new id and monotonic version`() = runTest {
        val before = (repository.addItem(addCommand()) as AddCartItemResult.Added).cart
        val cleared = repository.clearCart(ClearCartCommand) as CartResult.Success
        assertTrue(cleared.data.items.isEmpty())
        assertNull(cleared.data.merchant)
        assertNotEquals(before.id, cleared.data.id)
        assertEquals(before.version + 1, cleared.data.version)

        val clearedAgain = repository.clearCart() as CartResult.Success
        assertNotEquals(cleared.data.id, clearedAgain.data.id)
        assertEquals(cleared.data.version + 1, clearedAgain.data.version)
    }

    @Test fun `replace cart with item is atomic and accepts another merchant`() = runTest {
        val initial = (repository.addItem(addCommand()) as AddCartItemResult.Added).cart
        val replaced = repository.replaceCartWithItem(
            addCommand(merchantId = "merchant-two", merchantName = "Merchant Two")
        ) as CartResult.Success
        assertNotEquals(initial.id, replaced.data.id)
        assertEquals("merchant-two", replaced.data.merchant?.id)
        assertEquals(1, replaced.data.items.size)
        assertEquals(initial.version + 1, replaced.data.version)

        val beforeFailure = repository.cart.value
        val invalid = addCommand().let {
            it.copy(snapshot = it.snapshot.copy(productName = ""))
        }
        assertEquals(
            CartError.InvalidItem,
            (repository.replaceCartWithItem(invalid) as CartResult.Failure).error
        )
        assertEquals(beforeFailure, repository.cart.value)
    }

    @Test fun `replace item preserves line id and no-op preserves version`() = runTest {
        val original = repository.addItem(addCommand()) as AddCartItemResult.Added
        val replacementCommand = addCommand(
            optionId = "small", optionName = "Small", optionMinor = 0
        )
        val replaced = repository.replaceItem(
            ReplaceCartItemCommand(
                original.item.id,
                replacementCommand.merchant,
                replacementCommand.configuredProduct,
                replacementCommand.snapshot
            )
        ) as CartResult.Success
        assertEquals(original.item.id, replaced.data.items.single().id)
        assertEquals("small", replaced.data.items.single().selections.single().optionId)
        assertEquals(2L, replaced.data.version)

        val unchanged = repository.replaceItem(
            ReplaceCartItemCommand(
                original.item.id,
                replacementCommand.merchant,
                replacementCommand.configuredProduct,
                replacementCommand.snapshot
            )
        ) as CartResult.Success
        assertEquals(replaced.data, unchanged.data)
        assertEquals(2L, repository.cart.value.version)
    }

    @Test fun `replace item merges equivalent other line and validates failures`() = runTest {
        val first = repository.addItem(addCommand()) as AddCartItemResult.Added
        val secondCommand = addCommand(note = "sem cebola")
        val second = repository.addItem(secondCommand) as AddCartItemResult.Added
        val merged = repository.replaceItem(
            ReplaceCartItemCommand(
                second.item.id,
                first.cart.merchant!!,
                addCommand().configuredProduct,
                addCommand().snapshot
            )
        ) as CartResult.Success
        assertEquals(1, merged.data.items.size)
        assertEquals(2, merged.data.items.single().quantity)
        assertEquals(first.item.id, merged.data.items.single().id)

        assertEquals(
            CartError.ItemNotFound,
            (repository.replaceItem(
                ReplaceCartItemCommand(
                    "missing", secondCommand.merchant,
                    secondCommand.configuredProduct, secondCommand.snapshot
                )
            ) as CartResult.Failure).error
        )
    }

    @Test fun `replace item rejects merchant currency and merge limit without state change`() = runTest {
        val first = repository.addItem(addCommand(quantity = 99)) as AddCartItemResult.Added
        val secondCommand = addCommand(note = "outra linha")
        val second = repository.addItem(secondCommand) as AddCartItemResult.Added
        val before = repository.cart.value

        val merchantCommand = addCommand(merchantId = "merchant-two", merchantName = "Merchant Two")
        assertEquals(
            CartError.MerchantMismatch,
            (repository.replaceItem(
                ReplaceCartItemCommand(
                    first.item.id,
                    merchantCommand.merchant,
                    merchantCommand.configuredProduct,
                    merchantCommand.snapshot
                )
            ) as CartResult.Failure).error
        )

        val usdCommand = addCommand(currency = "USD")
        assertEquals(
            CartError.CurrencyMismatch,
            (repository.replaceItem(
                ReplaceCartItemCommand(
                    second.item.id,
                    usdCommand.merchant,
                    usdCommand.configuredProduct,
                    usdCommand.snapshot
                )
            ) as CartResult.Failure).error
        )

        val mergeCommand = addCommand()
        assertEquals(
            CartError.QuantityLimitExceeded,
            (repository.replaceItem(
                ReplaceCartItemCommand(
                    second.item.id,
                    mergeCommand.merchant,
                    mergeCommand.configuredProduct,
                    mergeCommand.snapshot
                )
            ) as CartResult.Failure).error
        )
        assertEquals(before, repository.cart.value)
    }

    @Test fun `published cart and item selection lists cannot be mutated`() = runTest {
        repository.addItem(addCommand())
        val cart = repository.cart.value

        assertThrows(UnsupportedOperationException::class.java) {
            (cart.items as MutableList).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (cart.items.single().selections as MutableList).clear()
        }
        assertEquals(1, repository.cart.value.items.size)
    }

    @Test fun `ten concurrent equal adds are serialized and aggregated`() = runTest {
        val results = List(10) { async { repository.addItem(addCommand()) } }.awaitAll()
        assertTrue(results.all { it is AddCartItemResult.Added })
        val cart = repository.cart.value
        assertEquals(1, cart.items.size)
        assertEquals(10, cart.items.single().quantity)
        assertEquals(10, cart.totals.itemCount)
        assertEquals(10L, cart.version)
    }

    @Test fun `concurrent different adds preserve every line and coherent totals`() = runTest {
        List(10) { index ->
            async {
                repository.addItem(
                    addCommand(productId = "product-$index", optionId = "option-$index")
                )
            }
        }.awaitAll()
        val cart = repository.cart.value
        assertEquals(10, cart.items.size)
        assertEquals(10, cart.items.map { it.configurationFingerprint }.distinct().size)
        assertEquals(10, cart.totals.itemCount)
        assertEquals(10L, cart.version)
    }

    @Test fun `concurrent add clear and update remove never lose invariants`() = runTest {
        val initial = repository.addItem(addCommand()) as AddCartItemResult.Added
        awaitAll(
            async { repository.addItem(addCommand()) },
            async { repository.clearCart() }
        )
        assertCartCoherent()

        repository.clearCart()
        val added = repository.addItem(addCommand()) as AddCartItemResult.Added
        awaitAll(
            async { repository.addItem(addCommand()) },
            async { repository.removeItem(RemoveCartItemCommand(added.item.id)) }
        )
        assertCartCoherent()

        repository.clearCart()
        val updateItem = (repository.addItem(addCommand()) as AddCartItemResult.Added).item
        (2..10).map { quantity ->
            async {
                repository.updateQuantity(
                    UpdateCartItemQuantityCommand(updateItem.id, quantity)
                )
            }
        }.awaitAll()
        assertCartCoherent()
        assertTrue(repository.cart.value.items.single().quantity in 2..10)
        assertEquals(18L, repository.cart.value.version)
        assertTrue(initial.item.id.isNotBlank())
    }

    @Test fun `one hundred concurrent adds stop at line limit without duplicates`() = runTest {
        val results = List(100) { async { repository.addItem(addCommand()) } }.awaitAll()
        assertEquals(99, results.count { it is AddCartItemResult.Added })
        assertEquals(1, results.count {
            it is AddCartItemResult.Failure && it.error == CartError.QuantityLimitExceeded
        })
        val cart = repository.cart.value
        assertEquals(99, cart.items.single().quantity)
        assertEquals(99, cart.totals.itemCount)
        assertEquals(99L, cart.version)
    }

    @Test fun `concurrent replace cart and old merchant add have only valid atomic emissions`() = runTest {
        repository.addItem(addCommand())
        val emissions = mutableListOf<ao.consuma.aqui.feature.cart.domain.model.Cart>()
        val collector = backgroundScope.launch { repository.cart.take(2).toList(emissions) }
        awaitAll(
            async {
                repository.replaceCartWithItem(
                    addCommand(merchantId = "merchant-two", merchantName = "Merchant Two")
                )
            },
            async { repository.addItem(addCommand(productId = "old-merchant-extra")) }
        )
        collector.join()
        assertCartCoherent()
        assertEquals("merchant-two", repository.cart.value.merchant?.id)
        assertTrue(emissions.all { cart ->
            cart.items.all { it.merchantId == cart.merchant?.id } &&
                cart.totals.itemCount == cart.items.sumOf { it.quantity }
        })
        assertTrue(emissions.zipWithNext().all { (before, after) -> after.version > before.version })
    }

    @Test fun `concurrent remove and update linearize without resurrection`() = runTest {
        val item = (repository.addItem(addCommand()) as AddCartItemResult.Added).item
        awaitAll(
            async { repository.removeItem(RemoveCartItemCommand(item.id)) },
            async { repository.updateQuantity(UpdateCartItemQuantityCommand(item.id, 8)) }
        )
        assertTrue(repository.cart.value.items.isEmpty())
        assertCartCoherent()
    }

    @Test fun `concurrent replace item and add preserve a coherent serial outcome`() = runTest {
        val original = repository.addItem(addCommand()) as AddCartItemResult.Added
        val replacement = addCommand(optionId = "small", optionName = "Small", optionMinor = 0)
        awaitAll(
            async {
                repository.replaceItem(
                    ReplaceCartItemCommand(
                        original.item.id,
                        replacement.merchant,
                        replacement.configuredProduct,
                        replacement.snapshot
                    )
                )
            },
            async { repository.addItem(addCommand()) }
        )
        assertCartCoherent()
        assertTrue(repository.cart.value.version >= 2)
        assertEquals(
            repository.cart.value.items.size,
            repository.cart.value.items.map { it.configurationFingerprint }.distinct().size
        )
    }

    private fun assertCartCoherent() {
        val cart = repository.cart.value
        assertEquals(cart.items.sumOf { it.quantity }, cart.totals.itemCount)
        assertEquals(cart.items.size, cart.totals.distinctItemCount)
        assertEquals(cart.items.size, cart.items.map { it.id }.distinct().size)
        assertEquals(
            cart.items.sumOf { it.totalPrice.amountMinor },
            cart.totals.subtotal.amountMinor
        )
        assertTrue(cart.items.all { it.quantity in 1..99 })
        if (cart.items.isEmpty()) assertNull(cart.merchant)
    }
}
