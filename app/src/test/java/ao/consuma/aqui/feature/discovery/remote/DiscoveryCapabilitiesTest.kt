package ao.consuma.aqui.feature.discovery.remote

import ao.consuma.aqui.feature.discovery.domain.capability.DiscoveryCapabilities
import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy
import org.junit.Assert.*
import org.junit.Test

class DiscoveryCapabilitiesTest {
    @Test fun `remote advertises only backend capabilities`() {
        assertEquals(setOf(DiscoveryOrderBy.NAME), DiscoveryCapabilities.Remote.supportedSorts)
        assertFalse(DiscoveryCapabilities.Remote.supportsOnlyOpen)
        assertFalse(DiscoveryCapabilities.Remote.supportsFulfillmentFilter)
    }

    @Test fun `mock preserves all isolated development capabilities`() {
        assertEquals(DiscoveryOrderBy.entries.toSet(), DiscoveryCapabilities.Mock.supportedSorts)
        assertTrue(DiscoveryCapabilities.Mock.supportsOnlyOpen)
        assertTrue(DiscoveryCapabilities.Mock.supportsFulfillmentFilter)
    }
}
