package ao.consuma.aqui.feature.catalog.domain.request

data class CatalogRequest(
    val merchantId: String,
    val forceRefresh: Boolean = false
)

data class ProductRequest(
    val merchantId: String,
    val productId: String,
    val forceRefresh: Boolean = false
)

data class CatalogSearchRequest(
    val merchantId: String,
    val query: String = "",
    val categoryId: String? = null,
    val onlyAvailable: Boolean = false,
    val page: Int = 1,
    val pageSize: Int = CatalogPaging.DEFAULT_PAGE_SIZE,
    val forceRefresh: Boolean = false
)

object CatalogPaging {
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
}
