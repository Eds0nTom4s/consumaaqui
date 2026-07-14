package ao.consuma.aqui.feature.discovery.presentation.mapper

import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.feature.discovery.data.DiscoveryFixtures
import ao.consuma.aqui.feature.discovery.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class DiscoveryUiMapperTest {
    private val mapper = DiscoveryUiMapper()

    @Test fun `categories include resource backed all option and preserve domain labels`() {
        val categories = mapper.categories(DiscoveryFixtures.categories)
        assertNull(categories.first().id)
        assertEquals(UiText.Resource(R.string.search_filter_all), categories.first().label)
        assertEquals(UiText.Dynamic("Restaurantes"), categories[1].label)
    }

    @Test fun `compact mapping preserves identity category image availability and primary modality`() {
        val content = DiscoveryFixtures.home(hasLocation = true)
        val sections = mapper.home(content, hasLocation = true)
        val merchant = sections.featured.first { it.id == "sabor-maianga" }
        assertEquals("Sabor da Maianga", merchant.name)
        assertEquals("Restaurantes", merchant.categoryLabel)
        assertFalse(merchant.hasImage)
        assertEquals(ConsumaStatusSemantic.SUCCESS, merchant.availabilitySemantic)
        assertEquals(UiText.Resource(R.string.home_fulfillment_delivery), merchant.primaryFulfillmentLabel)
        assertNotNull(merchant.convenienceText)
        assertNotNull(merchant.promotionText)
    }

    @Test fun `compact mapping hides distance when location is absent`() {
        val content = DiscoveryFixtures.home(hasLocation = false)
        val merchant = mapper.home(content, hasLocation = false).recommended.first()
        val joined = merchant.convenienceText as UiText.Joined
        assertTrue(joined.values.none { it is UiText.Resource && it.id == R.string.merchant_distance_meters })
        assertTrue(joined.values.none { it is UiText.Resource && it.id == R.string.merchant_distance_kilometers })
    }

    @Test fun `list mapping preserves multiple modalities rating distance state promotion and description`() {
        val content = ao.consuma.aqui.feature.discovery.domain.model.MerchantSearchContent(
            DiscoveryFixtures.categories,
            DiscoveryFixtures.merchants,
            1,
            20,
            DiscoveryFixtures.merchants.size,
            false
        )
        val items = mapper.search(content, hasLocation = true)
        val featured = items.first { it.id == "sabor-maianga" }
        assertEquals("Sabores angolanos preparados no dia", featured.subtitle)
        assertTrue((featured.fulfillmentText as UiText.Joined).values.size > 1)
        assertNotNull(featured.convenienceText)
        assertNotNull(featured.promotionText)
        assertEquals(ConsumaStatusSemantic.SUCCESS, featured.availabilitySemantic)
        assertEquals(ConsumaStatusSemantic.ERROR, items.first { it.id == "cantinho-kilamba" }.availabilitySemantic)
        assertEquals(ConsumaStatusSemantic.WARNING, items.first { it.id == "doce-embondeiro" }.availabilitySemantic)
    }

    @Test fun `overview mapping preserves complete and absent fields`() {
        val complete = mapper.overview(DiscoveryFixtures.overview(DiscoveryFixtures.merchants.first()))
        assertEquals("Sabor da Maianga", complete.name)
        assertEquals("Restaurantes", complete.category)
        assertNotNull(complete.fullDescription)
        assertNotNull(complete.contactText)
        assertNotNull(complete.openingHoursText)
        assertNotNull(complete.addressText)
        assertNotNull(complete.ratingText)
        assertNotNull(complete.promotionText)
        assertTrue(complete.catalogAvailable)

        val partial = mapper.overview(DiscoveryFixtures.overview(DiscoveryFixtures.merchants.first { it.id == "servicos-viana" }))
        assertNull(partial.contactText)
        assertNull(partial.openingHoursText)
        assertNull(partial.ratingText)
        assertNull(partial.promotionText)
        assertFalse(partial.catalogAvailable)
    }

    @Test fun `formatting uses typed resource values`() {
        assertEquals(UiText.Resource(R.string.merchant_distance_meters, listOf(450)), mapper.distance(450))
        assertEquals(UiText.Resource(R.string.merchant_distance_kilometers, listOf("1,2")), mapper.distance(1250))
        assertNull(mapper.distance(null))
        assertEquals(UiText.Resource(R.string.merchant_money_kwanza, listOf("5.500")), mapper.money(MoneyAmount(550000, "AOA")))
    }

    @Test fun `sort and result labels remain typed and proximity availability is explicit`() {
        val noLocation = mapper.sortOptions(false)
        assertFalse(noLocation.first { it.value == DiscoveryOrderBy.NEAREST }.enabled)
        assertTrue(noLocation.filterNot { it.value == DiscoveryOrderBy.NEAREST }.all { it.enabled })
        assertTrue(mapper.resultContext(1, true) is UiText.Plural)
        assertTrue(mapper.resultContext(2, false) is UiText.Plural)
    }
}
