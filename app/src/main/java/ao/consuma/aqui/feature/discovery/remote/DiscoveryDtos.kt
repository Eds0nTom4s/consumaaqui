package ao.consuma.aqui.feature.discovery.remote

import kotlinx.serialization.Serializable

@Serializable data class MerchantCategoryDto(val id: String, val name: String)
@Serializable data class MerchantAvailabilityDto(
    val status: String,
    val minutesRemaining: Int? = null,
    val opensAt: String? = null
)
@Serializable data class MerchantLocationDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val municipalityId: String? = null,
    val distanceMeters: Int? = null
)
@Serializable data class MerchantPromotionDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val badge: String? = null
)
@Serializable data class MerchantRatingDto(val value: Double, val count: Int? = null)
@Serializable data class MoneyAmountDto(val minorUnits: Long, val currency: String)
@Serializable data class MerchantSummaryDto(
    val id: String,
    val name: String,
    val category: MerchantCategoryDto,
    val shortDescription: String? = null,
    val imageUrl: String? = null,
    val availability: MerchantAvailabilityDto,
    val fulfillmentOptions: List<String>,
    val location: MerchantLocationDto? = null,
    val estimatedPreparationMinutes: Int? = null,
    val rating: MerchantRatingDto? = null,
    val popularity: Int? = null,
    val minimumOrderAmount: MoneyAmountDto? = null,
    val promotion: MerchantPromotionDto? = null,
    val featured: Boolean? = null,
    val catalogAvailable: Boolean
)
@Serializable data class MerchantSectionDto(val items: List<MerchantSummaryDto>, val hasMore: Boolean)
@Serializable data class DiscoveryHomeDto(
    val categories: List<MerchantCategoryDto>,
    val nearby: MerchantSectionDto,
    val recommended: MerchantSectionDto,
    val featured: MerchantSectionDto
)
@Serializable data class MerchantSearchDto(
    val categories: List<MerchantCategoryDto>,
    val merchants: List<MerchantSummaryDto>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val hasMore: Boolean
)
@Serializable data class MerchantAddressDto(val displayName: String)
@Serializable data class MerchantContactDto(val phone: String? = null, val email: String? = null)
@Serializable data class WeeklyScheduleDto(
    val openDays: List<String>,
    val opensAt: String,
    val closesAt: String
)
@Serializable data class MerchantOverviewDto(
    val id: String,
    val name: String,
    val shortDescription: String? = null,
    val fullDescription: String? = null,
    val category: MerchantCategoryDto,
    val bannerUrl: String? = null,
    val logoUrl: String? = null,
    val availability: MerchantAvailabilityDto,
    val fulfillmentOptions: List<String>,
    val rating: MerchantRatingDto? = null,
    val address: MerchantAddressDto? = null,
    val contact: MerchantContactDto? = null,
    val schedule: WeeklyScheduleDto? = null,
    val promotion: MerchantPromotionDto? = null,
    val catalogAvailable: Boolean
)
@Serializable data class DiscoveryErrorDto(
    val timestamp: String? = null,
    val status: Int? = null,
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
    val path: String? = null
)
