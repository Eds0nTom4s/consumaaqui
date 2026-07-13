package ao.consuma.aqui.feature.home.domain.model

data class MoneyAmount(val amountMinor: Long, val currencyCode: String)

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
    data class OpensAt(val localTime: String) : MerchantAvailability
    data object Closed : MerchantAvailability
    data object Unknown : MerchantAvailability
}

enum class FulfillmentOption { PICKUP, DELIVERY, DINE_IN, SERVICE }

data class PromotionSummary(
    val id: String,
    val title: String,
    val description: String?,
    val badge: String?
)

data class MerchantSummary(
    val id: String,
    val name: String,
    val categoryId: String,
    val shortDescription: String?,
    val imageUrl: String?,
    val availability: MerchantAvailability,
    val fulfillmentOptions: Set<FulfillmentOption>,
    val distanceMeters: Int?,
    val estimatedPreparationMinutes: Int?,
    val rating: Double?,
    val ratingCount: Int?,
    val minimumOrderAmount: MoneyAmount?,
    val promotion: PromotionSummary?,
    val isFeatured: Boolean
)

data class MerchantSection(
    val items: List<MerchantSummary>,
    val hasMore: Boolean
)

data class HomeDiscoveryContent(
    val categories: List<MerchantCategory>,
    val nearby: MerchantSection,
    val featured: MerchantSection
)
