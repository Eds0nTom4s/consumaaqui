package ao.consuma.aqui.feature.discovery.domain.result

enum class DataSource { MEMORY, REMOTE, CACHE }

sealed interface DiscoveryError {
    data object InvalidRequest : DiscoveryError
    data object UnsupportedSort : DiscoveryError
    data object NetworkUnavailable : DiscoveryError
    data object Timeout : DiscoveryError
    data object ContractError : DiscoveryError
    data object LocationRequired : DiscoveryError
    data object Unauthorized : DiscoveryError
    data object Forbidden : DiscoveryError
    data object RateLimited : DiscoveryError
    data object NotFound : DiscoveryError
    data object ServiceUnavailable : DiscoveryError
    data object Server : DiscoveryError
    data object Unknown : DiscoveryError
}

/** Explicit outcomes keep transport exceptions outside presentation code. */
sealed interface DiscoveryResult<out T> {
    data class Success<T>(val data: T, val source: DataSource) : DiscoveryResult<T>
    data class Empty<T>(val data: T? = null) : DiscoveryResult<T>
    data class Offline<T>(
        val cachedData: T? = null,
        val source: DataSource = DataSource.CACHE
    ) : DiscoveryResult<T>
    data class Error(val reason: DiscoveryError) : DiscoveryResult<Nothing>
}
