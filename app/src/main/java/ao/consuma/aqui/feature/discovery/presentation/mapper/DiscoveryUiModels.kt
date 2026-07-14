package ao.consuma.aqui.feature.discovery.presentation.mapper

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy

sealed interface UiText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val quantity: Int, val args: List<Any> = listOf(quantity)) : UiText
    data class Joined(val values: List<UiText>, val separator: String = " · ") : UiText
    data class Dynamic(val value: String) : UiText
}

data class LocationUiModel(val displayName: String)
data class CategoryUiModel(val id: String?, val label: UiText)

data class MerchantCompactUiModel(
    val id: String,
    val name: String,
    val categoryLabel: String,
    val hasImage: Boolean,
    val availabilityLabel: UiText,
    val availabilitySemantic: ConsumaStatusSemantic,
    val primaryFulfillmentLabel: UiText?,
    val convenienceText: UiText?,
    val promotionText: String?,
    val accessibilityLabel: UiText
)

data class MerchantListItemUiModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val categoryLabel: String,
    val hasImage: Boolean,
    val availabilityLabel: UiText,
    val availabilitySemantic: ConsumaStatusSemantic,
    val fulfillmentText: UiText?,
    val convenienceText: UiText?,
    val promotionText: String?,
    val accessibilityLabel: UiText
)

data class HomeMerchantUiSections(
    val nearby: List<MerchantCompactUiModel>,
    val recommended: List<MerchantCompactUiModel>,
    val featured: List<MerchantCompactUiModel>
)

data class SearchSortOptionUiModel(
    val value: DiscoveryOrderBy,
    val label: UiText,
    val enabled: Boolean
)

data class MerchantOverviewUiModel(
    val id: String,
    val name: String,
    val shortDescription: String?,
    val fullDescription: String?,
    val category: String,
    val hasBanner: Boolean,
    val hasLogo: Boolean,
    val availabilityLabel: UiText,
    val availabilitySemantic: ConsumaStatusSemantic,
    val fulfillmentText: UiText?,
    val contactText: String?,
    val openingHoursText: UiText?,
    val addressText: String?,
    val ratingText: UiText?,
    val promotionText: String?,
    val catalogAvailable: Boolean
)
