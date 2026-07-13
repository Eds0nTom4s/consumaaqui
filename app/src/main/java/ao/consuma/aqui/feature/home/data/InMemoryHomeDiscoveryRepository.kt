package ao.consuma.aqui.feature.home.data

import ao.consuma.aqui.feature.home.domain.repository.DataSource
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryError
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryRepository
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryRequest
import ao.consuma.aqui.feature.home.domain.repository.HomeDiscoveryResult
import javax.inject.Inject
import javax.inject.Singleton

enum class MockHomeScenario { CONTENT, EMPTY, ERROR, OFFLINE }

interface MockHomeScenarioController {
    var scenario: MockHomeScenario
}

@Singleton
class InMemoryHomeDiscoveryRepository @Inject constructor() :
    HomeDiscoveryRepository,
    MockHomeScenarioController {

    override var scenario: MockHomeScenario = MockHomeScenario.CONTENT

    override suspend fun getHomeContent(request: HomeDiscoveryRequest): HomeDiscoveryResult {
        if (scenario == MockHomeScenario.ERROR) {
            return HomeDiscoveryResult.Failure(HomeDiscoveryError.Server)
        }

        val query = request.query.orEmpty().trim()
        val items = if (scenario == MockHomeScenario.EMPTY) emptyList() else {
            HomeDiscoveryFixtures.merchants.filter { merchant ->
                val categoryMatches = request.selectedCategoryId == null ||
                    merchant.categoryId == request.selectedCategoryId
                val categoryName = HomeDiscoveryFixtures.categories
                    .find { it.id == merchant.categoryId }?.name.orEmpty()
                val queryMatches = query.isBlank() || listOf(
                    merchant.name,
                    merchant.shortDescription.orEmpty(),
                    categoryName
                ).any { it.contains(query, ignoreCase = true) }
                categoryMatches && queryMatches
            }.map { merchant ->
                if (request.location == null) merchant.copy(distanceMeters = null) else merchant
            }
        }

        return HomeDiscoveryResult.Success(
            content = HomeDiscoveryFixtures.content(items),
            source = DataSource.MEMORY,
            isOffline = scenario == MockHomeScenario.OFFLINE
        )
    }
}
