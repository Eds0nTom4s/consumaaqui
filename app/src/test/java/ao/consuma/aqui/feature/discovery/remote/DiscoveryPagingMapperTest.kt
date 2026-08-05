package ao.consuma.aqui.feature.discovery.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DiscoveryPagingMapperTest {
    @Test fun `android pages map centrally to backend pages`() {
        assertEquals(0, DiscoveryPagingMapper.toBackend(1))
        assertEquals(6, DiscoveryPagingMapper.toBackend(7))
        assertEquals(1, DiscoveryPagingMapper.toAndroid(0))
        assertEquals(8, DiscoveryPagingMapper.toAndroid(7))
    }

    @Test fun `invalid pages are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { DiscoveryPagingMapper.toBackend(0) }
        assertThrows(IllegalArgumentException::class.java) { DiscoveryPagingMapper.toAndroid(-1) }
    }
}
