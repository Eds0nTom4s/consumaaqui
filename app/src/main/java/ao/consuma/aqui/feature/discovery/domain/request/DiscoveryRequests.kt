package ao.consuma.aqui.feature.discovery.domain.request

import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryLocation
import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy
import ao.consuma.aqui.feature.discovery.domain.model.FulfillmentOption

data class HomeDiscoveryRequest(
    val location: DiscoveryLocation?,
    val selectedCategoryId: String? = null,
    val query: String? = null,
    val forceRefresh: Boolean = false
)

/** The UI intentionally consumes the first page only; visual pagination is deferred. */
data class DiscoverySearchRequest(
    val query: String = "",
    val categoryId: String? = null,
    val onlyOpen: Boolean = false,
    /** OR semantics: a merchant matches when it offers any selected option. */
    val fulfillmentOptions: Set<FulfillmentOption> = emptySet(),
    val orderBy: DiscoveryOrderBy = DiscoveryOrderBy.FEATURED,
    val page: Int = 1,
    val pageSize: Int = DiscoveryPaging.DEFAULT_PAGE_SIZE,
    val location: DiscoveryLocation? = null
)

data class MerchantRequest(val merchantId: String)

object DiscoveryPaging {
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
}
