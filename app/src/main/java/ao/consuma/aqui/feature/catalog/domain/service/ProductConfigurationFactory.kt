package ao.consuma.aqui.feature.catalog.domain.service

import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.ProductConfiguration

object ProductConfigurationFactory {
    fun create(product: CatalogProduct): ProductConfiguration = ProductConfiguration(
        productId = product.id,
        selections = product.optionGroups.mapNotNull { group ->
            group.options
                .filter { it.available && it.defaultSelected }
                .map { it.id }
                .toSet()
                .takeIf { it.isNotEmpty() }
                ?.let { group.id to it }
        }.toMap(),
        quantity = ProductConfiguration.MIN_QUANTITY,
        note = ""
    )
}
