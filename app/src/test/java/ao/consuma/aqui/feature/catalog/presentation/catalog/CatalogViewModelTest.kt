package ao.consuma.aqui.feature.catalog.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import ao.consuma.aqui.feature.catalog.data.CatalogFixtures
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
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    @Test fun `initial state is loading and load returns content`() {
        val viewModel = viewModel(InMemoryCatalogRepository())
        assertEquals(CatalogUiState.Loading, viewModel.uiState.value)
        viewModel.onEvent(CatalogUiEvent.Load)
        val state = viewModel.uiState.value as CatalogUiState.Content
        assertEquals("sabor-maianga", state.data.merchantId)
        assertEquals(12, state.data.products.size)
    }

    @Test fun `invalid merchant catalog not found and unavailable are distinct`() {
        val invalid = CatalogViewModel(
            InMemoryCatalogRepository(), CatalogUiMapper(), SavedStateHandle(), UnconfinedTestDispatcher()
        )
        invalid.onEvent(CatalogUiEvent.Load)
        assertEquals(CatalogUiState.InvalidMerchant, invalid.uiState.value)

        val missing = viewModel(InMemoryCatalogRepository(), "missing")
        missing.onEvent(CatalogUiEvent.Load)
        assertEquals(CatalogUiState.CatalogNotFound, missing.uiState.value)

        val unavailable = viewModel(InMemoryCatalogRepository(), "servicos-viana")
        unavailable.onEvent(CatalogUiEvent.Load)
        assertEquals(CatalogEmptyReason.CATALOG_UNAVAILABLE, (unavailable.uiState.value as CatalogUiState.Empty).reason)
    }

    @Test fun `published empty offline and error use dedicated states`() {
        val empty = viewModel(InMemoryCatalogRepository(), "fonte-fresca")
        empty.onEvent(CatalogUiEvent.Load)
        assertEquals(CatalogEmptyReason.CATALOG_EMPTY, (empty.uiState.value as CatalogUiState.Empty).reason)

        val repository = InMemoryCatalogRepository().apply { scenario = MockCatalogScenario.OFFLINE }
        val offline = viewModel(repository)
        offline.onEvent(CatalogUiEvent.Load)
        assertTrue((offline.uiState.value as CatalogUiState.Content).data.isOffline)

        repository.scenario = MockCatalogScenario.ERROR
        offline.onEvent(CatalogUiEvent.Retry)
        assertTrue(offline.uiState.value is CatalogUiState.Error)
    }

    @Test fun `query and category are combined and empty reasons are specific`() {
        val viewModel = viewModel(InMemoryCatalogRepository())
        viewModel.onEvent(CatalogUiEvent.Load)
        viewModel.onEvent(CatalogUiEvent.CategorySelected("sabor-maianga-category-bebidas"))
        var state = viewModel.uiState.value as CatalogUiState.Content
        assertTrue(state.data.products.all { it.categoryName == "Bebidas" })

        viewModel.onEvent(CatalogUiEvent.QueryChanged("não existe"))
        val empty = viewModel.uiState.value as CatalogUiState.Empty
        assertEquals(CatalogEmptyReason.SEARCH_EMPTY, empty.reason)
        viewModel.onEvent(CatalogUiEvent.ClearQuery)
        state = viewModel.uiState.value as CatalogUiState.Content
        assertEquals("sabor-maianga-category-bebidas", state.data.selectedCategoryId)
    }

    @Test fun `refresh sends force refresh and product selection emits ids only`() {
        val repository = RecordingRepository(InMemoryCatalogRepository())
        val viewModel = viewModel(repository)
        viewModel.onEvent(CatalogUiEvent.Load)
        viewModel.onEvent(CatalogUiEvent.Refresh)
        assertTrue(repository.catalogRequests.last().forceRefresh)
        assertTrue(repository.searchRequests.last().forceRefresh)
        assertFalse((viewModel.uiState.value as CatalogUiState.Content).data.isRefreshing)

        var selected: Pair<String, String>? = null
        viewModel.onEvent(CatalogUiEvent.ProductSelected("product")) { merchant, product ->
            selected = merchant to product
        }
        assertEquals("sabor-maianga" to "product", selected)
    }

    @Test fun `query and valid category survive recreation while invalid category is discarded`() {
        val state = SavedStateHandle(mapOf("merchantId" to "sabor-maianga"))
        val first = viewModel(InMemoryCatalogRepository(), state = state)
        first.onEvent(CatalogUiEvent.Load)
        first.onEvent(CatalogUiEvent.QueryChanged("sumo"))
        first.onEvent(CatalogUiEvent.CategorySelected("sabor-maianga-category-bebidas"))

        val restored = viewModel(InMemoryCatalogRepository(), state = state)
        restored.onEvent(CatalogUiEvent.Load)
        val content = restored.uiState.value as CatalogUiState.Content
        assertEquals("sumo", content.data.query)
        assertEquals("sabor-maianga-category-bebidas", content.data.selectedCategoryId)

        state[CatalogSavedStateKeys.CATEGORY_ID] = "invalid"
        val corrected = viewModel(InMemoryCatalogRepository(), state = state)
        corrected.onEvent(CatalogUiEvent.Load)
        assertEquals(null, (corrected.uiState.value as CatalogUiState.Content).data.selectedCategoryId)
    }

    @Test fun `refresh preserves query and category and does not duplicate products`() {
        val viewModel = viewModel(InMemoryCatalogRepository())
        viewModel.onEvent(CatalogUiEvent.Load)
        viewModel.onEvent(CatalogUiEvent.QueryChanged("sumo"))
        viewModel.onEvent(CatalogUiEvent.CategorySelected("sabor-maianga-category-bebidas"))
        viewModel.onEvent(CatalogUiEvent.Refresh)
        val data = (viewModel.uiState.value as CatalogUiState.Content).data
        assertEquals("sumo", data.query)
        assertEquals("sabor-maianga-category-bebidas", data.selectedCategoryId)
        assertEquals(data.products.size, data.products.map { it.id }.distinct().size)
    }

    @Test fun `blank product id does not navigate`() {
        val viewModel = viewModel(InMemoryCatalogRepository())
        var navigations = 0
        viewModel.onEvent(CatalogUiEvent.ProductSelected(" ")) { _, _ -> navigations++ }
        assertEquals(0, navigations)
    }

    private fun viewModel(
        repository: CatalogRepository,
        merchantId: String = "sabor-maianga",
        state: SavedStateHandle = SavedStateHandle(mapOf("merchantId" to merchantId))
    ) = CatalogViewModel(repository, CatalogUiMapper(), state, UnconfinedTestDispatcher())

    private class RecordingRepository(private val delegate: CatalogRepository) : CatalogRepository {
        val catalogRequests = mutableListOf<CatalogRequest>()
        val searchRequests = mutableListOf<CatalogSearchRequest>()
        override suspend fun getCatalog(request: CatalogRequest): CatalogResult<MerchantCatalog> {
            catalogRequests += request
            return delegate.getCatalog(request)
        }
        override suspend fun getProduct(request: ProductRequest): CatalogResult<CatalogProduct> = delegate.getProduct(request)
        override suspend fun searchProducts(request: CatalogSearchRequest): CatalogResult<ProductSearchContent> {
            searchRequests += request
            return delegate.searchProducts(request)
        }
    }
}
