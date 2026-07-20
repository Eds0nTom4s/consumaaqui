package ao.consuma.aqui.feature.checkout.data

import ao.consuma.aqui.feature.cart.addCommand
import ao.consuma.aqui.feature.cart.domain.command.UpdateCartItemQuantityCommand
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.checkout.domain.command.ConfirmCheckoutCommand
import ao.consuma.aqui.feature.checkout.domain.command.MockCheckoutScenario
import ao.consuma.aqui.feature.checkout.domain.command.RequestQuoteCommand
import ao.consuma.aqui.feature.checkout.domain.command.SelectFulfillmentCommand
import ao.consuma.aqui.feature.checkout.domain.command.StartCheckoutCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdateCustomerCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdateDeliveryAddressCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdatePickupDetailsCommand
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.model.DeliveryAddress
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutConflict
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutResult
import ao.consuma.aqui.feature.checkout.repositoryFixture
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryCheckoutRepositoryTest {
    private lateinit var repository: InMemoryCheckoutRepository
    private lateinit var cart: ao.consuma.aqui.feature.cart.data.InMemoryCartRepository

    @Before fun setUp() {
        repositoryFixture().also { (checkout, cartRepository) ->
            repository = checkout
            cart = cartRepository
        }
    }

    private suspend fun populateCart() = cart.addItem(
        addCommand(merchantId = "sabor-maianga", merchantName = "Sabor da Maianga")
    ) as AddCartItemResult.Added

    private suspend fun start(
        scenario: MockCheckoutScenario = MockCheckoutScenario.SUCCESS
    ): CheckoutSession {
        populateCart()
        return (repository.startCheckout(StartCheckoutCommand(scenario = scenario))
            as CheckoutResult.Success).data
    }

    private suspend fun completePickup(
        scenario: MockCheckoutScenario = MockCheckoutScenario.SUCCESS
    ): CheckoutSession {
        val started = start(scenario)
        repository.selectFulfillment(SelectFulfillmentCommand(started.id, FulfillmentMethod.PICKUP))
        repository.updateCustomer(
            UpdateCustomerCommand(started.id, "Ana Silva", "+244 923 456 789", "ANA@example.com")
        )
        repository.updatePickupDetails(
            UpdatePickupDetailsCommand(started.id, "Ana Silva", "+244923456789")
        )
        return (repository.requestQuote(
            RequestQuoteCommand(started.id, started.cartId, started.cartVersion)
        ) as CheckoutResult.Success).data
    }

    @Test fun `empty cart fails without creating session`() = runTest {
        val result = repository.startCheckout() as CheckoutResult.Failure
        assertEquals(CheckoutError.EmptyCart, result.error)
        assertNull(repository.session.value)
    }

    @Test fun `start creates independent snapshot capabilities and explicit status`() = runTest {
        val added = populateCart()
        val started = (repository.startCheckout() as CheckoutResult.Success).data
        assertEquals(added.cart.id, started.cartId)
        assertEquals(added.cart.version, started.cartVersion)
        assertEquals(added.cart.totals.subtotal, started.cartSubtotal)
        assertEquals(added.cart.items.single().id, started.items.single().cartItemId)
        assertEquals("sabor-maianga", started.merchantId)
        assertTrue(started.capabilities.pickupAvailable)
        assertTrue(started.capabilities.deliveryAvailable)
        assertEquals(CheckoutSessionStatus.Started, started.status)
        assertEquals(0L, started.version)
    }

    @Test fun `start expected cart mismatch is recoverable conflict`() = runTest {
        populateCart()
        val result = repository.startCheckout(StartCheckoutCommand("stale", 0))
            as CheckoutResult.Conflict
        assertTrue(result.conflict is CheckoutConflict.CartChanged)
        assertNull(repository.session.value)
    }

    @Test fun `customer update validates normalizes no-op and confirmed state`() = runTest {
        val started = start()
        val invalid = repository.updateCustomer(
            UpdateCustomerCommand(started.id, "A", "bad", null)
        ) as CheckoutResult.Failure
        assertEquals(CheckoutError.InvalidCustomer, invalid.error)
        assertEquals(0L, repository.session.value?.version)

        val updated = (repository.updateCustomer(
            UpdateCustomerCommand(started.id, " Ana Silva ", "+244 923 456 789", "ANA@EXAMPLE.COM")
        ) as CheckoutResult.Success).data
        assertEquals("Ana Silva", updated.customer?.fullName)
        assertEquals("+244923456789", updated.customer?.contact?.phoneNumber)
        assertEquals("ana@example.com", updated.customer?.contact?.email)
        assertEquals(1L, updated.version)
        val same = (repository.updateCustomer(
            UpdateCustomerCommand(started.id, "Ana Silva", "+244923456789", "ana@example.com")
        ) as CheckoutResult.Success).data
        assertEquals(updated, same)
        assertEquals(
            CheckoutError.SessionNotFound,
            (repository.updateCustomer(
                UpdateCustomerCommand("missing", "Ana Silva", "923456789", null)
            ) as CheckoutResult.Failure).error
        )
    }

    @Test fun `fulfillment selection enforces capability and changing method clears incompatible details and quote`() =
        runTest {
            val ready = completePickup()
            assertTrue(ready.fulfillment is CheckoutFulfillment.Pickup)
            assertTrue(ready.quote != null)
            val changed = (repository.selectFulfillment(
                SelectFulfillmentCommand(ready.id, FulfillmentMethod.DELIVERY_MOCK)
            ) as CheckoutResult.Success).data
            assertNull(changed.fulfillment)
            assertNull(changed.quote)
            assertEquals(CheckoutSessionStatus.FulfillmentPending, changed.status)
            assertEquals(ready.version + 1, changed.version)

            repository.resetCheckout()
            val unavailable = start(MockCheckoutScenario.FULFILLMENT_UNAVAILABLE)
            val failure = repository.selectFulfillment(
                SelectFulfillmentCommand(unavailable.id, FulfillmentMethod.PICKUP)
            ) as CheckoutResult.Failure
            assertEquals(CheckoutError.FulfillmentUnavailable, failure.error)
            assertEquals(0L, repository.session.value?.version)
        }

    @Test fun `pickup and delivery details enforce selected type and invalidate previous quote`() = runTest {
        val started = start()
        assertEquals(
            CheckoutError.InvalidState,
            (repository.updatePickupDetails(
                UpdatePickupDetailsCommand(started.id, "Ana Silva", "923456789")
            ) as CheckoutResult.Failure).error
        )
        repository.selectFulfillment(
            SelectFulfillmentCommand(started.id, FulfillmentMethod.DELIVERY_MOCK)
        )
        repository.updateCustomer(UpdateCustomerCommand(started.id, "Ana Silva", "923456789", null))
        val invalid = repository.updateDeliveryAddress(
            UpdateDeliveryAddressCommand(
                started.id,
                DeliveryAddress("", "Luanda", null, "Rua", null, null, null, null),
                "Ana Silva", "923456789", null
            )
        ) as CheckoutResult.Failure
        assertEquals(CheckoutError.InvalidAddress, invalid.error)
        val valid = (repository.updateDeliveryAddress(
            UpdateDeliveryAddressCommand(
                started.id,
                DeliveryAddress(
                    "Luanda", "Luanda", "Maianga", "Rua 10", null,
                    "Próximo ao Banco BIC", null, null
                ),
                "Ana Silva", "923456789", "Portão azul"
            )
        ) as CheckoutResult.Success).data
        assertTrue(valid.fulfillment is CheckoutFulfillment.Delivery)
        assertEquals(CheckoutSessionStatus.QuotePending, valid.status)
    }

    @Test fun `pickup quote succeeds with exact totals and effective version`() = runTest {
        val ready = completePickup()
        assertEquals(CheckoutSessionStatus.ReadyForReview, ready.status)
        assertEquals(1_200L, ready.quote?.subtotal?.amountMinor)
        assertEquals(1_200L, ready.quote?.total?.amountMinor)
        assertTrue(ready.quote?.charges.orEmpty().isEmpty())
        assertEquals(4L, ready.version)
    }

    @Test fun `quote error and expired fixtures remain explicit`() = runTest {
        val quoteError = start(MockCheckoutScenario.QUOTE_ERROR)
        repository.selectFulfillment(SelectFulfillmentCommand(quoteError.id, FulfillmentMethod.PICKUP))
        repository.updateCustomer(UpdateCustomerCommand(quoteError.id, "Ana Silva", "923456789", null))
        repository.updatePickupDetails(UpdatePickupDetailsCommand(quoteError.id, "Ana Silva", "923456789"))
        assertEquals(
            CheckoutError.QuoteUnavailable,
            (repository.requestQuote(
                RequestQuoteCommand(quoteError.id, quoteError.cartId, quoteError.cartVersion)
            ) as CheckoutResult.Failure).error
        )
        repository.resetCheckout()
        val expired = completePickup(MockCheckoutScenario.QUOTE_EXPIRED)
        assertTrue(expired.quote?.expiresAt?.isBefore(ao.consuma.aqui.feature.checkout.fixedInstant) == true)
        assertEquals(
            CheckoutError.QuoteExpired,
            (repository.confirmCheckout(
                ConfirmCheckoutCommand(expired.id, expired.cartId, expired.cartVersion)
            ) as CheckoutResult.Failure).error
        )
    }

    @Test fun `cart change before quote or confirmation yields recoverable conflict`() = runTest {
        val started = start()
        repository.selectFulfillment(SelectFulfillmentCommand(started.id, FulfillmentMethod.PICKUP))
        repository.updateCustomer(UpdateCustomerCommand(started.id, "Ana Silva", "923456789", null))
        repository.updatePickupDetails(UpdatePickupDetailsCommand(started.id, "Ana Silva", "923456789"))
        val itemId = cart.cart.value.items.single().id
        cart.updateQuantity(UpdateCartItemQuantityCommand(itemId, 2))
        val quote = repository.requestQuote(
            RequestQuoteCommand(started.id, started.cartId, started.cartVersion)
        ) as CheckoutResult.Conflict
        assertTrue(quote.conflict is CheckoutConflict.CartChanged)
        assertNull(repository.session.value?.quote)

        repository.resetCheckout()
        val ready = completePickup()
        cart.updateQuantity(UpdateCartItemQuantityCommand(cart.cart.value.items.single().id, 2))
        val confirm = repository.confirmCheckout(
            ConfirmCheckoutCommand(ready.id, ready.cartId, ready.cartVersion)
        ) as CheckoutResult.Conflict
        assertTrue(confirm.conflict is CheckoutConflict.CartChanged)
        assertEquals(CheckoutSessionStatus.ReadyForReview, repository.session.value?.status)
    }

    @Test fun `cleared merchant and currency changes retain specific recoverable conflicts`() = runTest {
        suspend fun prepared() = start().also { started ->
            repository.selectFulfillment(
                SelectFulfillmentCommand(started.id, FulfillmentMethod.PICKUP)
            )
            repository.updateCustomer(
                UpdateCustomerCommand(started.id, "Ana Silva", "923456789", null)
            )
            repository.updatePickupDetails(
                UpdatePickupDetailsCommand(started.id, "Ana Silva", "923456789")
            )
        }

        val cleared = prepared()
        cart.clearCart()
        assertTrue((repository.requestQuote(
            RequestQuoteCommand(cleared.id, cleared.cartId, cleared.cartVersion)
        ) as CheckoutResult.Conflict).conflict is CheckoutConflict.CartCleared)

        repository.resetCheckout()
        val merchantChanged = prepared()
        cart.replaceCartWithItem(
            addCommand(merchantId = "other", merchantName = "Outro Comerciante")
        )
        assertTrue((repository.requestQuote(
            RequestQuoteCommand(merchantChanged.id, merchantChanged.cartId, merchantChanged.cartVersion)
        ) as CheckoutResult.Conflict).conflict is CheckoutConflict.MerchantChanged)

        repository.resetCheckout()
        cart.clearCart()
        val currencyChanged = prepared()
        cart.replaceCartWithItem(
            addCommand(
                merchantId = "sabor-maianga",
                merchantName = "Sabor da Maianga",
                currency = "USD"
            )
        )
        assertTrue((repository.requestQuote(
            RequestQuoteCommand(currencyChanged.id, currencyChanged.cartId, currencyChanged.cartVersion)
        ) as CheckoutResult.Conflict).conflict is CheckoutConflict.CurrencyChanged)
    }

    @Test fun `confirmation creates no order clears no cart and duplicate returns identical draft`() = runTest {
        val ready = completePickup()
        val cartBefore = cart.cart.value
        val command = ConfirmCheckoutCommand(ready.id, ready.cartId, ready.cartVersion)
        val first = (repository.confirmCheckout(command) as CheckoutResult.Success).data
        val second = (repository.confirmCheckout(command) as CheckoutResult.Success).data
        assertSame(first, second)
        assertSame(first, repository.draft.value)
        assertEquals(first.id, second.id)
        assertEquals(first.clientReference, second.clientReference)
        assertEquals(cartBefore, cart.cart.value)
        assertEquals(CheckoutSessionStatus.ConfirmedMock, repository.session.value?.status)
        assertEquals(ready.version + 1, repository.session.value?.version)
        assertEquals(
            CheckoutError.SessionAlreadyConfirmed,
            (repository.updateCustomer(
                UpdateCustomerCommand(ready.id, "Outra Pessoa", "923456789", null)
            ) as CheckoutResult.Failure).error
        )
    }

    @Test fun `reset atomically discards session and draft without changing cart`() = runTest {
        completePickup()
        val cartBefore = cart.cart.value
        assertTrue(repository.resetCheckout() is CheckoutResult.Success)
        assertNull(repository.session.value)
        assertNull(repository.draft.value)
        assertEquals(cartBefore, cart.cart.value)
        assertTrue(repository.resetCheckout() is CheckoutResult.Success)
    }

    @Test fun `concurrent customer updates are serialized with monotonic effective versions`() = runTest {
        val started = start()
        val results = listOf("Ana Silva", "Maria Costa").map { name ->
            async {
                repository.updateCustomer(
                    UpdateCustomerCommand(started.id, name, "923456789", null)
                )
            }
        }.awaitAll()
        assertTrue(results.all { it is CheckoutResult.Success })
        assertEquals(
            listOf(1L, 2L),
            results.map { (it as CheckoutResult.Success).data.version }.sorted()
        )
        assertEquals(2L, repository.session.value?.version)
        // Construction invariants guarantee that every StateFlow value is a valid session.
        assertTrue(repository.session.value?.customer != null)
    }

    @Test fun `simultaneous confirmations create one draft and confirmation reset cannot deadlock`() = runTest {
        val ready = completePickup()
        val command = ConfirmCheckoutCommand(ready.id, ready.cartId, ready.cartVersion)
        val results = List(2) { async { repository.confirmCheckout(command) } }.awaitAll()
        val drafts = results.map { (it as CheckoutResult.Success).data }
        assertEquals(1, drafts.map { it.id }.distinct().size)
        assertEquals(1, drafts.map { it.clientReference }.distinct().size)

        val reset = async { repository.resetCheckout() }
        val after = async { repository.confirmCheckout(command) }
        reset.await()
        val outcome = after.await()
        assertTrue(outcome is CheckoutResult.Failure || outcome is CheckoutResult.Success)
        assertNull(repository.session.value)
    }

    @Test fun `simultaneous fulfillment changes and quote updates remain serialized`() = runTest {
        val started = start()
        repository.updateCustomer(
            UpdateCustomerCommand(started.id, "Ana Silva", "923456789", null)
        )
        val fulfillmentResults = listOf(
            FulfillmentMethod.PICKUP,
            FulfillmentMethod.DELIVERY_MOCK
        ).map { method ->
            async { repository.selectFulfillment(SelectFulfillmentCommand(started.id, method)) }
        }.awaitAll()
        assertEquals(
            listOf(2L, 3L),
            fulfillmentResults.map { (it as CheckoutResult.Success).data.version }.sorted()
        )
        assertTrue(repository.session.value?.selectedFulfillmentMethod in setOf(
            FulfillmentMethod.PICKUP, FulfillmentMethod.DELIVERY_MOCK
        ))

        // Establish pickup details regardless of which concurrent selection won.
        repository.selectFulfillment(SelectFulfillmentCommand(started.id, FulfillmentMethod.PICKUP))
        repository.updatePickupDetails(UpdatePickupDetailsCommand(started.id, "Ana Silva", "923456789"))
        val session = repository.session.value!!
        val quoteCommand = RequestQuoteCommand(session.id, session.cartId, session.cartVersion)
        val quoteAndCustomer = listOf(
            async { repository.requestQuote(quoteCommand) },
            async {
                repository.updateCustomer(
                    UpdateCustomerCommand(session.id, "Maria Costa", "923456789", null)
                )
            }
        ).awaitAll()
        assertTrue(quoteAndCustomer.all { it is CheckoutResult.Success })
        assertEquals(CheckoutSessionStatus.ReadyForReview, repository.session.value?.status)
        assertEquals("Maria Costa", repository.session.value?.customer?.fullName)
    }

    @Test fun `two simultaneous quotes preserve monotonic version without invalid states`() = runTest {
        val ready = completePickup()
        val command = RequestQuoteCommand(ready.id, ready.cartId, ready.cartVersion)
        val results = List(2) { async { repository.requestQuote(command) } }.awaitAll()
            .map { (it as CheckoutResult.Success).data }
        assertEquals(listOf(ready.version + 1, ready.version + 2), results.map { it.version }.sorted())
        assertEquals(2, results.mapNotNull { it.quote?.id }.distinct().size)
        assertTrue(results.all { it.status == CheckoutSessionStatus.ReadyForReview })
    }

    @Test fun `cart mutation racing confirmation has a linearized safe outcome`() = runTest {
        val ready = completePickup()
        val itemId = cart.cart.value.items.single().id
        val mutate = async { cart.updateQuantity(UpdateCartItemQuantityCommand(itemId, 2)) }
        val confirm = async {
            repository.confirmCheckout(
                ConfirmCheckoutCommand(ready.id, ready.cartId, ready.cartVersion)
            )
        }
        mutate.await()
        val result = confirm.await()
        assertTrue(result is CheckoutResult.Conflict)
        assertEquals(CheckoutSessionStatus.ReadyForReview, repository.session.value?.status)
    }
}
