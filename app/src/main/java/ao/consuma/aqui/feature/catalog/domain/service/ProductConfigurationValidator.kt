package ao.consuma.aqui.feature.catalog.domain.service

import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.ConfiguredProduct
import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.catalog.domain.model.ProductConfiguration
import javax.inject.Inject

sealed interface ProductConfigurationError {
    data object ProductMismatch : ProductConfigurationError
    data object ProductUnavailable : ProductConfigurationError
    data class UnknownGroup(val groupId: String) : ProductConfigurationError
    data class UnknownOption(val groupId: String, val optionId: String) : ProductConfigurationError
    data class UnavailableOption(val groupId: String, val optionId: String) : ProductConfigurationError
    data class MinimumSelectionsNotMet(
        val groupId: String,
        val minimumSelections: Int
    ) : ProductConfigurationError
    data class MaximumSelectionsExceeded(
        val groupId: String,
        val maximumSelections: Int
    ) : ProductConfigurationError
    data class NoAvailableOptions(val groupId: String) : ProductConfigurationError
    data object CurrencyMismatch : ProductConfigurationError
    data object InvalidQuantity : ProductConfigurationError
    data object PriceOverflow : ProductConfigurationError
}

sealed interface ProductConfigurationResult {
    data class Valid(val configuredProduct: ConfiguredProduct) : ProductConfigurationResult
    data class Invalid(val errors: Set<ProductConfigurationError>) : ProductConfigurationResult
}

class ProductConfigurationValidator @Inject constructor() {
    fun validate(
        product: CatalogProduct,
        configuration: ProductConfiguration
    ): ProductConfigurationResult {
        val errors = linkedSetOf<ProductConfigurationError>()
        if (configuration.productId != product.id) errors += ProductConfigurationError.ProductMismatch
        if (product.availability != ProductAvailability.Available) {
            errors += ProductConfigurationError.ProductUnavailable
        }

        val groupsById = product.optionGroups.associateBy { it.id }
        (configuration.selections.keys - groupsById.keys).forEach {
            errors += ProductConfigurationError.UnknownGroup(it)
        }

        product.optionGroups.forEach { group ->
            val selectedIds = configuration.selections[group.id].orEmpty()
            val availableOptions = group.options.filter { it.available }
            if (availableOptions.isEmpty() && group.minimumSelections > 0) {
                errors += ProductConfigurationError.NoAvailableOptions(group.id)
            }
            if (selectedIds.size < group.minimumSelections) {
                errors += ProductConfigurationError.MinimumSelectionsNotMet(
                    group.id,
                    group.minimumSelections
                )
            }
            if (selectedIds.size > group.maximumSelections) {
                errors += ProductConfigurationError.MaximumSelectionsExceeded(
                    group.id,
                    group.maximumSelections
                )
            }
            selectedIds.forEach { optionId ->
                val option = group.options.find { it.id == optionId }
                when {
                    option == null -> errors += ProductConfigurationError.UnknownOption(group.id, optionId)
                    !option.available -> errors += ProductConfigurationError.UnavailableOption(group.id, optionId)
                }
            }
        }

        if (errors.isNotEmpty()) return ProductConfigurationResult.Invalid(errors)

        val selectedPrices = product.optionGroups.flatMap { group ->
            val selectedIds = configuration.selections[group.id].orEmpty()
            group.options.filter { it.id in selectedIds }.mapNotNull { it.additionalPrice }
        }
        return when (val calculation = ProductPriceCalculator.calculate(
            product.basePrice,
            selectedPrices,
            configuration.quantity
        )) {
            PriceCalculationResult.CurrencyMismatch -> ProductConfigurationResult.Invalid(
                setOf(ProductConfigurationError.CurrencyMismatch)
            )
            PriceCalculationResult.InvalidQuantity -> ProductConfigurationResult.Invalid(
                setOf(ProductConfigurationError.InvalidQuantity)
            )
            PriceCalculationResult.Overflow -> ProductConfigurationResult.Invalid(
                setOf(ProductConfigurationError.PriceOverflow)
            )
            is PriceCalculationResult.Success -> ProductConfigurationResult.Valid(
                ConfiguredProduct(
                    merchantId = product.merchantId,
                    productId = product.id,
                    quantity = configuration.quantity,
                    selectedOptionIds = configuration.selections
                        .filterValues { it.isNotEmpty() }
                        .mapValues { it.value.toSet() },
                    note = configuration.note.trim().takeIf(String::isNotEmpty),
                    unitPrice = calculation.prices.unitPrice,
                    optionsPrice = calculation.prices.optionsPrice,
                    totalUnitPrice = calculation.prices.totalUnitPrice,
                    totalPrice = calculation.prices.totalPrice
                )
            )
        }
    }
}
