package ao.consuma.aqui.feature.catalog.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount

data class ProductOption(
    val id: String,
    val name: String,
    val description: String?,
    val additionalPrice: MoneyAmount?,
    val available: Boolean,
    val sortOrder: Int,
    val defaultSelected: Boolean
) {
    init {
        require(id.isNotBlank()) { "Option id is required" }
        require(name.isNotBlank()) { "Option name is required" }
        require(sortOrder >= 0) { "Option sort order cannot be negative" }
        require(!defaultSelected || available) { "An unavailable option cannot be selected by default" }
    }
}
