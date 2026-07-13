package ao.consuma.aqui.feature.home.data

import ao.consuma.aqui.feature.home.domain.model.DiscoveryLocation
import ao.consuma.aqui.feature.home.domain.repository.DataSource
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryRequest
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking

class InMemoryHomeDiscoveryRepositoryTest {
    private lateinit var repository: InMemoryHomeDiscoveryRepository
    private val location = DiscoveryLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga")

    @Before fun setUp() { repository = InMemoryHomeDiscoveryRepository() }

    @Test fun `content returns categories`() = assertEquals(5, success().content.categories.size)
    @Test fun `content returns merchants`() = assertEquals(8, success().content.nearby.items.size)
    @Test fun `category filter returns only matching merchants`() {
        val items = success(HomeDiscoveryRequest(location, selectedCategoryId = "restaurant")).content.nearby.items
        assertTrue(items.isNotEmpty()); assertTrue(items.all { it.categoryId == "restaurant" })
    }
    @Test fun `search matches merchant name`() = assertEquals(listOf("cafe-horizonte"), success(HomeDiscoveryRequest(location, query = "Café Horizonte")).content.nearby.items.map { it.id })
    @Test fun `search matches description`() = assertEquals("mercado-talatona", success(HomeDiscoveryRequest(location, query = "essenciais")).content.nearby.items.single().id)
    @Test fun `search is case insensitive`() = assertEquals("sabor-maianga", success(HomeDiscoveryRequest(location, query = "SABOR DA MAIANGA")).content.nearby.items.single().id)
    @Test fun `search trims spaces`() = assertEquals("servicos-viana", success(HomeDiscoveryRequest(location, query = "  assistência  ")).content.nearby.items.single().id)
    @Test fun `unknown query is empty`() = assertTrue(success(HomeDiscoveryRequest(location, query = "inexistente")).content.nearby.items.isEmpty())
    @Test fun `error returns explicit failure`() { repository.scenario = MockHomeScenario.ERROR; assertTrue(result(HomeDiscoveryRequest(location)) is HomeDiscoveryResult.Failure) }
    @Test fun `offline returns memory content marked offline`() { repository.scenario = MockHomeScenario.OFFLINE; val result = success(); assertEquals(DataSource.MEMORY, result.source); assertTrue(result.isOffline) }
    @Test fun `force refresh preserves result contract`() = assertEquals(8, success(HomeDiscoveryRequest(location, forceRefresh = true)).content.nearby.items.size)
    @Test fun `fixture ids are stable and unique`() { val ids = success().content.nearby.items.map { it.id }; assertFalse(ids.any { it.isBlank() }); assertEquals(ids.size, ids.toSet().size) }
    @Test fun `missing location removes simulated distance`() = assertTrue(success(HomeDiscoveryRequest(null)).content.nearby.items.all { it.distanceMeters == null })

    private fun success(request: HomeDiscoveryRequest = HomeDiscoveryRequest(location)) =
        result(request) as HomeDiscoveryResult.Success
    private fun result(request: HomeDiscoveryRequest): HomeDiscoveryResult = runBlocking { repository.getHomeContent(request) }
}
