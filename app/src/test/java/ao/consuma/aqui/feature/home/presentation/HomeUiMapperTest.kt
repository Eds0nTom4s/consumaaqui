package ao.consuma.aqui.feature.home.presentation

import ao.consuma.aqui.R
import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.discovery.data.DiscoveryFixtures
import ao.consuma.aqui.feature.discovery.domain.model.MerchantAvailability
import ao.consuma.aqui.feature.discovery.presentation.mapper.UiText
import org.junit.Assert.*
import org.junit.Test

class HomeUiMapperTest {
    private val mapper = HomeUiMapper()

    @Test fun `distance and money formatting are centralized`() {
        assertEquals(UiText.Resource(R.string.merchant_distance_meters, listOf(500)), mapper.distance(500))
        assertEquals(UiText.Resource(R.string.merchant_distance_kilometers, listOf("1,2")), mapper.distance(1250))
        assertEquals(UiText.Resource(R.string.merchant_money_kwanza, listOf("5.500")), mapper.money(MoneyAmount(550000, "AOA")))
        assertNull(mapper.money(null))
    }
    @Test fun `compact card maps availability and one primary fulfillment`() {
        val merchant = DiscoveryFixtures.merchants.first().copy(availability = MerchantAvailability.ClosingSoon(20))
        val model = mapper.home(DiscoveryFixtures.home(listOf(merchant)), true).featured.single()
        assertEquals(R.string.home_availability_closing_soon, (model.availabilityLabel as UiText.Resource).id)
        assertNotNull(model.primaryFulfillmentLabel)
    }
    @Test fun `home mapper keeps distinct curated sections`() {
        val sections = mapper.home(DiscoveryFixtures.home(), true)
        assertTrue(sections.featured.all { model -> DiscoveryFixtures.merchants.first { it.id == model.id }.isFeatured })
        assertEquals(4, sections.nearby.size)
        assertTrue(sections.recommended.isEmpty())
    }
    @Test fun `without location maps recommendations instead of nearby`() {
        val sections = mapper.home(DiscoveryFixtures.home(hasLocation = false), false)
        assertTrue(sections.nearby.isEmpty()); assertTrue(sections.recommended.isNotEmpty())
    }
}
