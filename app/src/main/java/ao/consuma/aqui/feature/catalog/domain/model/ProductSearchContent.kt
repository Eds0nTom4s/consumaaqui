package ao.consuma.aqui.feature.catalog.domain.model

data class ProductSearchContent(
    val merchantId: String,
    val categories: List<CatalogCategory>,
    val products: List<CatalogProduct>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val hasMore: Boolean
)
