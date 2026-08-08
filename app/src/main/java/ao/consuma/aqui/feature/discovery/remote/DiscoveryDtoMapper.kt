package ao.consuma.aqui.feature.discovery.remote

import ao.consuma.aqui.feature.discovery.domain.model.*
import javax.inject.Inject

class DiscoveryDtoMapper @Inject constructor() {
    fun home(dto: DiscoveryHomeDto) = HomeDiscoveryContent(
        categories = emptyList(),
        nearby = section(dto.nearby),
        recommended = section(dto.recommended),
        featured = section(dto.featured)
    )

    fun search(dto: MerchantSearchDto) = MerchantSearchContent(
        categories = emptyList(),
        merchants = dto.merchants.map(::summary),
        page = DiscoveryPagingMapper.toAndroid(dto.page),
        pageSize = dto.pageSize,
        totalCount = dto.totalCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        hasMore = dto.hasMore
    )

    fun overview(dto: MerchantOverviewDto) = MerchantOverview(
        id = dto.merchantId,
        name = dto.name,
        shortDescription = null,
        fullDescription = dto.fullDescription,
        category = null,
        bannerUrl = null,
        logoUrl = null,
        availability = availability(dto.availability),
        fulfillmentOptions = fulfillment(dto.fulfillmentOptions),
        rating = null,
        ratingCount = null,
        address = null,
        contact = null,
        schedule = null,
        promotion = null,
        catalogAvailable = dto.catalogAvailable
    )

    private fun section(dto: MerchantSectionDto) = MerchantSection(dto.items.map(::summary), dto.hasMore)

    private fun summary(dto: MerchantSummaryDto) = MerchantSummary(
        id = dto.merchantId,
        name = dto.name,
        categoryId = null,
        shortDescription = null,
        imageUrl = null,
        availability = availability(dto.availability),
        fulfillmentOptions = fulfillment(dto.fulfillmentOptions),
        distanceMeters = dto.distanceMeters,
        estimatedPreparationMinutes = null,
        rating = null,
        ratingCount = null,
        popularity = dto.popularityScore,
        minimumOrderAmount = null,
        promotion = null,
        isFeatured = dto.featured,
        catalogAvailable = dto.catalogAvailable
    )

    private fun availability(value: String): MerchantAvailability = when (value) {
        "OPEN" -> MerchantAvailability.Open
        "CLOSED" -> MerchantAvailability.Closed
        else -> MerchantAvailability.Unknown
    }

    private fun fulfillment(values: List<String>): Set<FulfillmentOption> =
        values.mapNotNull { runCatching { FulfillmentOption.valueOf(it) }.getOrNull() }.toSet()
}
