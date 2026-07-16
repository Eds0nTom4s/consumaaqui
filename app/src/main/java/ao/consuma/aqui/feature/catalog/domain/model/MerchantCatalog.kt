package ao.consuma.aqui.feature.catalog.domain.model

import java.time.Instant

/**
 * Public catalog snapshot. Products are stored separately from category metadata
 * and reference categories by id, avoiding duplication and preparing pagination.
 */
data class MerchantCatalog(
    val merchantId: String,
    val catalogId: String,
    val name: String?,
    val description: String?,
    val categories: List<CatalogCategory>,
    val products: List<CatalogProduct>,
    val currencyCode: String,
    val updatedAt: Instant?,
    val version: String?
) {
    init {
        require(merchantId.isNotBlank()) { "Merchant id is required" }
        require(catalogId.isNotBlank()) { "Catalog id is required" }
        require(CURRENCY_CODE.matches(currencyCode)) { "Invalid catalog currency" }
        require(categories.distinctBy(CatalogCategory::id).size == categories.size) {
            "Category ids must be unique"
        }
        require(products.distinctBy(CatalogProduct::id).size == products.size) {
            "Product ids must be unique inside a catalog"
        }
        val categoryIds = categories.map(CatalogCategory::id).toSet()
        require(categories.all { it.merchantId == merchantId }) {
            "Every category must belong to the catalog merchant"
        }
        require(products.all { it.merchantId == merchantId && it.categoryId in categoryIds }) {
            "Every product must belong to the merchant and to a catalog category"
        }
        require(products.all { it.basePrice.currencyCode == currencyCode }) {
            "Every product must use the catalog currency"
        }
    }

    private companion object {
        val CURRENCY_CODE = Regex("[A-Z]{3}")
    }
}
