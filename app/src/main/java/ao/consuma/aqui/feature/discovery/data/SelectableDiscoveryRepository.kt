package ao.consuma.aqui.feature.discovery.data

import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySource
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySourcePolicy
import ao.consuma.aqui.feature.discovery.domain.model.HomeDiscoveryContent
import ao.consuma.aqui.feature.discovery.domain.model.MerchantOverview
import ao.consuma.aqui.feature.discovery.domain.model.MerchantSearchContent
import ao.consuma.aqui.feature.discovery.domain.repository.DiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.request.DiscoverySearchRequest
import ao.consuma.aqui.feature.discovery.domain.request.HomeDiscoveryRequest
import ao.consuma.aqui.feature.discovery.domain.request.MerchantRequest
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult
import ao.consuma.aqui.feature.discovery.remote.RemoteDiscoveryRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Routes only by explicit environment/developer selection. It never falls back after an error. */
@Singleton
class SelectableDiscoveryRepository @Inject constructor(
    private val mock: InMemoryDiscoveryRepository,
    private val remote: RemoteDiscoveryRepository,
    private val sourcePolicy: DiscoverySourcePolicy
) : DiscoveryRepository {
    private val selected: DiscoveryRepository
        get() = if (sourcePolicy.source.value == DiscoverySource.MOCK) mock else remote

    override suspend fun home(request: HomeDiscoveryRequest): DiscoveryResult<HomeDiscoveryContent> = selected.home(request)
    override suspend fun search(request: DiscoverySearchRequest): DiscoveryResult<MerchantSearchContent> = selected.search(request)
    override suspend fun merchant(request: MerchantRequest): DiscoveryResult<MerchantOverview> = selected.merchant(request)
}
