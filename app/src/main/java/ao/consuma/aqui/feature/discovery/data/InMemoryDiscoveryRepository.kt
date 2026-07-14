package ao.consuma.aqui.feature.discovery.data

import ao.consuma.aqui.feature.discovery.domain.model.*
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.request.*
import ao.consuma.aqui.feature.discovery.domain.result.*
import javax.inject.Inject
import javax.inject.Singleton
import java.text.Normalizer
import java.util.Locale

enum class MockDiscoveryScenario { CONTENT, EMPTY, ERROR, OFFLINE }

interface MockDiscoveryScenarioController { var scenario: MockDiscoveryScenario }

@Singleton
class InMemoryDiscoveryRepository @Inject constructor() :
    DiscoveryRepository,
    MockDiscoveryScenarioController {

    override var scenario: MockDiscoveryScenario = MockDiscoveryScenario.CONTENT

    override suspend fun home(request: HomeDiscoveryRequest): DiscoveryResult<HomeDiscoveryContent> {
        val items = filtered(
            query = request.query.orEmpty(),
            categoryId = request.selectedCategoryId,
            locationAvailable = request.location != null
        )
        return outcome(DiscoveryFixtures.home(items, request.location != null)) {
            it.nearby.items.isEmpty() && it.recommended.items.isEmpty() && it.featured.items.isEmpty()
        }
    }

    override suspend fun search(request: DiscoverySearchRequest): DiscoveryResult<MerchantSearchContent> {
        val filtered = filtered(
            query = request.query,
            categoryId = request.categoryId,
            onlyOpen = request.onlyOpen,
            fulfillmentOptions = request.fulfillmentOptions,
            locationAvailable = request.location != null
        ).sorted(request.orderBy)
        val page = request.page.coerceAtLeast(1)
        val pageSize = request.pageSize.coerceIn(1, DiscoveryPaging.MAX_PAGE_SIZE)
        val from = ((page - 1) * pageSize).coerceAtMost(filtered.size)
        val to = (from + pageSize).coerceAtMost(filtered.size)
        val content = MerchantSearchContent(
            categories = DiscoveryFixtures.categories,
            merchants = filtered.subList(from, to),
            page = page,
            pageSize = pageSize,
            totalCount = filtered.size,
            hasMore = to < filtered.size
        )
        return outcome(content) { it.merchants.isEmpty() }
    }

    override suspend fun merchant(request: MerchantRequest): DiscoveryResult<MerchantOverview> {
        if (scenario == MockDiscoveryScenario.ERROR) return DiscoveryResult.Error(DiscoveryError.Server)
        if (scenario == MockDiscoveryScenario.EMPTY) return DiscoveryResult.Error(DiscoveryError.NotFound)
        val summary = DiscoveryFixtures.merchants.find { it.id == request.merchantId }
            ?: return DiscoveryResult.Error(DiscoveryError.NotFound)
        val overview = DiscoveryFixtures.overview(summary)
        return if (scenario == MockDiscoveryScenario.OFFLINE) {
            DiscoveryResult.Offline(overview)
        } else {
            DiscoveryResult.Success(overview, DataSource.MEMORY)
        }
    }

    private fun filtered(
        query: String,
        categoryId: String?,
        onlyOpen: Boolean = false,
        fulfillmentOptions: Set<FulfillmentOption> = emptySet(),
        locationAvailable: Boolean
    ): List<MerchantSummary> {
        if (scenario == MockDiscoveryScenario.EMPTY) return emptyList()
        val normalizedQuery = query.normalized()
        val categoryNames = DiscoveryFixtures.categories.associate { it.id to it.name }
        return DiscoveryFixtures.merchants.filter { merchant ->
            val searchable = listOf(
                merchant.name,
                merchant.shortDescription.orEmpty(),
                categoryNames[merchant.categoryId].orEmpty()
            )
            (categoryId == null || merchant.categoryId == categoryId) &&
                (!onlyOpen || merchant.availability is MerchantAvailability.Open || merchant.availability is MerchantAvailability.ClosingSoon) &&
                (fulfillmentOptions.isEmpty() || merchant.fulfillmentOptions.any(fulfillmentOptions::contains)) &&
                (normalizedQuery.isBlank() || searchable.any { it.normalized().contains(normalizedQuery) })
        }.map { if (locationAvailable) it else it.copy(distanceMeters = null) }
    }

    private fun List<MerchantSummary>.sorted(orderBy: DiscoveryOrderBy): List<MerchantSummary> =
        when (orderBy) {
            DiscoveryOrderBy.NEAREST -> sortedWith(
                compareBy<MerchantSummary> { it.distanceMeters == null }
                    .thenBy { it.distanceMeters ?: Int.MAX_VALUE }
                    .thenBy { it.id }
            )
            DiscoveryOrderBy.TOP_RATED -> sortedWith(
                compareByDescending<MerchantSummary> { it.rating ?: -1.0 }
                    .thenByDescending { it.ratingCount ?: 0 }
                    .thenBy { it.id }
            )
            DiscoveryOrderBy.MOST_POPULAR -> sortedWith(
                compareByDescending<MerchantSummary> { it.popularity }.thenBy { it.id }
            )
            DiscoveryOrderBy.FEATURED -> sortedWith(
                compareByDescending<MerchantSummary> { it.isFeatured }
                    .thenByDescending { it.popularity }
                    .thenBy { it.id }
            )
            DiscoveryOrderBy.NAME -> sortedWith(
                compareBy<MerchantSummary> { it.name.normalized() }.thenBy { it.id }
            )
        }

    private fun <T> outcome(content: T, empty: (T) -> Boolean): DiscoveryResult<T> = when (scenario) {
        MockDiscoveryScenario.ERROR -> DiscoveryResult.Error(DiscoveryError.Server)
        MockDiscoveryScenario.EMPTY -> DiscoveryResult.Empty(content)
        MockDiscoveryScenario.OFFLINE -> DiscoveryResult.Offline(content)
        MockDiscoveryScenario.CONTENT -> if (empty(content)) DiscoveryResult.Empty(content) else DiscoveryResult.Success(content, DataSource.MEMORY)
    }

    private fun String.normalized(): String = Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(DIACRITICS_REGEX, "")
        .lowercase(Locale.ROOT)

    private companion object {
        val DIACRITICS_REGEX = "\\p{M}+".toRegex()
    }
}
