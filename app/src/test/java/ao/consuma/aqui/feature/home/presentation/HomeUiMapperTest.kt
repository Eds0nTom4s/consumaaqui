package ao.consuma.aqui.feature.home.presentation

import ao.consuma.aqui.R
import ao.consuma.aqui.feature.home.data.HomeDiscoveryFixtures
import ao.consuma.aqui.feature.home.domain.model.FulfillmentOption
import ao.consuma.aqui.feature.home.domain.model.MerchantAvailability
import ao.consuma.aqui.feature.home.domain.model.MoneyAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiMapperTest {
    private val mapper = HomeUiMapper()

    @Test fun `distance below kilometre uses metres`() = assertEquals("500 m", mapper.distance(500))
    @Test fun `distance above kilometre uses decimal kilometres`() = assertEquals("1,2 km", mapper.distance(1250))
    @Test fun `money formats kwanza from minor units`() = assertEquals("5.500 Kz", mapper.money(MoneyAmount(550000, "AOA")))
    @Test fun `missing money remains absent`() = assertNull(mapper.money(null))
    @Test fun `open availability maps to open resource`() = assertEquals(R.string.home_availability_open, merchantWith(MerchantAvailability.Open).availabilityLabel.resourceId())
    @Test fun `closing soon keeps remaining minutes`() { val text = merchantWith(MerchantAvailability.ClosingSoon(20)).availabilityLabel as UiText.Resource; assertEquals(R.string.home_availability_closing_soon, text.id); assertEquals(20, text.args.single()) }
    @Test fun `closed availability maps to closed resource`() = assertEquals(R.string.home_availability_closed, merchantWith(MerchantAvailability.Closed).availabilityLabel.resourceId())
    @Test fun `missing rating remains absent`() = assertNull(HomeDiscoveryFixtures.merchants.first { it.rating == null }.let { merchant(it).ratingText })
    @Test fun `promotion remains optional`() { assertTrue(HomeDiscoveryFixtures.merchants.map { merchant(it).promotionText }.any { it == null }); assertTrue(HomeDiscoveryFixtures.merchants.map { merchant(it).promotionText }.any { it != null }) }
    @Test fun `fulfillment maps to resource labels`() { val labels = merchant(HomeDiscoveryFixtures.merchants.first { FulfillmentOption.SERVICE in it.fulfillmentOptions }).fulfillmentLabels; assertEquals(R.string.home_fulfillment_service, labels.single().resourceId()) }
    @Test fun `missing location suppresses distance`() = assertNull(mapper.merchants(HomeDiscoveryFixtures.content(), false).first.first().distanceText)

    private fun merchantWith(availability: MerchantAvailability) = merchant(HomeDiscoveryFixtures.merchants.first().copy(availability = availability))
    private fun merchant(value: ao.consuma.aqui.feature.home.domain.model.MerchantSummary) = mapper.merchants(HomeDiscoveryFixtures.content(listOf(value)), true).first.single()
    private fun UiText.resourceId() = (this as UiText.Resource).id
}
