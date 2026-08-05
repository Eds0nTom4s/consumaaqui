package ao.consuma.aqui.feature.discovery.remote

import ao.consuma.aqui.BuildConfig
import ao.consuma.aqui.feature.discovery.data.BuildConfigDiscoverySourcePolicy
import ao.consuma.aqui.feature.discovery.data.InMemoryDiscoveryRepository
import ao.consuma.aqui.feature.discovery.data.SelectableDiscoveryRepository
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySource
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySourcePolicy
import ao.consuma.aqui.feature.discovery.domain.request.MerchantRequest
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryError
import ao.consuma.aqui.feature.discovery.domain.result.DiscoveryResult
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DiscoverySourcePolicyTest {
    @Test fun `build variant source policy is explicit`() {
        val policy = BuildConfigDiscoverySourcePolicy()
        if (BuildConfig.ENVIRONMENT == "DEBUG") {
            assertTrue(policy.selectable)
            assertEquals(DiscoverySource.MOCK, policy.source.value)
            assertTrue(policy.select(DiscoverySource.REMOTE))
        } else {
            assertFalse(policy.selectable)
            assertEquals(DiscoverySource.REMOTE, policy.source.value)
            assertFalse(policy.select(DiscoverySource.MOCK))
        }
    }

    @Test fun `remote failure never falls back to mock fixtures`() = runBlocking {
        val policy = object : DiscoverySourcePolicy {
            override val source = MutableStateFlow(DiscoverySource.REMOTE)
            override val selectable = true
            override fun select(source: DiscoverySource): Boolean { this.source.value = source; return true }
        }
        val remote = RemoteDiscoveryRepository(
            DiscoveryApiProvider { throw IOException("offline") },
            DiscoveryDtoMapper(),
            DiscoveryNetworkFactory.json()
        )
        val repository = SelectableDiscoveryRepository(InMemoryDiscoveryRepository(), remote, policy)
        val result = repository.merchant(MerchantRequest("sabor-maianga")) as DiscoveryResult.Error
        assertEquals(DiscoveryError.NetworkUnavailable, result.reason)
    }
}
