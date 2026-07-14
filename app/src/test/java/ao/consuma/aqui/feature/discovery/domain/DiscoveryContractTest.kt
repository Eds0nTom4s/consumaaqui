package ao.consuma.aqui.feature.discovery.domain

import ao.consuma.aqui.feature.discovery.domain.model.*
import ao.consuma.aqui.feature.discovery.domain.request.*
import ao.consuma.aqui.feature.discovery.domain.result.*
import org.junit.Assert.*
import org.junit.Test

class DiscoveryContractTest {
    private val location = DiscoveryLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga")

    @Test fun `request uses domain location and preserves multiple modalities`() {
        val request = DiscoverySearchRequest(
            query = " café ",
            fulfillmentOptions = setOf(FulfillmentOption.DELIVERY, FulfillmentOption.PICKUP),
            orderBy = DiscoveryOrderBy.TOP_RATED,
            page = 2,
            pageSize = 10,
            location = location
        )
        assertEquals(location, request.location)
        assertEquals(2, request.fulfillmentOptions.size)
        assertEquals(DiscoveryOrderBy.TOP_RATED, request.orderBy)
        assertEquals(2, request.page)
        assertEquals(10, request.pageSize)
        assertEquals(" café ", request.query)
    }

    @Test fun `results preserve source offline payload and explicit errors`() {
        val success = DiscoveryResult.Success("payload", DataSource.MEMORY)
        val offline = DiscoveryResult.Offline("cached", DataSource.CACHE)
        val notFound = DiscoveryResult.Error(DiscoveryError.NotFound)
        assertEquals(DataSource.MEMORY, success.source)
        assertEquals("cached", offline.cachedData)
        assertEquals(DiscoveryError.NotFound, notFound.reason)
        assertNotEquals(DiscoveryError.Unknown, notFound.reason)
    }

    @Test fun `default paging contract is stable`() {
        val request = DiscoverySearchRequest()
        assertEquals(1, request.page)
        assertEquals(20, request.pageSize)
        assertEquals(DiscoveryPaging.DEFAULT_PAGE_SIZE, request.pageSize)
    }
}
