package ao.consuma.aqui.feature.catalog.presentation.mapper

import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.catalog.data.CatalogFixtures
import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.catalog.domain.service.ProductPriceBreakdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class CatalogUiMapperTest {
    private val mapper = CatalogUiMapper()

    @Test fun `money formatting is deterministic and currency aware`() {
        assertEquals("4.500 Kz", mapper.money(MoneyAmount(450_000, "AOA")))
        assertEquals("12,50 USD", mapper.money(MoneyAmount(1_250, "USD")))
    }

    @Test fun `product card maps discount missing data options and category`() {
        val product = CatalogFixtures.product("sabor-maianga", "sabor-maianga-product-frango-grelhado")!!
        val ui = mapper.product(product, "Grelhados")
        assertEquals("4.500 Kz", ui.compareAtPriceText)
        assertTrue(ui.hasOptions)
        assertEquals("Grelhados", ui.categoryName)

        val minimal = mapper.product(
            CatalogFixtures.product("sabor-maianga", "sabor-maianga-product-agua-casa")!!,
            "Bebidas"
        )
        assertNull(minimal.description)
        assertFalse(minimal.hasImage)
        assertNull(minimal.compareAtPriceText)
    }

    @Test fun `availability maps labels and non colour semantics`() {
        val product = CatalogFixtures.product("sabor-maianga", "sabor-maianga-product-calulu-demonstrativo")!!
        val ui = mapper.productDetail(product)
        assertEquals(ConsumaStatusSemantic.ERROR, ui.availabilitySemantic)
        assertEquals(CatalogUiText.Resource(R.string.product_out_of_stock), ui.availabilityLabel)
        assertFalse(ui.canConfigure)
    }

    @Test fun `every availability state maps to an explicit label semantic and capability`() {
        val base = CatalogFixtures.product(
            "sabor-maianga",
            "sabor-maianga-product-muamba-casa"
        )!!
        val cases = listOf(
            ProductAvailability.Available to R.string.product_available,
            ProductAvailability.Unavailable to R.string.product_unavailable,
            ProductAvailability.TemporarilyUnavailable to R.string.product_temporarily_unavailable,
            ProductAvailability.AvailableFrom(LocalTime.of(18, 30)) to R.string.product_available_from,
            ProductAvailability.OutOfStock to R.string.product_out_of_stock,
            ProductAvailability.Unknown to R.string.product_availability_unknown
        )
        cases.forEach { (availability, label) ->
            val ui = mapper.productDetail(base.copy(availability = availability))
            assertEquals(label, (ui.availabilityLabel as CatalogUiText.Resource).id)
            assertEquals(availability == ProductAvailability.Available, ui.canConfigure)
            assertTrue(ui.availabilitySemantic in ConsumaStatusSemantic.entries)
        }
    }

    @Test fun `option rules and additional prices are preformatted`() {
        val product = CatalogFixtures.product("sabor-maianga", "sabor-maianga-product-frango-grelhado")!!
        val detail = mapper.productDetail(product)
        val sauce = detail.optionGroups.first { it.id == "frango-grelhado-sauce" }
        assertTrue(sauce.singleChoice)
        assertFalse(sauce.required)
        assertEquals(CatalogUiText.Resource(R.string.product_rule_choose_up_to, listOf(1)), sauce.ruleLabel)
        assertEquals("+ 200 Kz", sauce.options.first { it.id.endsWith("garlic") }.additionalPriceText)
    }

    @Test fun `price summary maps base options and total`() {
        val summary = mapper.priceSummary(
            ProductPriceBreakdown(
                MoneyAmount(1_000, "AOA"), MoneyAmount(200, "AOA"),
                MoneyAmount(1_200, "AOA"), MoneyAmount(2_400, "AOA")
            )
        )
        assertEquals("10 Kz", summary.unitPriceText)
        assertEquals("2 Kz", summary.optionsPriceText)
        assertEquals("24 Kz", summary.totalPriceText)
    }
}
