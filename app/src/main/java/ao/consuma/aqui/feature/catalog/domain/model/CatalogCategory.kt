package ao.consuma.aqui.feature.catalog.domain.model

data class CatalogCategory(
    val id: String,
    val merchantId: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val sortOrder: Int,
    val available: Boolean
) {
    init {
        require(id.isNotBlank()) { "Category id is required" }
        require(merchantId.isNotBlank()) { "Merchant id is required" }
        require(name.isNotBlank()) { "Category name is required" }
        require(sortOrder >= 0) { "Category sort order cannot be negative" }
    }
}
