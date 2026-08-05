package ao.consuma.aqui.feature.discovery.presentation.mapper

import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.discovery.domain.model.*
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

open class DiscoveryUiMapper @Inject constructor() {
    fun categories(categories: List<MerchantCategory>): List<CategoryUiModel> =
        listOf(CategoryUiModel(null, UiText.Resource(R.string.search_filter_all))) +
            categories.map { CategoryUiModel(it.id, UiText.Dynamic(it.name)) }

    fun home(content: HomeDiscoveryContent, hasLocation: Boolean): HomeMerchantUiSections {
        val names = content.categories.associate { it.id to it.name }
        val map: (MerchantSummary) -> MerchantCompactUiModel = { compact(it, names[it.categoryId].orEmpty(), hasLocation) }
        return HomeMerchantUiSections(
            nearby = content.nearby.items.map(map),
            recommended = content.recommended.items.map(map),
            featured = content.featured.items.map(map)
        )
    }

    fun search(content: MerchantSearchContent, hasLocation: Boolean): List<MerchantListItemUiModel> {
        val names = content.categories.associate { it.id to it.name }
        return content.merchants.map { listItem(it, names[it.categoryId].orEmpty(), hasLocation) }
    }

    fun overview(value: MerchantOverview) = MerchantOverviewUiModel(
        id = value.id,
        name = value.name,
        shortDescription = value.shortDescription,
        fullDescription = value.fullDescription,
        category = value.category.name,
        hasBanner = value.bannerUrl != null,
        hasLogo = value.logoUrl != null,
        availabilityLabel = availability(value.availability).first,
        availabilitySemantic = availability(value.availability).second,
        fulfillmentText = fulfillmentSummary(value.fulfillmentOptions),
        contactText = value.contact?.let { listOfNotNull(it.phone, it.email).joinToString(" · ") }?.takeIf { it.isNotBlank() },
        openingHoursText = value.schedule?.toUiText(),
        addressText = value.address?.displayName,
        ratingText = rating(value.rating, value.ratingCount),
        promotionText = value.promotion?.badge ?: value.promotion?.title,
        catalogAvailable = value.catalogAvailable
    )

    fun sortOptions(
        hasLocation: Boolean,
        supportedSorts: Set<DiscoveryOrderBy> = DiscoveryOrderBy.entries.toSet()
    ): List<SearchSortOptionUiModel> = DiscoveryOrderBy.entries.filter { it in supportedSorts }.map {
        SearchSortOptionUiModel(it, sortLabel(it), enabled = hasLocation || it != DiscoveryOrderBy.NEAREST)
    }

    fun resultContext(total: Int, exploration: Boolean): UiText = if (exploration) {
        UiText.Plural(R.plurals.search_exploration_count, total)
    } else {
        UiText.Plural(R.plurals.search_results_count, total)
    }

    fun distance(meters: Int?): UiText? = when {
        meters == null -> null
        meters < 1000 -> UiText.Resource(R.string.merchant_distance_meters, listOf(meters))
        else -> UiText.Resource(
            R.string.merchant_distance_kilometers,
            listOf("${meters / 1000},${(meters % 1000) / 100}")
        )
    }

    fun money(value: MoneyAmount?): UiText? = value?.let {
        val units = it.amountMinor / 100
        val grouped = units.toString().reversed().chunked(3).joinToString(".").reversed()
        UiText.Resource(R.string.merchant_money_kwanza, listOf(grouped))
    }

    private fun compact(value: MerchantSummary, category: String, hasLocation: Boolean): MerchantCompactUiModel {
        val status = availability(value.availability)
        val primary = listOf(FulfillmentOption.DELIVERY, FulfillmentOption.PICKUP, FulfillmentOption.DINE_IN, FulfillmentOption.SERVICE)
            .firstOrNull(value.fulfillmentOptions::contains)
        val fulfillmentText = primary?.let(::fulfillment)
        val convenienceText = joinedOrNull(
            distance(value.distanceMeters.takeIf { hasLocation }),
            value.estimatedPreparationMinutes?.let {
                UiText.Resource(R.string.merchant_preparation_minutes, listOf(it))
            }
        )
        return MerchantCompactUiModel(
            id = value.id,
            name = value.name,
            categoryLabel = category,
            hasImage = value.imageUrl != null,
            availabilityLabel = status.first,
            availabilitySemantic = status.second,
            primaryFulfillmentLabel = fulfillmentText,
            convenienceText = convenienceText,
            promotionText = value.promotion?.badge ?: value.promotion?.title,
            accessibilityLabel = UiText.Joined(listOfNotNull(
                UiText.Resource(R.string.merchant_card_accessibility, listOf(value.name, category)),
                status.first,
                fulfillmentText,
                convenienceText,
                value.promotion?.let { UiText.Dynamic(it.badge ?: it.title) }
            ), ", ")
        )
    }

    private fun listItem(value: MerchantSummary, category: String, hasLocation: Boolean): MerchantListItemUiModel {
        val status = availability(value.availability)
        val fulfillmentText = fulfillmentSummary(value.fulfillmentOptions)
        val rating = rating(value.rating, value.ratingCount)
        val convenienceText = joinedOrNull(
            distance(value.distanceMeters.takeIf { hasLocation }),
            value.estimatedPreparationMinutes?.let {
                UiText.Resource(R.string.merchant_preparation_minutes, listOf(it))
            },
            rating
        )
        return MerchantListItemUiModel(
            id = value.id,
            name = value.name,
            subtitle = value.shortDescription.orEmpty(),
            categoryLabel = category,
            hasImage = value.imageUrl != null,
            availabilityLabel = status.first,
            availabilitySemantic = status.second,
            fulfillmentText = fulfillmentText,
            convenienceText = convenienceText,
            promotionText = value.promotion?.badge ?: value.promotion?.title,
            accessibilityLabel = UiText.Joined(listOfNotNull(
                UiText.Resource(R.string.merchant_card_accessibility, listOf(value.name, category)),
                status.first,
                fulfillmentText,
                convenienceText,
                value.promotion?.let { UiText.Dynamic(it.badge ?: it.title) }
            ), ", ")
        )
    }

    private fun rating(value: Double?, count: Int?): UiText? = value?.let { rating ->
        val localizedRating = rating.toString().replace('.', ',')
        count?.let {
            UiText.Plural(R.plurals.merchant_rating_count, it, listOf(localizedRating, it))
        } ?: UiText.Resource(R.string.merchant_rating_value, listOf(localizedRating))
    }

    private fun joinedOrNull(vararg values: UiText?): UiText? =
        values.filterNotNull().takeIf { it.isNotEmpty() }?.let(UiText::Joined)

    private fun WeeklySchedule.toUiText(): UiText {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
        val times = listOf(opensAt.format(timeFormatter), closesAt.format(timeFormatter))
        return if (openDays == DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }.toSet()) {
            UiText.Resource(R.string.merchant_schedule_monday_saturday, times)
        } else {
            UiText.Plural(R.plurals.merchant_schedule_summary, openDays.size, listOf(openDays.size) + times)
        }
    }

    private fun sortLabel(value: DiscoveryOrderBy) = UiText.Resource(when (value) {
        DiscoveryOrderBy.NEAREST -> R.string.search_order_nearest
        DiscoveryOrderBy.TOP_RATED -> R.string.search_order_rating
        DiscoveryOrderBy.MOST_POPULAR -> R.string.search_order_popular
        DiscoveryOrderBy.FEATURED -> R.string.search_order_featured
        DiscoveryOrderBy.NAME -> R.string.search_order_name
    })

    private fun availability(value: MerchantAvailability): Pair<UiText, ConsumaStatusSemantic> = when (value) {
        MerchantAvailability.Open -> UiText.Resource(R.string.home_availability_open) to ConsumaStatusSemantic.SUCCESS
        is MerchantAvailability.ClosingSoon -> UiText.Resource(R.string.home_availability_closing_soon, listOf(value.minutesRemaining)) to ConsumaStatusSemantic.WARNING
        is MerchantAvailability.OpensAt -> UiText.Resource(
            R.string.home_availability_opens_at,
            listOf(value.localTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)))
        ) to ConsumaStatusSemantic.INFO
        MerchantAvailability.Closed -> UiText.Resource(R.string.home_availability_closed) to ConsumaStatusSemantic.ERROR
        MerchantAvailability.Unknown -> UiText.Resource(R.string.home_availability_unknown) to ConsumaStatusSemantic.NEUTRAL
    }

    private fun fulfillment(value: FulfillmentOption) = UiText.Resource(when (value) {
        FulfillmentOption.PICKUP -> R.string.home_fulfillment_pickup
        FulfillmentOption.DELIVERY -> R.string.home_fulfillment_delivery
        FulfillmentOption.DINE_IN -> R.string.home_fulfillment_dine_in
        FulfillmentOption.SERVICE -> R.string.home_fulfillment_service
    })

    private fun fulfillmentSummary(values: Set<FulfillmentOption>): UiText? = values
        .sortedBy { it.ordinal }
        .map(::fulfillment)
        .takeIf { it.isNotEmpty() }
        ?.let(UiText::Joined)
}
