package ao.consuma.aqui.feature.discovery.remote

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.discovery.domain.model.*
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

class DiscoveryDtoMapper @Inject constructor() {
    fun home(dto: DiscoveryHomeDto) = HomeDiscoveryContent(
        dto.categories.map(::category),
        section(dto.nearby),
        section(dto.recommended),
        section(dto.featured)
    )

    fun search(dto: MerchantSearchDto) = MerchantSearchContent(
        categories = dto.categories.map(::category),
        merchants = dto.merchants.map(::summary),
        page = DiscoveryPagingMapper.toAndroid(dto.page),
        pageSize = dto.pageSize,
        totalCount = dto.totalCount,
        hasMore = dto.hasMore
    )

    fun overview(dto: MerchantOverviewDto) = MerchantOverview(
        id = dto.id,
        name = dto.name,
        shortDescription = dto.shortDescription,
        fullDescription = dto.fullDescription,
        category = category(dto.category),
        bannerUrl = dto.bannerUrl,
        logoUrl = dto.logoUrl,
        availability = availability(dto.availability),
        fulfillmentOptions = fulfillment(dto.fulfillmentOptions),
        rating = dto.rating?.value,
        ratingCount = dto.rating?.count,
        address = dto.address?.let { MerchantAddress(it.displayName) },
        contact = dto.contact?.let { MerchantContact(it.phone, it.email) },
        schedule = dto.schedule?.let {
            WeeklySchedule(
                openDays = it.openDays.mapNotNull { day -> runCatching { DayOfWeek.valueOf(day) }.getOrNull() }.toSet(),
                opensAt = LocalTime.parse(it.opensAt),
                closesAt = LocalTime.parse(it.closesAt)
            )
        },
        promotion = dto.promotion?.toDomain(),
        catalogAvailable = dto.catalogAvailable
    )

    private fun category(dto: MerchantCategoryDto) = MerchantCategory(dto.id, dto.name)
    private fun section(dto: MerchantSectionDto) = MerchantSection(dto.items.map(::summary), dto.hasMore)

    private fun summary(dto: MerchantSummaryDto) = MerchantSummary(
        id = dto.id,
        name = dto.name,
        categoryId = dto.category.id,
        shortDescription = dto.shortDescription,
        imageUrl = dto.imageUrl,
        availability = availability(dto.availability),
        fulfillmentOptions = fulfillment(dto.fulfillmentOptions),
        distanceMeters = dto.location?.distanceMeters,
        estimatedPreparationMinutes = dto.estimatedPreparationMinutes,
        rating = dto.rating?.value,
        ratingCount = dto.rating?.count,
        popularity = dto.popularity ?: 0,
        minimumOrderAmount = dto.minimumOrderAmount?.let { MoneyAmount(it.minorUnits, it.currency) },
        promotion = dto.promotion?.toDomain(),
        isFeatured = dto.featured == true,
        catalogAvailable = dto.catalogAvailable
    )

    private fun availability(dto: MerchantAvailabilityDto): MerchantAvailability = when (dto.status) {
        "OPEN" -> MerchantAvailability.Open
        "CLOSING_SOON" -> dto.minutesRemaining?.let(MerchantAvailability::ClosingSoon) ?: MerchantAvailability.Unknown
        "OPENS_AT" -> dto.opensAt?.let { runCatching { MerchantAvailability.OpensAt(LocalTime.parse(it)) }.getOrNull() }
            ?: MerchantAvailability.Unknown
        "CLOSED" -> MerchantAvailability.Closed
        else -> MerchantAvailability.Unknown
    }

    private fun fulfillment(values: List<String>): Set<FulfillmentOption> =
        values.mapNotNull { runCatching { FulfillmentOption.valueOf(it) }.getOrNull() }.toSet()

    private fun MerchantPromotionDto.toDomain() = PromotionSummary(id, title, description, badge)
}
