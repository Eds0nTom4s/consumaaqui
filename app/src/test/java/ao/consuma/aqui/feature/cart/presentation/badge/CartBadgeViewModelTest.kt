package ao.consuma.aqui.feature.cart.presentation.badge

import ao.consuma.aqui.feature.cart.addCommand
import ao.consuma.aqui.feature.cart.data.InMemoryCartRepository
import ao.consuma.aqui.feature.cart.domain.command.RemoveCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.UpdateCartItemQuantityCommand
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.cart.domain.service.CartItemFactory
import ao.consuma.aqui.feature.cart.domain.service.CartItemIdentityFactory
import ao.consuma.aqui.feature.cart.domain.service.CartMerchantPolicy
import ao.consuma.aqui.feature.cart.domain.service.CartTotalsCalculator
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartBadgeViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `badge observes add merge quantity remove and clear from repository`() = runTest {
        val repository = repository()
        val viewModel = CartBadgeViewModel(repository, CartUiMapper())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect() }
        assertFalse(viewModel.uiState.value.visible)

        val added = repository.addItem(addCommand()) as AddCartItemResult.Added
        assertEquals("1", viewModel.uiState.value.displayText)
        repository.addItem(addCommand(quantity = 9))
        assertEquals("10", viewModel.uiState.value.displayText)
        repository.updateQuantity(UpdateCartItemQuantityCommand(added.item.id, 99))
        assertEquals("99", viewModel.uiState.value.displayText)
        repository.addItem(addCommand(productId = "other", optionId = "small"))
        assertEquals("99+", viewModel.uiState.value.displayText)
        repository.removeItem(RemoveCartItemCommand(added.item.id))
        assertEquals("1", viewModel.uiState.value.displayText)
        repository.clearCart()
        assertFalse(viewModel.uiState.value.visible)
    }

    @Test fun `conflict does not change badge and atomic replacement does`() = runTest {
        val repository = repository()
        val viewModel = CartBadgeViewModel(repository, CartUiMapper())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect() }
        repository.addItem(addCommand(quantity = 2))
        val before = viewModel.uiState.value
        repository.addItem(addCommand(merchantId = "other", merchantName = "Other", quantity = 7))
        assertEquals(before, viewModel.uiState.value)
        repository.replaceCartWithItem(
            addCommand(merchantId = "other", merchantName = "Other", quantity = 7)
        )
        assertTrue(viewModel.uiState.value.visible)
        assertEquals("7", viewModel.uiState.value.displayText)
    }

    private fun repository(): InMemoryCartRepository {
        val identity = CartItemIdentityFactory()
        return InMemoryCartRepository(
            CartItemFactory(identity), identity, CartTotalsCalculator(), CartMerchantPolicy()
        )
    }
}
