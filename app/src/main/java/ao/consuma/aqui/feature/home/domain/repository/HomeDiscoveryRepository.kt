package ao.consuma.aqui.feature.home.domain.repository

import ao.consuma.aqui.feature.home.domain.model.DiscoveryLocation
import ao.consuma.aqui.feature.home.domain.model.HomeDiscoveryContent

data class HomeDiscoveryRequest(
    val location: DiscoveryLocation?,
    val selectedCategoryId: String? = null,
    val query: String? = null,
    val forceRefresh: Boolean = false
)

enum class DataSource { MEMORY, REMOTE, CACHE }

sealed interface HomeDiscoveryError {
    data object NetworkUnavailable : HomeDiscoveryError
    data object LocationRequired : HomeDiscoveryError
    data object Unauthorized : HomeDiscoveryError
    data object NotFound : HomeDiscoveryError
    data object Server : HomeDiscoveryError
    data object Unknown : HomeDiscoveryError
}

sealed interface HomeDiscoveryResult {
    data class Success(
        val content: HomeDiscoveryContent,
        val source: DataSource,
        val isOffline: Boolean = false
    ) : HomeDiscoveryResult

    data class Failure(val error: HomeDiscoveryError) : HomeDiscoveryResult
}

interface HomeDiscoveryRepository {
    suspend fun getHomeContent(request: HomeDiscoveryRequest): HomeDiscoveryResult
}
