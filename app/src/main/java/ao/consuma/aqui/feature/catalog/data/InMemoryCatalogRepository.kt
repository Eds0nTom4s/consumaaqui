package ao.consuma.aqui.feature.catalog.data

import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.MerchantCatalog
import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.catalog.domain.model.ProductSearchContent
import ao.consuma.aqui.feature.catalog.domain.repository.CatalogRepository
import ao.consuma.aqui.feature.catalog.domain.request.CatalogPaging
import ao.consuma.aqui.feature.catalog.domain.request.CatalogRequest
import ao.consuma.aqui.feature.catalog.domain.request.CatalogSearchRequest
import ao.consuma.aqui.feature.catalog.domain.request.ProductRequest
import ao.consuma.aqui.feature.catalog.domain.result.CatalogDataSource
import ao.consuma.aqui.feature.catalog.domain.result.CatalogError
import ao.consuma.aqui.feature.catalog.domain.result.CatalogResult
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class MockCatalogScenario {
    CONTENT,
    EMPTY,
    ERROR,
    OFFLINE,
    CATALOG_NOT_FOUND,
    PRODUCT_NOT_FOUND
}

@Singleton
class InMemoryCatalogRepository @Inject constructor() : CatalogRepository {
    /** Test-only control; it is not bound or exposed to runtime presentation. */
    internal var scenario: MockCatalogScenario = MockCatalogScenario.CONTENT

    override suspend fun getCatalog(request: CatalogRequest): CatalogResult<MerchantCatalog> {
        merchantError(request.merchantId)?.let { return CatalogResult.Error(it) }
        if (scenario == MockCatalogScenario.ERROR) return CatalogResult.Error(CatalogError.Server)
        if (scenario == MockCatalogScenario.CATALOG_NOT_FOUND) {
            return CatalogResult.Error(CatalogError.CatalogNotFound)
        }
        val catalog = CatalogFixtures.catalog(request.merchantId)
            ?: return CatalogResult.Error(CatalogError.CatalogNotFound)
        if (scenario == MockCatalogScenario.EMPTY) {
            return CatalogResult.Empty(catalog.copy(products = emptyList()))
        }
        return if (scenario == MockCatalogScenario.OFFLINE) {
            CatalogResult.Offline(catalog, CatalogDataSource.MEMORY)
        } else if (catalog.products.isEmpty()) {
            CatalogResult.Empty(catalog)
        } else {
            CatalogResult.Success(catalog, CatalogDataSource.MEMORY)
        }
    }

    override suspend fun getProduct(request: ProductRequest): CatalogResult<CatalogProduct> {
        merchantError(request.merchantId)?.let { return CatalogResult.Error(it) }
        if (scenario == MockCatalogScenario.ERROR) return CatalogResult.Error(CatalogError.Server)
        if (scenario == MockCatalogScenario.CATALOG_NOT_FOUND) {
            return CatalogResult.Error(CatalogError.CatalogNotFound)
        }
        if (scenario == MockCatalogScenario.EMPTY) return CatalogResult.Empty()
        if (scenario == MockCatalogScenario.PRODUCT_NOT_FOUND) {
            return CatalogResult.Error(CatalogError.ProductNotFound)
        }
        val product = CatalogFixtures.product(request.merchantId, request.productId)
            ?: return CatalogResult.Error(CatalogError.ProductNotFound)
        return if (scenario == MockCatalogScenario.OFFLINE) {
            CatalogResult.Offline(product, CatalogDataSource.MEMORY)
        } else {
            CatalogResult.Success(product, CatalogDataSource.MEMORY)
        }
    }

    override suspend fun searchProducts(
        request: CatalogSearchRequest
    ): CatalogResult<ProductSearchContent> {
        merchantError(request.merchantId)?.let { return CatalogResult.Error(it) }
        if (scenario == MockCatalogScenario.ERROR) return CatalogResult.Error(CatalogError.Server)
        if (scenario == MockCatalogScenario.CATALOG_NOT_FOUND) {
            return CatalogResult.Error(CatalogError.CatalogNotFound)
        }
        val catalog = CatalogFixtures.catalog(request.merchantId)
            ?: return CatalogResult.Error(CatalogError.CatalogNotFound)
        val categoriesById = catalog.categories.associateBy { it.id }
        val normalizedQuery = request.query.normalized()
        val selectedCategoryAvailable = request.categoryId == null ||
            categoriesById[request.categoryId]?.available == true
        val filtered = if (scenario == MockCatalogScenario.EMPTY || !selectedCategoryAvailable) {
            emptyList()
        } else {
            catalog.products.filter { product ->
                val category = categoriesById[product.categoryId]
                val searchable = listOf(
                    product.name,
                    product.shortDescription.orEmpty(),
                    product.fullDescription.orEmpty(),
                    category?.name.orEmpty()
                ) + product.tags
                category?.available == true &&
                    (request.categoryId == null || product.categoryId == request.categoryId) &&
                    (!request.onlyAvailable || product.availability == ProductAvailability.Available) &&
                    (normalizedQuery.isEmpty() || searchable.any {
                        it.normalized().contains(normalizedQuery)
                    })
            }.sortedWith(
                compareBy<CatalogProduct> { it.sortOrder }
                    .thenByDescending { it.featured }
                    .thenBy { it.name.normalized() }
                    .thenBy { it.id }
            )
        }
        val page = request.page.coerceAtLeast(1)
        val pageSize = request.pageSize.coerceIn(1, CatalogPaging.MAX_PAGE_SIZE)
        val from = ((page.toLong() - 1L) * pageSize.toLong())
            .coerceAtMost(filtered.size.toLong())
            .toInt()
        val to = (from + pageSize).coerceAtMost(filtered.size)
        val content = ProductSearchContent(
            merchantId = request.merchantId,
            categories = catalog.categories.sortedWith(
                compareBy({ it.sortOrder }, { it.name.normalized() }, { it.id })
            ),
            products = filtered.subList(from, to),
            page = page,
            pageSize = pageSize,
            totalCount = filtered.size,
            hasMore = to < filtered.size
        )
        return when {
            scenario == MockCatalogScenario.OFFLINE -> {
                CatalogResult.Offline(content, CatalogDataSource.MEMORY)
            }
            content.products.isEmpty() -> CatalogResult.Empty(content)
            else -> CatalogResult.Success(content, CatalogDataSource.MEMORY)
        }
    }

    private fun merchantError(merchantId: String): CatalogError? = when {
        merchantId !in CatalogFixtures.knownMerchantIds -> CatalogError.MerchantNotFound
        merchantId in CatalogFixtures.catalogUnavailableMerchantIds -> CatalogError.CatalogUnavailable
        else -> null
    }

    private fun String.normalized(): String = Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(DIACRITICS_REGEX, "")
        .lowercase(Locale.ROOT)

    private companion object {
        val DIACRITICS_REGEX = "\\p{M}+".toRegex()
    }
}
