package ao.consuma.aqui.feature.catalog.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount

data class CatalogProduct(
    val id: String,
    val merchantId: String,
    val categoryId: String,
    val name: String,
    val shortDescription: String?,
    val fullDescription: String?,
    val imageUrl: String?,
    val basePrice: MoneyAmount,
    val compareAtPrice: MoneyAmount?,
    val availability: ProductAvailability,
    val preparationMinutes: Int?,
    val tags: Set<String>,
    val optionGroups: List<ProductOptionGroup>,
    val featured: Boolean,
    val sortOrder: Int
) {
    init {
        require(id.isNotBlank()) { "Product id is required" }
        require(merchantId.isNotBlank()) { "Merchant id is required" }
        require(categoryId.isNotBlank()) { "Category id is required" }
        require(name.isNotBlank()) { "Product name is required" }
        require(preparationMinutes == null || preparationMinutes >= 0) {
            "Preparation time cannot be negative"
        }
        require(tags.none { it.isBlank() }) { "Product tags cannot be blank" }
        require(sortOrder >= 0) { "Product sort order cannot be negative" }
        require(optionGroups.distinctBy(ProductOptionGroup::id).size == optionGroups.size) {
            "Option group ids must be unique inside a product"
        }
        compareAtPrice?.let {
            require(basePrice.hasSameCurrency(it)) { "Compare-at price must use the base currency" }
            require(it.amountMinor > basePrice.amountMinor) {
                "Compare-at price must be greater than the base price"
            }
        }
        require(optionGroups.flatMap(ProductOptionGroup::options).all { option ->
            option.additionalPrice?.let { price ->
                price.currencyCode == basePrice.currencyCode
            } ?: true
        }) { "Option prices must use the product currency" }
    }
}
