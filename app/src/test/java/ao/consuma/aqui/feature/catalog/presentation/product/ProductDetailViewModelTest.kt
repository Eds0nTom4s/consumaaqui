package ao.consuma.aqui.feature.catalog.presentation.product

import androidx.lifecycle.SavedStateHandle
import ao.consuma.aqui.feature.cart.addCommand
import ao.consuma.aqui.feature.cart.data.InMemoryCartRepository
import ao.consuma.aqui.feature.cart.domain.command.RemoveCartItemCommand
import ao.consuma.aqui.feature.cart.domain.model.CartConflict
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.cart.domain.service.AddConfiguredProductToCart
import ao.consuma.aqui.feature.cart.domain.service.CartItemFactory
import ao.consuma.aqui.feature.cart.domain.service.CartItemIdentityFactory
import ao.consuma.aqui.feature.cart.domain.service.CartMerchantPolicy
import ao.consuma.aqui.feature.cart.domain.service.CartTotalsCalculator
import ao.consuma.aqui.feature.catalog.data.InMemoryCatalogRepository
import ao.consuma.aqui.feature.catalog.data.MockCatalogScenario
import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.MerchantCatalog
import ao.consuma.aqui.feature.catalog.domain.model.ProductSearchContent
import ao.consuma.aqui.feature.catalog.domain.repository.CatalogRepository
import ao.consuma.aqui.feature.catalog.domain.request.CatalogRequest
import ao.consuma.aqui.feature.catalog.domain.request.CatalogSearchRequest
import ao.consuma.aqui.feature.catalog.domain.request.ProductRequest
import ao.consuma.aqui.feature.catalog.domain.result.CatalogResult
import ao.consuma.aqui.feature.catalog.domain.service.ProductConfigurationValidator
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiMapper
import java.util.ArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before fun setUpMainDispatcher() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test fun `initial load applies defaults and calculates total`() {
        val viewModel = viewModel(InMemoryCatalogRepository())
        assertEquals(ProductDetailUiState.Loading, viewModel.uiState.value)
        viewModel.onEvent(ProductDetailUiEvent.Load)
        val state = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertEquals(setOf("muamba-casa-portion-individual"), state.selections["muamba-casa-portion"])
        assertEquals(setOf("muamba-casa-side-rice"), state.selections["muamba-casa-side"])
        assertEquals("4.500 Kz", state.priceSummary.totalPriceText)
    }

    @Test fun `invalid ids not found unavailable offline and error are explicit`() {
        val invalidCart = cartRepository()
        val invalid = ProductDetailViewModel(
            InMemoryCatalogRepository(), AddConfiguredProductToCart(invalidCart), invalidCart,
            ProductConfigurationValidator(), CatalogUiMapper(),
            SavedStateHandle(), mainDispatcher
        )
        invalid.onEvent(ProductDetailUiEvent.Load)
        assertEquals(ProductDetailUiState.InvalidArguments, invalid.uiState.value)

        val missing = viewModel(InMemoryCatalogRepository(), productId = "missing")
        missing.onEvent(ProductDetailUiEvent.Load)
        assertEquals(ProductDetailUiState.NotFound, missing.uiState.value)

        val unavailable = viewModel(InMemoryCatalogRepository(), productId = "sabor-maianga-product-calulu-demonstrativo")
        unavailable.onEvent(ProductDetailUiEvent.Load)
        assertTrue(unavailable.uiState.value is ProductDetailUiState.Unavailable)

        val repository = InMemoryCatalogRepository().apply { scenario = MockCatalogScenario.OFFLINE }
        val offline = viewModel(repository)
        offline.onEvent(ProductDetailUiEvent.Load)
        assertTrue((offline.uiState.value as ProductDetailUiState.Content).data.isOffline)
        repository.scenario = MockCatalogScenario.ERROR
        offline.onEvent(ProductDetailUiEvent.Retry)
        assertTrue(offline.uiState.value is ProductDetailUiState.Error)
    }

    @Test fun `option toggles respect availability and maximum and update price`() {
        val viewModel = viewModel(InMemoryCatalogRepository(), productId = "sabor-maianga-product-frango-grelhado")
        viewModel.onEvent(ProductDetailUiEvent.Load)
        viewModel.onEvent(ProductDetailUiEvent.OptionToggled("frango-grelhado-extras", "frango-grelhado-extras-cheese"))
        viewModel.onEvent(ProductDetailUiEvent.OptionToggled("frango-grelhado-extras", "frango-grelhado-extras-egg"))
        viewModel.onEvent(ProductDetailUiEvent.OptionToggled("frango-grelhado-extras", "frango-grelhado-extras-bacon"))
        val state = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertEquals(2, state.selections["frango-grelhado-extras"]?.size)
        assertTrue("frango-grelhado-extras-bacon" !in state.selections["frango-grelhado-extras"].orEmpty())
        assertEquals("4.800 Kz", state.priceSummary.totalPriceText)
    }

    @Test fun `optional single choice can be cleared while required single choice cannot`() {
        val viewModel = viewModel(
            InMemoryCatalogRepository(),
            productId = "sabor-maianga-product-frango-grelhado"
        )
        viewModel.onEvent(ProductDetailUiEvent.Load)

        viewModel.onEvent(
            ProductDetailUiEvent.OptionToggled(
                "frango-grelhado-sauce",
                "frango-grelhado-sauce-none"
            )
        )
        var content = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertNull(content.selections["frango-grelhado-sauce"])

        val required = viewModel(InMemoryCatalogRepository())
        required.onEvent(ProductDetailUiEvent.Load)
        required.onEvent(
            ProductDetailUiEvent.OptionToggled(
                "muamba-casa-portion",
                "muamba-casa-portion-individual"
            )
        )
        content = (required.uiState.value as ProductDetailUiState.Content).data
        assertEquals(
            setOf("muamba-casa-portion-individual"),
            content.selections["muamba-casa-portion"]
        )
    }

    @Test fun `quantity note and selections update and persist with controlled limits`() {
        val saved = state()
        val viewModel = viewModel(InMemoryCatalogRepository(), state = saved)
        viewModel.onEvent(ProductDetailUiEvent.Load)
        viewModel.onEvent(ProductDetailUiEvent.QuantityDecreased)
        assertEquals(1, (viewModel.uiState.value as ProductDetailUiState.Content).data.quantity)
        repeat(120) { viewModel.onEvent(ProductDetailUiEvent.QuantityIncreased) }
        viewModel.onEvent(ProductDetailUiEvent.NoteChanged("a".repeat(300)))
        val content = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertEquals(99, content.quantity)
        assertEquals(250, content.note.length)

        val restored = viewModel(InMemoryCatalogRepository(), state = saved)
        restored.onEvent(ProductDetailUiEvent.Load)
        val restoredContent = (restored.uiState.value as ProductDetailUiState.Content).data
        assertEquals(99, restoredContent.quantity)
        assertEquals(250, restoredContent.note.length)
    }

    @Test fun `invalid add keeps configuration and exposes group errors`() {
        val saved = state().apply {
            this[ProductDetailSavedStateKeys.SELECTIONS] = ArrayList(listOf("invalid-collection"))
        }
        val viewModel = viewModel(InMemoryCatalogRepository(), state = saved)
        viewModel.onEvent(ProductDetailUiEvent.Load)
        var cartResult: AddCartItemResult? = null
        viewModel.onEvent(ProductDetailUiEvent.Add) { cartResult = it }
        val content = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertNull(cartResult)
        assertTrue(content.validationErrors.any { it.groupId == "muamba-casa-portion" })
        assertEquals(1, content.quantity)
    }

    @Test fun `valid add sends configured snapshot through cart boundary`() {
        val viewModel = viewModel(InMemoryCatalogRepository())
        viewModel.onEvent(ProductDetailUiEvent.Load)
        viewModel.onEvent(ProductDetailUiEvent.QuantityIncreased)
        viewModel.onEvent(ProductDetailUiEvent.NoteChanged("  sem talheres  "))
        var result: AddCartItemResult? = null
        viewModel.onEvent(ProductDetailUiEvent.Add) { result = it }
        val item = (result as AddCartItemResult.Added).item
        assertEquals(2, item.quantity)
        assertEquals("sem talheres", item.note)
        assertEquals(900_000L, item.totalPrice.amountMinor)
        assertEquals(
            "Catálogo Sabor da Maianga",
            (result as AddCartItemResult.Added).cart.merchant?.name
        )
    }

    @Test fun `double add emits a configured product once until configuration changes`() {
        val viewModel = viewModel(InMemoryCatalogRepository())
        viewModel.onEvent(ProductDetailUiEvent.Load)
        var emissions = 0
        repeat(2) { viewModel.onEvent(ProductDetailUiEvent.Add) { emissions++ } }
        assertEquals(1, emissions)
        viewModel.onEvent(ProductDetailUiEvent.QuantityIncreased)
        viewModel.onEvent(ProductDetailUiEvent.Add) { emissions++ }
        assertEquals(2, emissions)
    }

    @Test fun `merchant conflict is explicit and allows a controlled retry`() = runTest {
        val cartRepository = cartRepository()
        cartRepository.addItem(
            addCommand(merchantId = "merchant-two", merchantName = "Merchant Two")
        )
        val viewModel = viewModel(
            InMemoryCatalogRepository(),
            cart = cartRepository
        )
        viewModel.onEvent(ProductDetailUiEvent.Load)
        val results = mutableListOf<AddCartItemResult>()

        repeat(2) {
            viewModel.onEvent(ProductDetailUiEvent.Add, results::add)
        }

        assertEquals(2, results.size)
        assertTrue(results.all { it is AddCartItemResult.Conflict })
        val conflict = (results.first() as AddCartItemResult.Conflict).conflict
            as CartConflict.DifferentMerchant
        assertEquals("merchant-two", conflict.currentMerchant.id)
        assertEquals("sabor-maianga", conflict.requestedMerchant.id)
        assertEquals(1, cartRepository.cart.value.items.size)
    }

    @Test fun `keeping merchant conflict preserves configuration cart and version`() = runTest {
        val cart = cartRepository()
        cart.addItem(addCommand(merchantId = "merchant-two", merchantName = "Merchant Two"))
        val before = cart.cart.value
        val viewModel = viewModel(InMemoryCatalogRepository(), cart = cart)
        viewModel.onEvent(ProductDetailUiEvent.Load)
        viewModel.onEvent(ProductDetailUiEvent.QuantityIncreased)
        viewModel.onEvent(ProductDetailUiEvent.Add)
        var content = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertEquals(2, content.quantity)
        assertTrue(content.conflict != null)

        viewModel.onEvent(ProductDetailUiEvent.KeepCurrentCart)
        content = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertNull(content.conflict)
        assertEquals(2, content.quantity)
        assertEquals(before, cart.cart.value)
    }

    @Test fun `replacing merchant conflict is atomic and creates requested cart`() = runTest {
        val cart = cartRepository()
        cart.addItem(addCommand(merchantId = "merchant-two", merchantName = "Merchant Two"))
        val oldId = cart.cart.value.id
        val viewModel = viewModel(InMemoryCatalogRepository(), cart = cart)
        viewModel.onEvent(ProductDetailUiEvent.Load)
        viewModel.onEvent(ProductDetailUiEvent.Add)
        viewModel.onEvent(ProductDetailUiEvent.ReplaceCart)

        assertEquals("sabor-maianga", cart.cart.value.merchant?.id)
        assertEquals(1, cart.cart.value.items.size)
        assertTrue(oldId != cart.cart.value.id)
    }

    @Test fun `valid cart item opens edit mode restores data and saves replacement`() = runTest {
        val cart = cartRepository()
        val add = viewModel(InMemoryCatalogRepository(), cart = cart)
        add.onEvent(ProductDetailUiEvent.Load)
        add.onEvent(ProductDetailUiEvent.QuantityIncreased)
        add.onEvent(ProductDetailUiEvent.NoteChanged("sem talheres"))
        add.onEvent(ProductDetailUiEvent.Add)
        val original = cart.cart.value.items.single()
        val saved = state().apply { this[ProductDetailSavedStateKeys.CART_ITEM_ID] = original.id }
        val viewModel = viewModel(InMemoryCatalogRepository(), state = saved, cart = cart)
        viewModel.onEvent(ProductDetailUiEvent.Load)
        val content = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertEquals(ProductDetailMode.EDIT, content.mode)
        assertEquals(2, content.quantity)
        assertEquals("sem talheres", content.note)

        val version = cart.cart.value.version
        viewModel.onEvent(ProductDetailUiEvent.QuantityIncreased)
        viewModel.onEvent(ProductDetailUiEvent.Add)
        assertEquals(3, cart.cart.value.items.single().quantity)
        assertEquals(version + 1, cart.cart.value.version)
    }

    @Test fun `invalid edit item and mismatched route are controlled`() = runTest {
        val cart = cartRepository()
        val missing = viewModel(
            InMemoryCatalogRepository(),
            state = state().apply { this[ProductDetailSavedStateKeys.CART_ITEM_ID] = "missing" },
            cart = cart
        )
        missing.onEvent(ProductDetailUiEvent.Load)
        assertEquals(ProductDetailUiState.NotFound, missing.uiState.value)

        val added = cart.addItem(addCommand()) as AddCartItemResult.Added
        val mismatch = viewModel(
            InMemoryCatalogRepository(),
            state = state().apply { this[ProductDetailSavedStateKeys.CART_ITEM_ID] = added.item.id },
            cart = cart
        )
        mismatch.onEvent(ProductDetailUiEvent.Load)
        assertEquals(ProductDetailUiState.InvalidArguments, mismatch.uiState.value)
    }

    @Test fun `item removed externally before edit save is handled without resurrection`() = runTest {
        val cart = cartRepository()
        val add = viewModel(InMemoryCatalogRepository(), cart = cart)
        add.onEvent(ProductDetailUiEvent.Load)
        add.onEvent(ProductDetailUiEvent.Add)
        val item = cart.cart.value.items.single()
        val edit = viewModel(
            InMemoryCatalogRepository(),
            state = state().apply { this[ProductDetailSavedStateKeys.CART_ITEM_ID] = item.id },
            cart = cart
        )
        edit.onEvent(ProductDetailUiEvent.Load)
        cart.removeItem(RemoveCartItemCommand(item.id))

        edit.onEvent(ProductDetailUiEvent.Add)

        assertTrue(edit.uiState.value is ProductDetailUiState.Content)
        assertTrue(cart.cart.value.items.isEmpty())
    }

    @Test fun `validation attempt and group errors survive recreation`() {
        val saved = state().apply {
            this[ProductDetailSavedStateKeys.SELECTIONS] = ArrayList<String>()
        }
        val first = viewModel(InMemoryCatalogRepository(), state = saved)
        first.onEvent(ProductDetailUiEvent.Load)
        first.onEvent(ProductDetailUiEvent.Add)
        assertEquals(true, saved.get<Boolean>(ProductDetailSavedStateKeys.VALIDATION_ATTEMPTED))

        val restored = viewModel(InMemoryCatalogRepository(), state = saved)
        restored.onEvent(ProductDetailUiEvent.Load)
        val errors = (restored.uiState.value as ProductDetailUiState.Content).data.validationErrors
        assertTrue(errors.any { it.groupId == "muamba-casa-portion" })
    }

    @Test fun `invalid saved quantity note and selections are sanitized`() {
        val saved = state().apply {
            this[ProductDetailSavedStateKeys.QUANTITY] = -20
            this[ProductDetailSavedStateKeys.NOTE] = "x".repeat(300)
            this[ProductDetailSavedStateKeys.SELECTIONS] = ArrayList(
                listOf(
                    "missing\u001Fmissing",
                    "muamba-casa-portion\u001Fmissing",
                    "muamba-casa-portion\u001Fmuamba-casa-portion-individual"
                )
            )
        }
        val viewModel = viewModel(InMemoryCatalogRepository(), state = saved)
        viewModel.onEvent(ProductDetailUiEvent.Load)
        val content = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertEquals(1, content.quantity)
        assertEquals(250, content.note.length)
        assertEquals(setOf("muamba-casa-portion-individual"), content.selections["muamba-casa-portion"])
        assertNull(content.selections["missing"])
    }

    @Test fun `retry repeats exact product request`() {
        val repository = RecordingRepository(InMemoryCatalogRepository())
        val viewModel = viewModel(repository)
        viewModel.onEvent(ProductDetailUiEvent.Load)
        viewModel.onEvent(ProductDetailUiEvent.Retry)
        assertEquals(2, repository.productRequests.size)
        assertEquals(repository.productRequests.first(), repository.productRequests.last())
    }

    private fun state() = SavedStateHandle(
        mapOf(
            "merchantId" to "sabor-maianga",
            "productId" to "sabor-maianga-product-muamba-casa"
        )
    )

    private fun viewModel(
        repository: CatalogRepository,
        productId: String = "sabor-maianga-product-muamba-casa",
        state: SavedStateHandle = SavedStateHandle(mapOf("merchantId" to "sabor-maianga", "productId" to productId)),
        cart: InMemoryCartRepository = cartRepository()
    ) = ProductDetailViewModel(
        repository,
        AddConfiguredProductToCart(cart),
        cart,
        ProductConfigurationValidator(),
        CatalogUiMapper(),
        state,
        mainDispatcher
    )

    private fun cartRepository(): InMemoryCartRepository {
        val identity = CartItemIdentityFactory()
        return InMemoryCartRepository(
            CartItemFactory(identity), identity, CartTotalsCalculator(), CartMerchantPolicy()
        )
    }

    private class RecordingRepository(private val delegate: CatalogRepository) : CatalogRepository {
        val productRequests = mutableListOf<ProductRequest>()
        override suspend fun getCatalog(request: CatalogRequest): CatalogResult<MerchantCatalog> = delegate.getCatalog(request)
        override suspend fun searchProducts(request: CatalogSearchRequest): CatalogResult<ProductSearchContent> = delegate.searchProducts(request)
        override suspend fun getProduct(request: ProductRequest): CatalogResult<CatalogProduct> {
            productRequests += request
            return delegate.getProduct(request)
        }
    }
}
