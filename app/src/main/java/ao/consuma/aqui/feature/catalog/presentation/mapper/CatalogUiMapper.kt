package ao.consuma.aqui.feature.catalog.presentation.mapper

import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.catalog.domain.model.CatalogCategory
import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.MerchantCatalog
import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.catalog.domain.model.ProductOptionGroup
import ao.consuma.aqui.feature.catalog.domain.service.ProductConfigurationError
import ao.consuma.aqui.feature.catalog.domain.service.ProductPriceBreakdown
import javax.inject.Inject

class CatalogUiMapper @Inject constructor() {
    fun catalog(
        value: MerchantCatalog,
        products: List<CatalogProduct>,
        query: String,
        selectedCategoryId: String?,
        refreshing: Boolean,
        offline: Boolean
    ): CatalogContentUiModel {
        val categoryNames = value.categories.associate { it.id to it.name }
        return CatalogContentUiModel(
            merchantId = value.merchantId,
            title = value.name?.takeIf(String::isNotBlank) ?: value.merchantId,
            description = value.description,
            query = query,
            categories = categories(value.categories),
            selectedCategoryId = selectedCategoryId,
            products = products.map { product(it, categoryNames[it.categoryId].orEmpty()) },
            isSearchMode = query.trim().isNotEmpty(),
            isRefreshing = refreshing,
            isOffline = offline,
            resultContext = CatalogUiText.Plural(R.plurals.catalog_results_count, products.size)
        )
    }

    fun categories(values: List<CatalogCategory>): List<CatalogCategoryUiModel> =
        listOf(CatalogCategoryUiModel(null, CatalogUiText.Resource(R.string.catalog_category_all), true)) +
            values.sortedWith(compareBy(CatalogCategory::sortOrder, CatalogCategory::name))
                .map { CatalogCategoryUiModel(it.id, CatalogUiText.Dynamic(it.name), it.available) }

    fun product(value: CatalogProduct, categoryName: String): CatalogProductCardUiModel {
        val status = availability(value.availability)
        return CatalogProductCardUiModel(
            id = value.id,
            merchantId = value.merchantId,
            name = value.name,
            description = value.shortDescription,
            hasImage = value.imageUrl != null,
            priceText = money(value.basePrice),
            compareAtPriceText = value.compareAtPrice?.let(::money),
            availabilityLabel = status.first,
            availabilitySemantic = status.second,
            preparationText = value.preparationMinutes?.let {
                CatalogUiText.Resource(R.string.catalog_preparation_minutes, listOf(it))
            },
            hasOptions = value.optionGroups.isNotEmpty(),
            featured = value.featured,
            categoryName = categoryName,
            accessibilityDescription = CatalogUiText.Joined(
                listOfNotNull(
                    CatalogUiText.Resource(
                        R.string.catalog_product_accessibility,
                        listOf(value.name, money(value.basePrice), categoryName)
                    ),
                    status.first,
                    value.preparationMinutes?.let {
                        CatalogUiText.Resource(R.string.catalog_preparation_minutes, listOf(it))
                    }
                )
            )
        )
    }

    fun productDetail(value: CatalogProduct): ProductDetailUiModel {
        val status = availability(value.availability)
        return ProductDetailUiModel(
            id = value.id,
            merchantId = value.merchantId,
            name = value.name,
            fullDescription = value.fullDescription ?: value.shortDescription,
            hasImage = value.imageUrl != null,
            priceText = money(value.basePrice),
            compareAtPriceText = value.compareAtPrice?.let(::money),
            availabilityLabel = status.first,
            availabilitySemantic = status.second,
            preparationText = value.preparationMinutes?.let {
                CatalogUiText.Resource(R.string.catalog_preparation_minutes, listOf(it))
            },
            optionGroups = value.optionGroups.sortedBy(ProductOptionGroup::sortOrder).map(::optionGroup),
            canConfigure = value.availability == ProductAvailability.Available
        )
    }

    fun priceSummary(value: ProductPriceBreakdown) = ProductPriceSummaryUiModel(
        unitPriceText = money(value.unitPrice),
        optionsPriceText = money(value.optionsPrice),
        totalPriceText = money(value.totalPrice)
    )

    fun configurationErrors(errors: Set<ProductConfigurationError>): List<ProductConfigurationErrorUiModel> =
        errors.map { error ->
            val groupId = when (error) {
                is ProductConfigurationError.UnknownGroup -> error.groupId
                is ProductConfigurationError.UnknownOption -> error.groupId
                is ProductConfigurationError.UnavailableOption -> error.groupId
                is ProductConfigurationError.MinimumSelectionsNotMet -> error.groupId
                is ProductConfigurationError.MaximumSelectionsExceeded -> error.groupId
                is ProductConfigurationError.NoAvailableOptions -> error.groupId
                else -> null
            }
            val message = when (error) {
                is ProductConfigurationError.MinimumSelectionsNotMet -> CatalogUiText.Plural(
                    R.plurals.product_error_minimum,
                    error.minimumSelections,
                    listOf(error.minimumSelections)
                )
                is ProductConfigurationError.MaximumSelectionsExceeded -> CatalogUiText.Plural(
                    R.plurals.product_error_maximum,
                    error.maximumSelections,
                    listOf(error.maximumSelections)
                )
                is ProductConfigurationError.UnavailableOption -> CatalogUiText.Resource(R.string.product_error_option_unavailable)
                is ProductConfigurationError.UnknownOption,
                is ProductConfigurationError.UnknownGroup -> CatalogUiText.Resource(R.string.product_error_invalid_option)
                is ProductConfigurationError.NoAvailableOptions -> CatalogUiText.Resource(R.string.product_error_no_options)
                ProductConfigurationError.ProductUnavailable -> CatalogUiText.Resource(R.string.product_error_unavailable)
                ProductConfigurationError.ProductMismatch -> CatalogUiText.Resource(R.string.product_error_invalid_product)
                ProductConfigurationError.CurrencyMismatch,
                ProductConfigurationError.PriceOverflow -> CatalogUiText.Resource(R.string.product_error_price)
                ProductConfigurationError.InvalidQuantity -> CatalogUiText.Resource(R.string.product_error_quantity)
            }
            ProductConfigurationErrorUiModel(groupId, message)
        }

    fun money(value: MoneyAmount): String {
        val whole = value.amountMinor / 100
        val fraction = value.amountMinor % 100
        val grouped = whole.toString().reversed().chunked(3).joinToString(".").reversed()
        val amount = if (fraction == 0L) grouped else "$grouped,${fraction.toString().padStart(2, '0')}"
        return "$amount ${if (value.currencyCode == "AOA") "Kz" else value.currencyCode}"
    }

    private fun optionGroup(value: ProductOptionGroup) = ProductOptionGroupUiModel(
        id = value.id,
        name = value.name,
        description = value.description,
        required = value.required,
        minimumSelections = value.minimumSelections,
        maximumSelections = value.maximumSelections,
        singleChoice = value.selectionRule.singleChoice,
        ruleLabel = when {
            value.minimumSelections == 1 && value.maximumSelections == 1 -> CatalogUiText.Resource(R.string.product_rule_choose_one)
            value.minimumSelections == 0 -> CatalogUiText.Resource(R.string.product_rule_choose_up_to, listOf(value.maximumSelections))
            else -> CatalogUiText.Resource(R.string.product_rule_choose_range, listOf(value.minimumSelections, value.maximumSelections))
        },
        options = value.options.sortedBy { it.sortOrder }.map {
            ProductOptionUiModel(
                id = it.id,
                name = it.name,
                description = it.description,
                additionalPriceText = it.additionalPrice?.takeIf { price -> price.amountMinor > 0 }?.let { price -> "+ ${money(price)}" },
                available = it.available,
                defaultSelected = it.defaultSelected
            )
        }
    )

    private fun availability(value: ProductAvailability): Pair<CatalogUiText, ConsumaStatusSemantic> = when (value) {
        ProductAvailability.Available -> CatalogUiText.Resource(R.string.product_available) to ConsumaStatusSemantic.SUCCESS
        ProductAvailability.Unavailable -> CatalogUiText.Resource(R.string.product_unavailable) to ConsumaStatusSemantic.ERROR
        ProductAvailability.TemporarilyUnavailable -> CatalogUiText.Resource(R.string.product_temporarily_unavailable) to ConsumaStatusSemantic.WARNING
        is ProductAvailability.AvailableFrom -> CatalogUiText.Resource(R.string.product_available_from, listOf(value.time.toString())) to ConsumaStatusSemantic.INFO
        ProductAvailability.OutOfStock -> CatalogUiText.Resource(R.string.product_out_of_stock) to ConsumaStatusSemantic.ERROR
        ProductAvailability.Unknown -> CatalogUiText.Resource(R.string.product_availability_unknown) to ConsumaStatusSemantic.NEUTRAL
    }
}
