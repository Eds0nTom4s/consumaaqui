package ao.consuma.aqui.feature.discovery.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MerchantSummaryDto(
    val merchantId: String,
    val name: String,
    val availability: String,
    val fulfillmentOptions: List<String> = emptyList(),
    val distanceMeters: Int? = null,
    val rating: JsonElement? = null,
    val popularityScore: Double? = null,
    val featured: Boolean,
    val catalogAvailable: Boolean
)

@Serializable
data class MerchantSectionDto(
    val items: List<MerchantSummaryDto> = emptyList(),
    val hasMore: Boolean
)

@Serializable
data class DiscoveryHomeDto(
    val categories: List<JsonElement> = emptyList(),
    val nearby: MerchantSectionDto,
    val recommended: MerchantSectionDto,
    val featured: MerchantSectionDto
)

@Serializable
data class MerchantSearchDto(
    val categories: List<JsonElement> = emptyList(),
    val merchants: List<MerchantSummaryDto> = emptyList(),
    val page: Int,
    val pageSize: Int,
    val totalCount: Long,
    val hasMore: Boolean
)

@Serializable
data class MerchantOverviewDto(
    val merchantId: String,
    val name: String,
    val availability: String,
    val fulfillmentOptions: List<String> = emptyList(),
    val distanceMeters: Int? = null,
    val rating: JsonElement? = null,
    val popularityScore: Double? = null,
    val featured: Boolean,
    val catalogAvailable: Boolean,
    val fullDescription: String? = null,
    val weeklySchedule: List<JsonElement>? = null,
    val catalogId: String? = null
)

@Serializable
data class AndroidPublicErrorEnvelopeDto(val error: AndroidPublicErrorDto)

@Serializable
data class AndroidPublicErrorDto(
    val code: String,
    val message: String,
    val retryable: Boolean,
    val fieldErrors: List<JsonElement> = emptyList(),
    val traceId: String
)
