package ao.consuma.aqui.feature.home.presentation

import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.feature.home.domain.model.FulfillmentOption
import ao.consuma.aqui.feature.home.domain.model.HomeDiscoveryContent
import ao.consuma.aqui.feature.home.domain.model.MerchantAvailability
import ao.consuma.aqui.feature.home.domain.model.MerchantSummary
import ao.consuma.aqui.feature.home.domain.model.MoneyAmount
import javax.inject.Inject

class HomeUiMapper @Inject constructor() {
    fun categories(content: HomeDiscoveryContent): List<CategoryUiModel> =
        listOf(CategoryUiModel(null, "Todos")) +
            content.categories.map { CategoryUiModel(it.id, it.name) }

    fun merchants(
        content: HomeDiscoveryContent,
        hasLocation: Boolean
    ): Pair<List<MerchantCardUiModel>, List<MerchantCardUiModel>> {
        val categoryNames = content.categories.associate { it.id to it.name }
        val mapMerchant: (MerchantSummary) -> MerchantCardUiModel = { merchant ->
            merchant.toUiModel(categoryNames[merchant.categoryId].orEmpty(), hasLocation)
        }
        return content.nearby.items.map(mapMerchant) to content.featured.items.map(mapMerchant)
    }

    fun distance(meters: Int?): String? = when {
        meters == null -> null
        meters < 1000 -> "$meters m"
        else -> "${meters / 1000},${(meters % 1000) / 100} km"
    }

    fun money(value: MoneyAmount?): String? = value?.let {
        val units = it.amountMinor / 100
        val grouped = units.toString().reversed().chunked(3).joinToString(".").reversed()
        "$grouped Kz"
    }

    private fun MerchantSummary.toUiModel(
        categoryLabel: String,
        hasLocation: Boolean
    ): MerchantCardUiModel {
        val (availabilityLabel, semantic) = when (val value = availability) {
            MerchantAvailability.Open -> UiText.Resource(R.string.home_availability_open) to ConsumaStatusSemantic.SUCCESS
            is MerchantAvailability.ClosingSoon -> UiText.Resource(R.string.home_availability_closing_soon, listOf(value.minutesRemaining)) to ConsumaStatusSemantic.WARNING
            is MerchantAvailability.OpensAt -> UiText.Resource(R.string.home_availability_opens_at, listOf(value.localTime)) to ConsumaStatusSemantic.INFO
            MerchantAvailability.Closed -> UiText.Resource(R.string.home_availability_closed) to ConsumaStatusSemantic.ERROR
            MerchantAvailability.Unknown -> UiText.Resource(R.string.home_availability_unknown) to ConsumaStatusSemantic.NEUTRAL
        }
        val fulfillment = fulfillmentOptions.sortedBy { it.ordinal }.map {
            UiText.Resource(
                when (it) {
                    FulfillmentOption.PICKUP -> R.string.home_fulfillment_pickup
                    FulfillmentOption.DELIVERY -> R.string.home_fulfillment_delivery
                    FulfillmentOption.DINE_IN -> R.string.home_fulfillment_dine_in
                    FulfillmentOption.SERVICE -> R.string.home_fulfillment_service
                }
            )
        }
        val distance = distance(distanceMeters.takeIf { hasLocation })
        val preparation = estimatedPreparationMinutes?.let { "$it min" }
        val rating = rating?.let { value ->
            ratingCount?.let { count -> "${value.toString().replace('.', ',')} ($count)" }
                ?: value.toString().replace('.', ',')
        }
        return MerchantCardUiModel(
            id = id,
            name = name,
            subtitle = shortDescription.orEmpty(),
            categoryLabel = categoryLabel,
            hasImage = imageUrl != null,
            distanceText = distance,
            preparationTimeText = preparation,
            availabilityLabel = availabilityLabel,
            availabilitySemantic = semantic,
            fulfillmentLabels = fulfillment,
            ratingText = rating,
            minimumOrderText = money(minimumOrderAmount),
            promotionText = promotion?.badge ?: promotion?.title,
            accessibilityDescription = listOfNotNull(name, categoryLabel, distance, preparation, rating).joinToString(", ")
        )
    }
}
