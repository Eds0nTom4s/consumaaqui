package ao.consuma.aqui.feature.discovery.remote

import ao.consuma.aqui.feature.discovery.domain.model.FulfillmentOption
import ao.consuma.aqui.feature.discovery.domain.model.MerchantAvailability
import org.junit.Assert.*
import org.junit.Test

class DiscoveryDtoMapperTest {
    private val mapper = DiscoveryDtoMapper()

    @Test fun `canonical optional unknown and catalog fields map without invented values`() {
        val dto = summary(availability = "FUTURE_STATUS", fulfillment = listOf("PICKUP", "FUTURE_MODE"))
        val mapped = mapper.search(MerchantSearchDto(emptyList(), listOf(dto), 0, 20, 1, false)).merchants.single()
        assertEquals(MerchantAvailability.Unknown, mapped.availability)
        assertEquals(setOf(FulfillmentOption.PICKUP), mapped.fulfillmentOptions)
        assertTrue(mapped.catalogAvailable)
        assertNull(mapped.categoryId)
        assertNull(mapped.rating)
        assertNull(mapped.popularity)
    }

    @Test fun `empty lists and zero based backend page are preserved and translated centrally`() {
        val mapped = mapper.search(MerchantSearchDto(emptyList(), emptyList(), 3, 10, 0, false))
        assertTrue(mapped.categories.isEmpty())
        assertTrue(mapped.merchants.isEmpty())
        assertEquals(4, mapped.page)
    }

    @Test fun `detail unsupported nullable fields remain absent`() {
        val mapped = mapper.overview(
            MerchantOverviewDto(UUID, "Café Órbita", "UNKNOWN", emptyList(), featured = false, catalogAvailable = true)
        )
        assertEquals(UUID, mapped.id)
        assertNull(mapped.category)
        assertNull(mapped.schedule)
        assertNull(mapped.address)
        assertTrue(mapped.catalogAvailable)
    }

    private fun summary(
        availability: String,
        fulfillment: List<String>
    ) = MerchantSummaryDto(
        merchantId = UUID,
        name = "Café Órbita",
        availability = availability,
        fulfillmentOptions = fulfillment,
        featured = false,
        catalogAvailable = true
    )

    private companion object {
        const val UUID = "123e4567-e89b-42d3-a456-426614174000"
    }
}
