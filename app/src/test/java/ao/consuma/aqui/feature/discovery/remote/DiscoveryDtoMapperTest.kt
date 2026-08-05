package ao.consuma.aqui.feature.discovery.remote

import ao.consuma.aqui.feature.discovery.domain.model.FulfillmentOption
import ao.consuma.aqui.feature.discovery.domain.model.MerchantAvailability
import org.junit.Assert.*
import org.junit.Test

class DiscoveryDtoMapperTest {
    private val mapper = DiscoveryDtoMapper()

    @Test fun `optional unknown and catalog fields map without inventing values`() {
        val dto = summary(
            availability = MerchantAvailabilityDto("FUTURE_STATUS"),
            fulfillment = listOf("PICKUP", "FUTURE_MODE"),
            catalogAvailable = true
        )
        val mapped = mapper.search(MerchantSearchDto(emptyList(), listOf(dto), 0, 20, 1, false)).merchants.single()
        assertEquals(MerchantAvailability.Unknown, mapped.availability)
        assertEquals(setOf(FulfillmentOption.PICKUP), mapped.fulfillmentOptions)
        assertTrue(mapped.catalogAvailable)
        assertNull(mapped.rating)
        assertEquals(0, mapped.popularity)
    }

    @Test fun `empty lists and backend page are preserved`() {
        val mapped = mapper.search(MerchantSearchDto(emptyList(), emptyList(), 3, 10, 0, false))
        assertTrue(mapped.categories.isEmpty())
        assertTrue(mapped.merchants.isEmpty())
        assertEquals(4, mapped.page)
    }

    private fun summary(
        availability: MerchantAvailabilityDto,
        fulfillment: List<String>,
        catalogAvailable: Boolean
    ) = MerchantSummaryDto(
        id = "cafe-orbita",
        name = "Café Órbita",
        category = MerchantCategoryDto("cafe", "Cafés"),
        availability = availability,
        fulfillmentOptions = fulfillment,
        catalogAvailable = catalogAvailable
    )
}
