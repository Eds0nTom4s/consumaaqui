package ao.consuma.aqui.feature.catalog.domain.repository

import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.MerchantCatalog
import ao.consuma.aqui.feature.catalog.domain.model.ProductSearchContent
import ao.consuma.aqui.feature.catalog.domain.request.CatalogRequest
import ao.consuma.aqui.feature.catalog.domain.request.CatalogSearchRequest
import ao.consuma.aqui.feature.catalog.domain.request.ProductRequest
import ao.consuma.aqui.feature.catalog.domain.result.CatalogResult

/**
 * Catalog search is explicit because production catalogs may require independent
 * pagination, caching and a backend endpoint instead of downloading all products.
 */
interface CatalogRepository {
    suspend fun getCatalog(request: CatalogRequest): CatalogResult<MerchantCatalog>
    suspend fun getProduct(request: ProductRequest): CatalogResult<CatalogProduct>
    suspend fun searchProducts(request: CatalogSearchRequest): CatalogResult<ProductSearchContent>
}
