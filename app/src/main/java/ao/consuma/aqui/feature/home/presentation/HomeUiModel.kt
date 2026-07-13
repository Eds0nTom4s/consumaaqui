package ao.consuma.aqui.feature.home.presentation

import androidx.annotation.StringRes
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic

sealed interface UiText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Dynamic(val value: String) : UiText
}

data class LocationUiModel(val displayName: String)
data class CategoryUiModel(val id: String?, val label: String)

data class MerchantCardUiModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val categoryLabel: String,
    val hasImage: Boolean,
    val distanceText: String?,
    val preparationTimeText: String?,
    val availabilityLabel: UiText,
    val availabilitySemantic: ConsumaStatusSemantic,
    val fulfillmentLabels: List<UiText>,
    val ratingText: String?,
    val minimumOrderText: String?,
    val promotionText: String?,
    val accessibilityDescription: String
)
