package ao.consuma.aqui.feature.discovery.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount
import java.time.DayOfWeek
import java.time.LocalTime

/** API-safe location snapshot. Deliberately independent from Android Location. */
data class DiscoveryLocation(
    val id: String,
    val city: String,
    val area: String,
    val displayName: String
)

data class MerchantCategory(val id: String, val name: String)

sealed interface MerchantAvailability {
    data object Open : MerchantAvailability
    data class ClosingSoon(val minutesRemaining: Int) : MerchantAvailability
    data class OpensAt(val localTime: LocalTime) : MerchantAvailability
    data object Closed : MerchantAvailability
    data object Unknown : MerchantAvailability
}

enum class FulfillmentOption { PICKUP, DELIVERY, DINE_IN, SERVICE }

enum class DiscoveryOrderBy { NEAREST, TOP_RATED, MOST_POPULAR, FEATURED, NAME }

data class PromotionSummary(
    val id: String,
    val title: String,
    val description: String?,
    val badge: String?
)

data class MerchantSummary(
    val id: String,
    val name: String,
    val categoryId: String?,
    val shortDescription: String?,
    val imageUrl: String?,
    val availability: MerchantAvailability,
    val fulfillmentOptions: Set<FulfillmentOption>,
    val distanceMeters: Int?,
    val estimatedPreparationMinutes: Int?,
    val rating: Double?,
    val ratingCount: Int?,
    val popularity: Double?,
    val minimumOrderAmount: MoneyAmount?,
    val promotion: PromotionSummary?,
    val isFeatured: Boolean,
    val catalogAvailable: Boolean
)

data class MerchantSection(val items: List<MerchantSummary>, val hasMore: Boolean)

data class HomeDiscoveryContent(
    val categories: List<MerchantCategory>,
    val nearby: MerchantSection,
    val recommended: MerchantSection,
    val featured: MerchantSection
)

data class MerchantSearchContent(
    val categories: List<MerchantCategory>,
    val merchants: List<MerchantSummary>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val hasMore: Boolean
)

data class MerchantOverview(
    val id: String,
    val name: String,
    val shortDescription: String?,
    val fullDescription: String?,
    val category: MerchantCategory?,
    val bannerUrl: String?,
    val logoUrl: String?,
    val availability: MerchantAvailability,
    val fulfillmentOptions: Set<FulfillmentOption>,
    val rating: Double?,
    val ratingCount: Int?,
    val address: MerchantAddress?,
    val contact: MerchantContact?,
    val schedule: WeeklySchedule?,
    val promotion: PromotionSummary?,
    val catalogAvailable: Boolean
)

data class MerchantAddress(val displayName: String)

data class MerchantContact(
    val phone: String?,
    val email: String?
)

/** Non-localized schedule data, compatible with the current minSdk 26. */
data class WeeklySchedule(
    val openDays: Set<DayOfWeek>,
    val opensAt: LocalTime,
    val closesAt: LocalTime
)
