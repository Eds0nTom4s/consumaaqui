package ao.consuma.aqui.feature.cart.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount

data class CartItemSelection(
    val groupId: String,
    val groupName: String,
    val optionId: String,
    val optionName: String,
    val additionalPrice: MoneyAmount?
) {
    init {
        require(groupId.isNotBlank()) { "Selection group id is required" }
        require(groupName.isNotBlank()) { "Selection group name is required" }
        require(optionId.isNotBlank()) { "Selection option id is required" }
        require(optionName.isNotBlank()) { "Selection option name is required" }
    }
}

internal val cartSelectionComparator = compareBy<CartItemSelection>(
    CartItemSelection::groupId,
    CartItemSelection::optionId
)
