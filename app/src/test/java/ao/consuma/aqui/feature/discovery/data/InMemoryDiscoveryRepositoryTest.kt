package ao.consuma.aqui.feature.discovery.data

import ao.consuma.aqui.feature.discovery.domain.model.*
import ao.consuma.aqui.feature.discovery.domain.request.*
import ao.consuma.aqui.feature.discovery.domain.result.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InMemoryDiscoveryRepositoryTest {
    private lateinit var repository: InMemoryDiscoveryRepository
    private val location = DiscoveryLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga")

    @Before fun setUp() { repository = InMemoryDiscoveryRepository() }

    @Test fun `home returns categories and curated merchant sections`() { val data = home(); assertEquals(5, data.categories.size); assertEquals(4, data.nearby.items.size); assertTrue(data.nearby.hasMore); assertTrue(data.featured.items.all { it.isFeatured }) }
    @Test fun `search matches trimmed name description and category case insensitively`() {
        assertEquals("cafe-horizonte", search(query = "  CAFÉ HORIZONTE ").single().id)
        assertEquals("mercado-talatona", search(query = "essenciais").single().id)
        assertEquals(2, search(query = "pastelaria").size)
    }
    @Test fun `filters are applied in repository`() {
        assertTrue(search(onlyOpen = true).all { it.availability is MerchantAvailability.Open || it.availability is MerchantAvailability.ClosingSoon })
        assertTrue(search(fulfillment = setOf(FulfillmentOption.DELIVERY)).all { FulfillmentOption.DELIVERY in it.fulfillmentOptions })
        assertTrue(search(categoryId = "services").all { it.categoryId == "services" })
    }
    @Test fun `multiple fulfillment filters use OR semantics`() {
        val selected = setOf(FulfillmentOption.DELIVERY, FulfillmentOption.SERVICE)
        val results = search(fulfillment = selected)
        assertTrue(results.any { FulfillmentOption.DELIVERY in it.fulfillmentOptions })
        assertTrue(results.any { FulfillmentOption.SERVICE in it.fulfillmentOptions })
        assertTrue(results.all { merchant -> merchant.fulfillmentOptions.any(selected::contains) })
    }
    @Test fun `no fulfillment selection does not restrict results`() = assertEquals(8, search(fulfillment = emptySet()).size)
    @Test fun `fulfillment combines with category and open now`() {
        val results = search(categoryId = "bakery", onlyOpen = true, fulfillment = setOf(FulfillmentOption.DELIVERY, FulfillmentOption.PICKUP))
        assertTrue(results.isNotEmpty()); assertTrue(results.all { it.categoryId == "bakery" })
    }
    @Test fun `all order contracts are respected`() {
        assertEquals("sabor-maianga", search(orderBy = DiscoveryOrderBy.NEAREST).first().id)
        assertEquals("paes-mutamba", search(orderBy = DiscoveryOrderBy.TOP_RATED).first().id)
        assertEquals("mercado-talatona", search(orderBy = DiscoveryOrderBy.MOST_POPULAR).first().id)
        assertTrue(search(orderBy = DiscoveryOrderBy.FEATURED).take(3).all { it.isFeatured })
        assertEquals(search(orderBy = DiscoveryOrderBy.NAME).map { it.name }.sortedBy { it.lowercase() }, search(orderBy = DiscoveryOrderBy.NAME).map { it.name })
    }
    @Test fun `pagination returns explicit metadata`() = runBlocking {
        val data = (repository.search(DiscoverySearchRequest(page = 2, pageSize = 3, location = location)) as DiscoveryResult.Success).data
        assertEquals(3, data.merchants.size); assertEquals(8, data.totalCount); assertTrue(data.hasMore)
    }
    @Test fun `missing location never invents distance`() = assertTrue(search(location = null).all { it.distanceMeters == null })
    @Test fun `merchant is loaded only by id`() = runBlocking { assertEquals("sabor-maianga", (repository.merchant(MerchantRequest("sabor-maianga")) as DiscoveryResult.Success).data.id) }
    @Test fun `unknown merchant is explicit not found error`() = runBlocking { assertEquals(DiscoveryError.NotFound, (repository.merchant(MerchantRequest("missing")) as DiscoveryResult.Error).reason) }
    @Test fun `empty offline and error are explicit outcomes`() = runBlocking {
        repository.scenario = MockDiscoveryScenario.EMPTY; assertTrue(repository.search(DiscoverySearchRequest()) is DiscoveryResult.Empty)
        repository.scenario = MockDiscoveryScenario.OFFLINE; assertTrue(repository.search(DiscoverySearchRequest()) is DiscoveryResult.Offline)
        repository.scenario = MockDiscoveryScenario.ERROR; assertTrue(repository.search(DiscoverySearchRequest()) is DiscoveryResult.Error)
    }

    @Test fun `home without location has recommendations and no nearby merchants`() = runBlocking {
        val content = (repository.home(HomeDiscoveryRequest(null)) as DiscoveryResult.Success).data
        assertTrue(content.nearby.items.isEmpty())
        assertTrue(content.recommended.items.isNotEmpty())
        assertTrue(content.recommended.items.all { it.distanceMeters == null })
    }

    @Test fun `home sections keep stable unique ids`() {
        val first = home()
        val second = home()
        listOf(first.nearby, first.recommended, first.featured).forEach { section ->
            assertEquals(section.items.size, section.items.map { it.id }.distinct().size)
        }
        assertEquals(first.featured.items.map { it.id }, second.featured.items.map { it.id })
        assertEquals(first.nearby.items.map { it.id }, second.nearby.items.map { it.id })
    }

    @Test fun `home force refresh preserves contract and source`() = runBlocking {
        val result = repository.home(HomeDiscoveryRequest(location, forceRefresh = true))
        assertTrue(result is DiscoveryResult.Success)
        result as DiscoveryResult.Success
        assertEquals(DataSource.MEMORY, result.source)
        assertEquals(DiscoveryFixtures.categories, result.data.categories)
    }

    @Test fun `home scenarios expose content empty offline payload and explicit error`() = runBlocking {
        assertTrue(repository.home(HomeDiscoveryRequest(location)) is DiscoveryResult.Success)
        repository.scenario = MockDiscoveryScenario.EMPTY
        assertTrue(repository.home(HomeDiscoveryRequest(location)) is DiscoveryResult.Empty)
        repository.scenario = MockDiscoveryScenario.OFFLINE
        val offline = repository.home(HomeDiscoveryRequest(location)) as DiscoveryResult.Offline
        assertNotNull(offline.cachedData)
        repository.scenario = MockDiscoveryScenario.ERROR
        assertEquals(DiscoveryError.Server, (repository.home(HomeDiscoveryRequest(location)) as DiscoveryResult.Error).reason)
    }

    @Test fun `search trims and removes accents at repository boundary`() {
        assertEquals(listOf("cafe-horizonte"), search(query = "  cafe  ").map { it.id })
        assertEquals(listOf("servicos-viana"), search(query = "SERVICOS").map { it.id })
    }

    @Test fun `search by every category and fulfillment modality is coherent`() {
        DiscoveryFixtures.categories.forEach { category ->
            assertTrue(search(categoryId = category.id).all { it.categoryId == category.id })
        }
        FulfillmentOption.entries.forEach { option ->
            assertTrue(search(fulfillment = setOf(option)).all { option in it.fulfillmentOptions })
        }
    }

    @Test fun `clearing filters does not alter a query result`() {
        val filtered = search(query = "pão", fulfillment = setOf(FulfillmentOption.DELIVERY))
        val cleared = search(query = "pão")
        assertTrue(filtered.size <= cleared.size)
        assertEquals(setOf("doce-embondeiro", "paes-mutamba"), cleared.map { it.id }.toSet())
    }

    @Test fun `empty query explores all merchants and no match is empty`() = runBlocking {
        assertEquals(DiscoveryFixtures.merchants.size, search(query = "   ").size)
        val result = repository.search(DiscoverySearchRequest(query = "não existe", location = location))
        assertTrue(result is DiscoveryResult.Empty)
        assertEquals(0, (result as DiscoveryResult.Empty).data?.totalCount)
    }

    @Test fun `default pagination is twenty and metadata remains coherent`() = runBlocking {
        val result = repository.search(DiscoverySearchRequest(location = location)) as DiscoveryResult.Success
        assertEquals(1, result.data.page)
        assertEquals(DiscoveryPaging.DEFAULT_PAGE_SIZE, result.data.pageSize)
        assertEquals(DiscoveryFixtures.merchants.size, result.data.totalCount)
        assertFalse(result.data.hasMore)
    }

    @Test fun `invalid pagination values are normalized without leaking an exception`() = runBlocking {
        val result = repository.search(DiscoverySearchRequest(page = 0, pageSize = 1000, location = location)) as DiscoveryResult.Success
        assertEquals(1, result.data.page)
        assertEquals(DiscoveryPaging.MAX_PAGE_SIZE, result.data.pageSize)
    }

    @Test fun `nearest is increasing and absent distances stay last`() {
        val withLocation = search(orderBy = DiscoveryOrderBy.NEAREST)
        assertEquals(withLocation.mapNotNull { it.distanceMeters }.sorted(), withLocation.mapNotNull { it.distanceMeters })
        val withoutLocation = search(orderBy = DiscoveryOrderBy.NEAREST, location = null)
        assertTrue(withoutLocation.all { it.distanceMeters == null })
    }

    @Test fun `top rated uses rating count and leaves unrated merchants last`() {
        val results = search(orderBy = DiscoveryOrderBy.TOP_RATED)
        assertEquals("paes-mutamba", results.first().id)
        assertTrue(results.takeLast(2).all { it.rating == null })
    }

    @Test fun `popular featured and name orders are deterministic`() {
        DiscoveryOrderBy.entries.forEach { order ->
            assertEquals(search(orderBy = order).map { it.id }, search(orderBy = order).map { it.id })
        }
        assertEquals(DiscoveryFixtures.merchants.maxBy { it.popularity }.id, search(orderBy = DiscoveryOrderBy.MOST_POPULAR).first().id)
        assertTrue(search(orderBy = DiscoveryOrderBy.FEATURED).takeWhile { it.isFeatured }.isNotEmpty())
    }

    @Test fun `merchant overview preserves all shared summary facts`() = runBlocking {
        DiscoveryFixtures.merchants.forEach { summary ->
            val overview = (repository.merchant(MerchantRequest(summary.id)) as DiscoveryResult.Success).data
            assertEquals(summary.id, overview.id)
            assertEquals(summary.name, overview.name)
            assertEquals(summary.categoryId, overview.category.id)
            assertEquals(summary.fulfillmentOptions, overview.fulfillmentOptions)
            assertEquals(summary.rating, overview.rating)
            assertEquals(summary.ratingCount, overview.ratingCount)
            assertEquals(summary.promotion, overview.promotion)
        }
    }

    @Test fun `merchant overview supports optional fields and both catalog states`() = runBlocking {
        val partial = (repository.merchant(MerchantRequest("servicos-viana")) as DiscoveryResult.Success).data
        assertNull(partial.contact)
        assertNull(partial.schedule)
        assertNull(partial.promotion)
        assertFalse(partial.catalogAvailable)
        val complete = (repository.merchant(MerchantRequest("sabor-maianga")) as DiscoveryResult.Success).data
        assertNotNull(complete.contact)
        assertNotNull(complete.schedule)
        assertTrue(complete.catalogAvailable)
    }

    @Test fun `merchant offline contains overview and error remains explicit`() = runBlocking {
        repository.scenario = MockDiscoveryScenario.OFFLINE
        val offline = repository.merchant(MerchantRequest("sabor-maianga")) as DiscoveryResult.Offline
        assertEquals("sabor-maianga", offline.cachedData?.id)
        repository.scenario = MockDiscoveryScenario.ERROR
        assertEquals(DiscoveryError.Server, (repository.merchant(MerchantRequest("sabor-maianga")) as DiscoveryResult.Error).reason)
    }

    private fun home() = runBlocking { (repository.home(HomeDiscoveryRequest(location)) as DiscoveryResult.Success).data }
    private fun search(
        query: String = "",
        categoryId: String? = null,
        onlyOpen: Boolean = false,
        fulfillment: Set<FulfillmentOption> = emptySet(),
        orderBy: DiscoveryOrderBy = DiscoveryOrderBy.NEAREST,
        location: DiscoveryLocation? = this.location
    ) = runBlocking { (repository.search(DiscoverySearchRequest(query, categoryId, onlyOpen, fulfillment, orderBy, location = location)) as DiscoveryResult.Success).data.merchants }
}
