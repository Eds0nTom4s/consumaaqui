package ao.consuma.aqui.feature.discovery.domain.repository

import ao.consuma.aqui.feature.discovery.domain.model.HomeDiscoveryContent
import ao.consuma.aqui.feature.discovery.domain.model.MerchantOverview
import ao.consuma.aqui.feature.discovery.domain.model.MerchantSearchContent
import ao.consuma.aqui.feature.discovery.domain.request.DiscoverySearchRequest
import ao.consuma.aqui.feature.discovery.domain.request.HomeDiscoveryRequest
import ao.consuma.aqui.feature.discovery.domain.request.MerchantRequest
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult

interface DiscoveryRepository {
    suspend fun home(request: HomeDiscoveryRequest): DiscoveryResult<HomeDiscoveryContent>
    suspend fun search(request: DiscoverySearchRequest): DiscoveryResult<MerchantSearchContent>
    suspend fun merchant(request: MerchantRequest): DiscoveryResult<MerchantOverview>
}
