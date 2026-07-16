package ao.consuma.aqui.feature.catalog.data

import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.catalog.domain.request.CatalogRequest
import ao.consuma.aqui.feature.catalog.domain.request.CatalogSearchRequest
import ao.consuma.aqui.feature.catalog.domain.request.ProductRequest
import ao.consuma.aqui.feature.catalog.domain.result.CatalogDataSource
import ao.consuma.aqui.feature.catalog.domain.result.CatalogError
import ao.consuma.aqui.feature.catalog.domain.result.CatalogResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryCatalogRepositoryTest {
    private lateinit var repository: InMemoryCatalogRepository

    @Before fun setUp() {
        repository = InMemoryCatalogRepository()
    }

    @Test fun `catalog is loaded by merchant id with memory source`() = runBlocking {
        val result = repository.getCatalog(CatalogRequest("sabor-maianga")) as CatalogResult.Success
        assertEquals("sabor-maianga", result.data.merchantId)
        assertEquals(CatalogDataSource.MEMORY, result.source)
        assertTrue(result.data.categories.size >= 4)
    }

    @Test fun `force refresh preserves the stable contract`() = runBlocking {
        val result = repository.getCatalog(
            CatalogRequest("sabor-maianga", forceRefresh = true)
        ) as CatalogResult.Success
        assertEquals("catalog-sabor-maianga", result.data.catalogId)
    }

    @Test fun `unknown merchant and unavailable catalog are distinct`() = runBlocking {
        assertEquals(
            CatalogError.MerchantNotFound,
            (repository.getCatalog(CatalogRequest("missing")) as CatalogResult.Error).reason
        )
        assertEquals(
            CatalogError.CatalogUnavailable,
            (repository.getCatalog(CatalogRequest("servicos-viana")) as CatalogResult.Error).reason
        )
    }

    @Test fun `empty published catalog is not catalog not found`() = runBlocking {
        val result = repository.getCatalog(CatalogRequest("fonte-fresca"))
        assertTrue(result is CatalogResult.Empty)
        assertNotNull((result as CatalogResult.Empty).data)
    }

    @Test fun `product requires matching merchant and product ids`() = runBlocking {
        val productId = "sabor-maianga-product-muamba-casa"
        val success = repository.getProduct(ProductRequest("sabor-maianga", productId))
            as CatalogResult.Success
        assertEquals(productId, success.data.id)
        assertEquals(
            CatalogError.ProductNotFound,
            (repository.getProduct(ProductRequest("doce-embondeiro", productId)) as CatalogResult.Error).reason
        )
    }

    @Test fun `search normalizes name description category and tags`() = runBlocking {
        assertEquals(
            listOf("sabor-maianga-product-sumo-mucua"),
            search("sabor-maianga", query = "  MÚCUA ").map { it.id }
        )
        assertTrue(search("sabor-maianga", query = "demonstrativa").isNotEmpty())
        assertEquals(3, search("sabor-maianga", query = "sobremesas").size)
        assertTrue(search("sabor-maianga", query = "natural").any { it.id.endsWith("sumo-mucua") })
    }

    @Test fun `query combines with category id and unavailable category cannot be selected`() = runBlocking {
        val categoryId = "sabor-maianga-category-bebidas"
        val results = search("sabor-maianga", query = "sumo", categoryId = categoryId)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.categoryId == categoryId })

        val unavailable = repository.searchProducts(
            CatalogSearchRequest(
                merchantId = "mercado-talatona",
                categoryId = "mercado-talatona-category-frescos"
            )
        )
        assertTrue(unavailable is CatalogResult.Empty)
    }

    @Test fun `only available excludes every unavailable state`() = runBlocking {
        val results = search("sabor-maianga", onlyAvailable = true)
        assertTrue(results.all { it.availability == ProductAvailability.Available })
    }

    @Test fun `blank query returns category-organized page with stable order`() = runBlocking {
        val first = search("sabor-maianga")
        val second = search("sabor-maianga")
        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(first.map { it.sortOrder }, first.map { it.sortOrder }.sorted())
    }

    @Test fun `pagination metadata is normalized and coherent`() = runBlocking {
        val result = repository.searchProducts(
            CatalogSearchRequest("sabor-maianga", page = 2, pageSize = 5)
        ) as CatalogResult.Success
        assertEquals(5, result.data.products.size)
        assertEquals(12, result.data.totalCount)
        assertTrue(result.data.hasMore)

        val normalized = repository.searchProducts(
            CatalogSearchRequest("sabor-maianga", page = 0, pageSize = 1_000)
        ) as CatalogResult.Success
        assertEquals(1, normalized.data.page)
        assertEquals(100, normalized.data.pageSize)
        assertFalse(normalized.data.hasMore)
    }

    @Test fun `controlled scenarios are explicit and deterministic`() = runBlocking {
        repository.scenario = MockCatalogScenario.EMPTY
        assertTrue(repository.getCatalog(CatalogRequest("sabor-maianga")) is CatalogResult.Empty)
        assertTrue(repository.getProduct(ProductRequest("sabor-maianga", "anything")) is CatalogResult.Empty)

        repository.scenario = MockCatalogScenario.OFFLINE
        val offline = repository.getCatalog(CatalogRequest("sabor-maianga")) as CatalogResult.Offline
        assertNotNull(offline.cachedData)
        assertEquals(CatalogDataSource.MEMORY, offline.source)

        repository.scenario = MockCatalogScenario.ERROR
        assertEquals(
            CatalogError.Server,
            (repository.getCatalog(CatalogRequest("sabor-maianga")) as CatalogResult.Error).reason
        )

        repository.scenario = MockCatalogScenario.CATALOG_NOT_FOUND
        assertEquals(
            CatalogError.CatalogNotFound,
            (repository.getCatalog(CatalogRequest("sabor-maianga")) as CatalogResult.Error).reason
        )

        repository.scenario = MockCatalogScenario.PRODUCT_NOT_FOUND
        assertEquals(
            CatalogError.ProductNotFound,
            (repository.getProduct(ProductRequest("sabor-maianga", "anything")) as CatalogResult.Error).reason
        )
    }

    private fun search(
        merchantId: String,
        query: String = "",
        categoryId: String? = null,
        onlyAvailable: Boolean = false
    ) = runBlocking {
        val result = repository.searchProducts(
            CatalogSearchRequest(merchantId, query, categoryId, onlyAvailable)
        )
        when (result) {
            is CatalogResult.Success -> result.data.products
            is CatalogResult.Empty -> result.data?.products.orEmpty()
            else -> error("Unexpected result: $result")
        }
    }
}
