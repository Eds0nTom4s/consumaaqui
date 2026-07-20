package ao.consuma.aqui.feature.checkout.presentation.checkout

import androidx.lifecycle.SavedStateHandle
import ao.consuma.aqui.feature.cart.addCommand
import ao.consuma.aqui.feature.cart.data.InMemoryCartRepository
import ao.consuma.aqui.feature.cart.domain.command.UpdateCartItemQuantityCommand
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.checkout.data.InMemoryCheckoutRepository
import ao.consuma.aqui.feature.checkout.domain.command.ConfirmCheckoutCommand
import ao.consuma.aqui.feature.checkout.domain.command.RequestQuoteCommand
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutDraft
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.repository.CheckoutRepository
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutResult
import ao.consuma.aqui.feature.checkout.domain.service.StartCheckout
import ao.consuma.aqui.feature.checkout.fixedClock
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiMapper
import ao.consuma.aqui.feature.checkout.repositoryFixture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: InMemoryCheckoutRepository
    private lateinit var cart: InMemoryCartRepository

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        repositoryFixture().also { (checkout, cartRepository) ->
            repository = checkout
            cart = cartRepository
        }
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        checkoutRepository: CheckoutRepository = repository,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = CheckoutViewModel(
        checkoutRepository,
        StartCheckout(checkoutRepository, cart),
        CheckoutUiMapper(fixedClock),
        savedStateHandle
    )

    private suspend fun populateCart(): AddCartItemResult.Added = cart.addItem(
        addCommand(merchantId = "sabor-maianga", merchantName = "Sabor da Maianga")
    ) as AddCartItemResult.Added

    private suspend fun initialized(
        checkoutRepository: CheckoutRepository = repository,
        handle: SavedStateHandle = SavedStateHandle()
    ): CheckoutViewModel {
        populateCart()
        return viewModel(checkoutRepository, handle).also {
            it.onEvent(CheckoutUiEvent.Initialize)
        }
    }

    private fun content(viewModel: CheckoutViewModel) = viewModel.uiState.value as CheckoutUiState.Content

    private suspend fun toCustomer(viewModel: CheckoutViewModel) {
        viewModel.onEvent(CheckoutUiEvent.FulfillmentSelected(FulfillmentMethod.PICKUP))
        viewModel.onEvent(CheckoutUiEvent.Continue)
    }

    private suspend fun toPickupDetails(viewModel: CheckoutViewModel) {
        toCustomer(viewModel)
        viewModel.onEvent(CheckoutUiEvent.CustomerNameChanged("Ana Silva"))
        viewModel.onEvent(CheckoutUiEvent.CustomerPhoneChanged("+244 923 456 789"))
        viewModel.onEvent(CheckoutUiEvent.CustomerEmailChanged("ANA@example.com"))
        viewModel.onEvent(CheckoutUiEvent.Continue)
    }

    private suspend fun toPickupReview(viewModel: CheckoutViewModel) {
        toPickupDetails(viewModel)
        viewModel.onEvent(CheckoutUiEvent.Continue)
    }

    @Test fun `initial state initializes valid cart and empty cart is controlled`() = runTest {
        val emptyViewModel = viewModel()
        assertSame(CheckoutUiState.Initializing, emptyViewModel.uiState.value)
        emptyViewModel.onEvent(CheckoutUiEvent.Initialize)
        val empty = emptyViewModel.uiState.value as CheckoutUiState.Error
        assertFalse(empty.canRetry)

        val valid = initialized()
        val state = content(valid)
        assertEquals(CheckoutStep.FULFILLMENT, state.currentStep)
        assertEquals(repository.session.value?.id, state.sessionId)
        assertEquals(CheckoutSessionStatus.Started, repository.session.value?.status)
    }

    @Test fun `compatible session is reused and changed cart starts a fresh snapshot`() = runTest {
        val first = initialized()
        val initialSession = content(first).sessionId
        val second = viewModel().also { it.onEvent(CheckoutUiEvent.Initialize) }
        assertEquals(initialSession, content(second).sessionId)

        cart.updateQuantity(
            UpdateCartItemQuantityCommand(cart.cart.value.items.single().id, 2)
        )
        val third = viewModel().also { it.onEvent(CheckoutUiEvent.Initialize) }
        assertNotEquals(initialSession, content(third).sessionId)
        assertEquals(2, content(third).cartSummary.itemCount)
    }

    @Test fun `fulfillment selection is explicit unavailable is rejected and customer survives changes`() = runTest {
        val viewModel = initialized()
        viewModel.onEvent(CheckoutUiEvent.FulfillmentSelected(FulfillmentMethod.PICKUP))
        assertEquals(FulfillmentMethod.PICKUP, content(viewModel).selectedFulfillment)
        toCustomer(viewModel)
        viewModel.onEvent(CheckoutUiEvent.CustomerNameChanged("Ana Silva"))
        viewModel.onEvent(CheckoutUiEvent.CustomerPhoneChanged("923456789"))
        viewModel.onEvent(CheckoutUiEvent.Continue)
        viewModel.onEvent(CheckoutUiEvent.FulfillmentSelected(FulfillmentMethod.DELIVERY_MOCK))
        assertEquals("Ana Silva", repository.session.value?.customer?.fullName)
        assertNull(repository.session.value?.quote)
    }

    @Test fun `customer fields validate preserve text save normalized data and prefill pickup`() = runTest {
        val viewModel = initialized()
        toCustomer(viewModel)
        viewModel.onEvent(CheckoutUiEvent.CustomerNameChanged("A"))
        viewModel.onEvent(CheckoutUiEvent.CustomerPhoneChanged("invalid"))
        viewModel.onEvent(CheckoutUiEvent.CustomerEmailChanged("invalid"))
        viewModel.onEvent(CheckoutUiEvent.Continue)
        val invalid = content(viewModel)
        assertEquals(CheckoutStep.CUSTOMER, invalid.currentStep)
        assertTrue(invalid.customerForm.fullNameError != null)
        assertTrue(invalid.customerForm.phoneError != null)
        assertTrue(invalid.customerForm.emailError != null)
        assertEquals("A", invalid.customerForm.fullName)

        viewModel.onEvent(CheckoutUiEvent.CustomerNameChanged(" Ana Silva "))
        viewModel.onEvent(CheckoutUiEvent.CustomerPhoneChanged("+244 923 456 789"))
        viewModel.onEvent(CheckoutUiEvent.CustomerEmailChanged(" ANA@example.com "))
        viewModel.onEvent(CheckoutUiEvent.Continue)
        val details = content(viewModel)
        assertEquals(CheckoutStep.DETAILS, details.currentStep)
        assertEquals("Ana Silva", repository.session.value?.customer?.fullName)
        assertEquals("+244923456789", repository.session.value?.customer?.contact?.phoneNumber)
        assertEquals("Ana Silva", details.pickupForm.contactName)
    }

    @Test fun `pickup edits save asap request quote and build review`() = runTest {
        val viewModel = initialized()
        toPickupDetails(viewModel)
        viewModel.onEvent(CheckoutUiEvent.PickupNameChanged("Maria Costa"))
        viewModel.onEvent(CheckoutUiEvent.PickupPhoneChanged("923456789"))
        viewModel.onEvent(CheckoutUiEvent.Continue)
        val review = content(viewModel)
        assertEquals(CheckoutStep.REVIEW, review.currentStep)
        assertEquals("Maria Costa", (repository.session.value?.fulfillment as
            ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment.Pickup).details.contactName)
        assertTrue(review.quote != null)
        assertTrue(review.canConfirm)
    }

    @Test fun `delivery fields address reference recipient and counter are preserved and quoted`() = runTest {
        val viewModel = initialized()
        viewModel.onEvent(CheckoutUiEvent.FulfillmentSelected(FulfillmentMethod.DELIVERY_MOCK))
        viewModel.onEvent(CheckoutUiEvent.Continue)
        viewModel.onEvent(CheckoutUiEvent.CustomerNameChanged("Ana Silva"))
        viewModel.onEvent(CheckoutUiEvent.CustomerPhoneChanged("923456789"))
        viewModel.onEvent(CheckoutUiEvent.Continue)
        viewModel.onEvent(CheckoutUiEvent.DeliveryProvinceChanged("Luanda"))
        viewModel.onEvent(CheckoutUiEvent.DeliveryMunicipalityChanged("Talatona"))
        viewModel.onEvent(CheckoutUiEvent.DeliveryAreaChanged("Benfica"))
        viewModel.onEvent(CheckoutUiEvent.DeliveryStreetChanged("Rua 10"))
        viewModel.onEvent(CheckoutUiEvent.DeliveryBuildingChanged("Casa azul"))
        viewModel.onEvent(CheckoutUiEvent.DeliveryReferenceChanged("Próximo ao Banco BIC"))
        viewModel.onEvent(CheckoutUiEvent.DeliveryInstructionsChanged("a".repeat(300)))
        viewModel.onEvent(CheckoutUiEvent.DeliveryInstructionsChanged("a".repeat(301)))
        assertEquals(300, content(viewModel).deliveryForm.instructionsCount)
        viewModel.onEvent(CheckoutUiEvent.Continue)
        val review = content(viewModel)
        assertEquals(CheckoutStep.REVIEW, review.currentStep)
        assertEquals(1, review.quote?.charges?.size)
        assertEquals("Próximo ao Banco BIC", (
            repository.session.value?.fulfillment as
                ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment.Delivery
            ).details.address.referencePoint)
    }

    @Test fun `cart change during quote presents recoverable conflict and restart uses current cart`() = runTest {
        val viewModel = initialized()
        toPickupDetails(viewModel)
        cart.updateQuantity(UpdateCartItemQuantityCommand(cart.cart.value.items.single().id, 2))
        viewModel.onEvent(CheckoutUiEvent.Continue)
        assertTrue(
            viewModel.uiState.value.toString(),
            viewModel.uiState.value is CheckoutUiState.CartConflict
        )
        val previous = repository.session.value?.id
        viewModel.onEvent(CheckoutUiEvent.RestartCheckout)
        assertTrue(viewModel.uiState.value is CheckoutUiState.Content)
        assertNotEquals(previous, content(viewModel).sessionId)
        assertEquals(2, content(viewModel).cartSummary.itemCount)
    }

    @Test fun `quote request blocks duplicate operation and keeps visible form`() = runTest {
        val blocking = BlockingQuoteRepository(repository)
        val viewModel = initialized(blocking)
        toPickupDetails(viewModel)
        viewModel.onEvent(CheckoutUiEvent.Continue)
        blocking.started.await()
        assertEquals(CheckoutPendingOperation.RequestingQuote, content(viewModel).pendingOperation)
        viewModel.onEvent(CheckoutUiEvent.Continue)
        assertEquals(1, blocking.calls)
        blocking.release.complete(Unit)
        advanceUntilIdle()
        assertEquals(CheckoutStep.REVIEW, content(viewModel).currentStep)
    }

    @Test fun `quote expiration keeps data and refresh obtains another quote`() = runTest {
        val viewModel = initialized()
        toPickupReview(viewModel)
        val failing = ConfirmFailureRepository(repository, CheckoutError.QuoteExpired)
        val restored = viewModel(failing, SavedStateHandle(mapOf(
            CheckoutSavedStateKeys.SESSION_ID to repository.session.value?.id,
            CheckoutSavedStateKeys.STEP to CheckoutStep.REVIEW.name
        )))
        restored.onEvent(CheckoutUiEvent.Initialize)
        restored.onEvent(CheckoutUiEvent.Confirm)
        assertTrue(content(restored).quote?.expired == true)
        assertFalse(content(restored).canConfirm)
        restored.onEvent(CheckoutUiEvent.RefreshQuote)
        assertEquals(CheckoutStep.REVIEW, content(restored).currentStep)
        assertFalse(content(restored).quote?.expired == true)
    }

    @Test fun `confirmation emits navigation stores one draft and preserves cart`() = runTest {
        val viewModel = initialized()
        toPickupReview(viewModel)
        val effects = mutableListOf<CheckoutUiEffect>()
        backgroundScope.launch(dispatcher) { viewModel.effects.collect { effects += it } }
        val cartBefore = cart.cart.value
        viewModel.onEvent(CheckoutUiEvent.Confirm)
        assertSame(CheckoutUiState.ConfirmedMock, viewModel.uiState.value)
        assertTrue(repository.draft.value != null)
        assertEquals(cartBefore, cart.cart.value)
        assertEquals(listOf(CheckoutUiEffect.NavigateToConfirmation), effects)
    }

    @Test fun `back traverses steps first step returns cart and form data stays`() = runTest {
        val viewModel = initialized()
        val effects = mutableListOf<CheckoutUiEffect>()
        backgroundScope.launch(dispatcher) { viewModel.effects.collect { effects += it } }
        toCustomer(viewModel)
        viewModel.onEvent(CheckoutUiEvent.CustomerNameChanged("Ana Silva"))
        viewModel.onEvent(CheckoutUiEvent.BackStep)
        assertEquals(CheckoutStep.FULFILLMENT, content(viewModel).currentStep)
        viewModel.onEvent(CheckoutUiEvent.BackStep)
        assertTrue(effects.contains(CheckoutUiEffect.NavigateToCart))
        viewModel.onEvent(CheckoutUiEvent.Continue)
        assertEquals("Ana Silva", content(viewModel).customerForm.fullName)
    }

    @Test fun `saved state restores primitive fields and never stores domain models`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = initialized(handle = handle)
        toCustomer(viewModel)
        viewModel.onEvent(CheckoutUiEvent.CustomerNameChanged("Ana Silva"))
        viewModel.onEvent(CheckoutUiEvent.CustomerPhoneChanged("923456789"))
        viewModel.onEvent(CheckoutUiEvent.CustomerEmailChanged("ana@example.com"))
        assertEquals("Ana Silva", handle.get<String>(CheckoutSavedStateKeys.CUSTOMER_NAME))
        assertEquals(CheckoutStep.CUSTOMER.name, handle.get<String>(CheckoutSavedStateKeys.STEP))
        assertTrue(handle.keys().all { key ->
            val value = handle.get<Any>(key)
            value == null || value is String
        })
        assertTrue(handle.keys().none { it.contains("draft") || it.contains("quote") || it.contains("cart") })
    }

    @Test fun `typing remains local clears errors and does not mutate repository per character`() = runTest {
        val viewModel = initialized()
        toCustomer(viewModel)
        val versionBeforeTyping = repository.session.value?.version
        viewModel.onEvent(CheckoutUiEvent.Continue)
        assertTrue(content(viewModel).customerForm.fullNameError != null)
        viewModel.onEvent(CheckoutUiEvent.CustomerNameChanged("Ana Teste"))
        viewModel.onEvent(CheckoutUiEvent.CustomerPhoneChanged("923000111"))
        viewModel.onEvent(CheckoutUiEvent.CustomerEmailChanged("ana.teste@example.com"))
        assertNull(content(viewModel).customerForm.fullNameError)
        assertNull(content(viewModel).customerForm.phoneError)
        assertNull(content(viewModel).customerForm.emailError)
        assertNull(repository.session.value?.customer)
        assertEquals(versionBeforeTyping, repository.session.value?.version)
        viewModel.onEvent(CheckoutUiEvent.Continue)
        assertEquals("Ana Teste", repository.session.value?.customer?.fullName)
    }

    @Test fun `double initialize is ignored and invalid saved step is sanitized`() = runTest {
        populateCart()
        val invalidHandle = SavedStateHandle(mapOf(CheckoutSavedStateKeys.STEP to "INVALID"))
        val viewModel = viewModel(savedStateHandle = invalidHandle)
        viewModel.onEvent(CheckoutUiEvent.Initialize)
        val sessionId = content(viewModel).sessionId
        viewModel.onEvent(CheckoutUiEvent.Initialize)
        assertEquals(sessionId, content(viewModel).sessionId)
        assertEquals(CheckoutStep.FULFILLMENT, content(viewModel).currentStep)
    }

    @Test fun `new checkout after confirmation discards previous personal form data`() = runTest {
        val first = initialized()
        toPickupReview(first)
        val confirmedSessionId = repository.session.value?.id
        first.onEvent(CheckoutUiEvent.Confirm)
        assertTrue(repository.draft.value != null)

        val next = viewModel().also { it.onEvent(CheckoutUiEvent.Initialize) }
        val state = content(next)
        assertNotEquals(confirmedSessionId, state.sessionId)
        assertEquals("", state.customerForm.fullName)
        assertEquals("", state.customerForm.phone)
        assertEquals("", state.customerForm.email)
        assertEquals("", state.pickupForm.contactName)
        assertNull(repository.draft.value)
    }

    private class BlockingQuoteRepository(
        private val delegate: CheckoutRepository
    ) : CheckoutRepository by delegate {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        override suspend fun requestQuote(command: RequestQuoteCommand): CheckoutResult<CheckoutSession> {
            calls++
            started.complete(Unit)
            release.await()
            return delegate.requestQuote(command)
        }
    }

    private class ConfirmFailureRepository(
        private val delegate: CheckoutRepository,
        private val error: CheckoutError
    ) : CheckoutRepository by delegate {
        override suspend fun confirmCheckout(command: ConfirmCheckoutCommand): CheckoutResult<CheckoutDraft> =
            CheckoutResult.Failure(error)
    }
}
