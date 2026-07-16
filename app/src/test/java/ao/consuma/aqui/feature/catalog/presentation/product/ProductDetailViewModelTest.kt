package ao.consuma.aqui.feature.catalog.presentation.product

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {
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
        val invalid = ProductDetailViewModel(
            InMemoryCatalogRepository(), ProductConfigurationValidator(), CatalogUiMapper(),
            SavedStateHandle(), UnconfinedTestDispatcher()
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
        var configured: ao.consuma.aqui.feature.catalog.domain.model.ConfiguredProduct? = null
        viewModel.onEvent(ProductDetailUiEvent.Add) { configured = it }
        val content = (viewModel.uiState.value as ProductDetailUiState.Content).data
        assertNull(configured)
        assertTrue(content.validationErrors.any { it.groupId == "muamba-casa-portion" })
        assertEquals(1, content.quantity)
    }

    @Test fun `valid add emits configured product without persistence`() {
        val viewModel = viewModel(InMemoryCatalogRepository())
        viewModel.onEvent(ProductDetailUiEvent.Load)
        viewModel.onEvent(ProductDetailUiEvent.QuantityIncreased)
        viewModel.onEvent(ProductDetailUiEvent.NoteChanged("  sem talheres  "))
        var configured: ao.consuma.aqui.feature.catalog.domain.model.ConfiguredProduct? = null
        viewModel.onEvent(ProductDetailUiEvent.Add) { configured = it }
        assertEquals(2, configured?.quantity)
        assertEquals("sem talheres", configured?.note)
        assertEquals(900_000L, configured?.totalPrice?.amountMinor)
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
        state: SavedStateHandle = SavedStateHandle(mapOf("merchantId" to "sabor-maianga", "productId" to productId))
    ) = ProductDetailViewModel(
        repository,
        ProductConfigurationValidator(),
        CatalogUiMapper(),
        state,
        UnconfinedTestDispatcher()
    )

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
