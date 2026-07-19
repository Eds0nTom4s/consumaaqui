package ao.consuma.aqui.feature.cart.presentation.cart

import ao.consuma.aqui.feature.cart.addCommand
import ao.consuma.aqui.feature.cart.data.InMemoryCartRepository
import ao.consuma.aqui.feature.cart.domain.service.CartItemFactory
import ao.consuma.aqui.feature.cart.domain.service.CartItemIdentityFactory
import ao.consuma.aqui.feature.cart.domain.service.CartMerchantPolicy
import ao.consuma.aqui.feature.cart.domain.service.CartTotalsCalculator
import ao.consuma.aqui.feature.cart.domain.command.UpdateCartItemQuantityCommand
import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.cart.domain.result.CartError
import ao.consuma.aqui.feature.cart.domain.result.CartResult
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiMapper
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `empty and repository updates are observed without a second cart copy`() = runTest {
        val repository = repository()
        val viewModel = CartViewModel(repository, CartUiMapper())
        assertTrue(viewModel.uiState.value is CartUiState.Empty)
        repository.addItem(addCommand(quantity = 2))
        val content = viewModel.uiState.value as CartUiState.Content
        assertEquals(2, content.itemCount)
        assertEquals(1, content.distinctItemCount)
        assertEquals(repository.cart.value.version, content.version)
        assertEquals("24 Kz", content.subtotalText)
    }

    @Test fun `increase decrease enforce minimum and update repository totals`() = runTest {
        val repository = repository()
        repository.addItem(addCommand())
        val viewModel = CartViewModel(repository, CartUiMapper())
        val id = repository.cart.value.items.single().id
        viewModel.onEvent(CartUiEvent.DecreaseQuantity(id))
        assertEquals(1, repository.cart.value.totals.itemCount)
        viewModel.onEvent(CartUiEvent.IncreaseQuantity(id))
        assertEquals(2, repository.cart.value.totals.itemCount)
        viewModel.onEvent(CartUiEvent.DecreaseQuantity(id))
        assertEquals(1, repository.cart.value.totals.itemCount)
    }

    @Test fun `remove confirmation cancel and final removal produce empty`() = runTest {
        val repository = repository()
        repository.addItem(addCommand())
        val viewModel = CartViewModel(repository, CartUiMapper())
        val id = repository.cart.value.items.single().id
        viewModel.onEvent(CartUiEvent.RequestRemoveItem(id))
        assertEquals(id, (viewModel.uiState.value as CartUiState.Content).pendingRemovalItemId)
        viewModel.onEvent(CartUiEvent.CancelRemoveItem)
        assertNull((viewModel.uiState.value as CartUiState.Content).pendingRemovalItemId)
        viewModel.onEvent(CartUiEvent.RequestRemoveItem(id))
        viewModel.onEvent(CartUiEvent.ConfirmRemoveItem)
        assertTrue(viewModel.uiState.value is CartUiState.Empty)
    }

    @Test fun `clear confirmation can cancel or atomically clear`() = runTest {
        val repository = repository()
        repository.addItem(addCommand())
        val viewModel = CartViewModel(repository, CartUiMapper())
        viewModel.onEvent(CartUiEvent.RequestClearCart)
        assertTrue((viewModel.uiState.value as CartUiState.Content).showClearConfirmation)
        viewModel.onEvent(CartUiEvent.CancelClearCart)
        assertFalse((viewModel.uiState.value as CartUiState.Content).showClearConfirmation)
        viewModel.onEvent(CartUiEvent.RequestClearCart)
        viewModel.onEvent(CartUiEvent.ConfirmClearCart)
        assertTrue(repository.cart.value.items.isEmpty())
        assertTrue(viewModel.uiState.value is CartUiState.Empty)
    }

    @Test fun `request dialogs do not mutate cart or version`() = runTest {
        val repository = repository()
        repository.addItem(addCommand())
        val viewModel = CartViewModel(repository, CartUiMapper())
        val before = repository.cart.value
        viewModel.onEvent(CartUiEvent.RequestClearCart)
        assertEquals(before, repository.cart.value)
        viewModel.onEvent(CartUiEvent.CancelClearCart)
        assertEquals(before.version, repository.cart.value.version)
    }

    @Test fun `maximum quantity is not exceeded`() = runTest {
        val repository = repository()
        repository.addItem(addCommand(quantity = 99))
        val viewModel = CartViewModel(repository, CartUiMapper())
        val id = repository.cart.value.items.single().id
        viewModel.onEvent(CartUiEvent.IncreaseQuantity(id))
        assertEquals(99, repository.cart.value.totals.itemCount)
        assertFalse((viewModel.uiState.value as CartUiState.Content).items.single().canIncrease)
    }

    @Test fun `edit continue and explore emit identifiers instead of cart models`() = runTest {
        val repository = repository()
        repository.addItem(addCommand())
        val viewModel = CartViewModel(repository, CartUiMapper())
        val effects = mutableListOf<CartUiEffect>()
        backgroundScope.launch(dispatcher) { viewModel.effects.collect { effects += it } }
        val item = repository.cart.value.items.single()

        viewModel.onEvent(CartUiEvent.EditItem(item.id))
        viewModel.onEvent(CartUiEvent.ContinueShopping)
        viewModel.onEvent(CartUiEvent.ExploreMerchants)

        assertEquals(CartUiEffect.EditItem(item.merchantId, item.productId, item.id), effects[0])
        assertEquals(CartUiEffect.ContinueShopping(item.merchantId), effects[1])
        assertEquals(CartUiEffect.ExploreMerchants, effects[2])
    }

    @Test fun `mutation keeps content visible and blocks duplicate events`() = runTest {
        val delegate = repository()
        delegate.addItem(addCommand())
        val repository = BlockingUpdateRepository(delegate)
        val viewModel = CartViewModel(repository, CartUiMapper())
        val id = delegate.cart.value.items.single().id

        viewModel.onEvent(CartUiEvent.IncreaseQuantity(id))
        repository.started.await()
        val mutating = viewModel.uiState.value as CartUiState.Content
        assertEquals(CartPendingOperation.UpdatingQuantity(id), mutating.pendingOperation)
        viewModel.onEvent(CartUiEvent.IncreaseQuantity(id))
        assertEquals(1, repository.updateCalls)

        repository.release.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, delegate.cart.value.totals.itemCount)
        assertNull((viewModel.uiState.value as CartUiState.Content).pendingOperation)
    }

    @Test fun `repository failure maps message and preserves content`() = runTest {
        val delegate = repository()
        delegate.addItem(addCommand())
        val viewModel = CartViewModel(FailingUpdateRepository(delegate), CartUiMapper())
        val effects = mutableListOf<CartUiEffect>()
        backgroundScope.launch(dispatcher) { viewModel.effects.collect { effects += it } }
        val id = delegate.cart.value.items.single().id

        viewModel.onEvent(CartUiEvent.IncreaseQuantity(id))

        assertTrue(viewModel.uiState.value is CartUiState.Content)
        val message = (effects.single() as CartUiEffect.Message).text as CartUiText.Resource
        assertEquals(R.string.cart_error_quantity_limit, message.id)
    }

    private fun repository(): InMemoryCartRepository {
        val identity = CartItemIdentityFactory()
        return InMemoryCartRepository(
            CartItemFactory(identity), identity, CartTotalsCalculator(), CartMerchantPolicy()
        )
    }

    private class BlockingUpdateRepository(
        private val delegate: CartRepository
    ) : CartRepository by delegate {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var updateCalls = 0
        override suspend fun updateQuantity(command: UpdateCartItemQuantityCommand): CartResult<Cart> {
            updateCalls++
            started.complete(Unit)
            release.await()
            return delegate.updateQuantity(command)
        }
    }

    private class FailingUpdateRepository(
        private val delegate: CartRepository
    ) : CartRepository by delegate {
        override suspend fun updateQuantity(command: UpdateCartItemQuantityCommand): CartResult<Cart> =
            CartResult.Failure(CartError.QuantityLimitExceeded)
    }
}
