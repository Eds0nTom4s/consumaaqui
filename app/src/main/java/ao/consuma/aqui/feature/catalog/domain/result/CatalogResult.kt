package ao.consuma.aqui.feature.catalog.domain.result

enum class CatalogDataSource { MEMORY, REMOTE, CACHE }

sealed interface CatalogError {
    data object MerchantNotFound : CatalogError
    data object CatalogNotFound : CatalogError
    data object ProductNotFound : CatalogError
    data object CatalogUnavailable : CatalogError
    data object ProductUnavailable : CatalogError
    data object NetworkUnavailable : CatalogError
    data object Unauthorized : CatalogError
    data object Server : CatalogError
    data object InvalidConfiguration : CatalogError
    data object Unknown : CatalogError
}

sealed interface CatalogResult<out T> {
    data class Success<T>(
        val data: T,
        val source: CatalogDataSource
    ) : CatalogResult<T>

    data class Offline<T>(
        val cachedData: T?,
        val source: CatalogDataSource
    ) : CatalogResult<T>

    data class Empty<T>(val data: T? = null) : CatalogResult<T>
    data class Error(val reason: CatalogError) : CatalogResult<Nothing>
}
