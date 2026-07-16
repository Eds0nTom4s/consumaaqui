package ao.consuma.aqui.feature.catalog.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class CatalogModelsTest {
    @Test fun `selection rule derives required and single choice`() {
        val rule = SelectionRule(1, 1)
        assertTrue(rule.required)
        assertTrue(rule.singleChoice)
        assertFalse(SelectionRule(0, 3).required)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selection rule rejects minimum above maximum`() {
        SelectionRule(2, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unavailable option cannot be a default`() {
        ProductOption("option", "Option", null, null, false, 0, true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `compare-at price must be a real discount in the same currency`() {
        product(compareAtPrice = MoneyAmount(900, "AOA"))
    }

    @Test fun `catalog stores products separately and links them by category id`() {
        val category = CatalogCategory("category", "merchant", "Category", null, null, 0, true)
        val product = product()
        val catalog = MerchantCatalog(
            "merchant", "catalog", null, null,
            listOf(category), listOf(product), "AOA", null, null
        )
        assertEquals(category.id, catalog.products.single().categoryId)
        assertEquals("merchant", catalog.categories.single().merchantId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `catalog rejects product linked to missing category`() {
        val category = CatalogCategory("another", "merchant", "Category", null, null, 0, true)
        MerchantCatalog(
            "merchant", "catalog", null, null,
            listOf(category), listOf(product()), "AOA", null, null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `configuration enforces maximum quantity`() {
        ProductConfiguration("product", emptyMap(), 100, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `configuration enforces minimum quantity`() {
        ProductConfiguration("product", emptyMap(), 0, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `configuration enforces note limit`() {
        ProductConfiguration("product", emptyMap(), 1, "a".repeat(251))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `product rejects option price in another currency`() {
        product(
            optionGroups = listOf(
                ProductOptionGroup(
                    id = "group",
                    name = "Group",
                    description = null,
                    selectionRule = SelectionRule(0, 1),
                    options = listOf(
                        ProductOption(
                            id = "option",
                            name = "Option",
                            description = null,
                            additionalPrice = MoneyAmount(100, "USD"),
                            available = true,
                            sortOrder = 0,
                            defaultSelected = false
                        )
                    ),
                    sortOrder = 0
                )
            )
        )
    }

    @Test fun `availability states preserve their distinct meanings`() {
        val states = setOf(
            ProductAvailability.Available,
            ProductAvailability.Unavailable,
            ProductAvailability.TemporarilyUnavailable,
            ProductAvailability.OutOfStock,
            ProductAvailability.Unknown
        )
        assertEquals(5, states.size)
        assertEquals(LocalTime.of(18, 30), ProductAvailability.AvailableFrom(LocalTime.of(18, 30)).time)
    }

    @Test fun `selection rules cover optional single and required multiple choice`() {
        val optionalSingle = SelectionRule(0, 1)
        val requiredMultiple = SelectionRule(2, 3)
        assertFalse(optionalSingle.required)
        assertTrue(optionalSingle.singleChoice)
        assertTrue(requiredMultiple.required)
        assertFalse(requiredMultiple.singleChoice)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selection rule rejects a zero maximum`() { SelectionRule(0, 0) }

    @Test(expected = IllegalArgumentException::class)
    fun `group rejects maximum above defined options`() {
        ProductOptionGroup(
            "group", "Group", null, SelectionRule(0, 2),
            listOf(ProductOption("one", "One", null, null, true, 0, false)), 0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `group without options is rejected by explicit selection limits`() {
        ProductOptionGroup("group", "Group", null, SelectionRule(0, 1), emptyList(), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `group rejects duplicate option ids`() {
        ProductOptionGroup(
            "group", "Group", null, SelectionRule(0, 1),
            listOf(
                ProductOption("same", "One", null, null, true, 0, false),
                ProductOption("same", "Two", null, null, true, 1, false)
            ), 0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `group rejects defaults above maximum`() {
        ProductOptionGroup(
            "group", "Group", null, SelectionRule(0, 1),
            listOf(
                ProductOption("one", "One", null, null, true, 0, true),
                ProductOption("two", "Two", null, null, true, 1, true)
            ), 0
        )
    }

    @Test fun `configuration accepts boundary quantities and preserves internal spaces`() {
        assertEquals(1, ProductConfiguration("product", emptyMap(), 1, "").quantity)
        val max = ProductConfiguration("product", emptyMap(), 99, "sem  cebola")
        assertEquals("sem  cebola", max.note)
    }

    private fun product(
        compareAtPrice: MoneyAmount? = null,
        optionGroups: List<ProductOptionGroup> = emptyList()
    ) = CatalogProduct(
        id = "product",
        merchantId = "merchant",
        categoryId = "category",
        name = "Product",
        shortDescription = null,
        fullDescription = null,
        imageUrl = null,
        basePrice = MoneyAmount(1_000, "AOA"),
        compareAtPrice = compareAtPrice,
        availability = ProductAvailability.Available,
        preparationMinutes = null,
        tags = emptySet(),
        optionGroups = optionGroups,
        featured = false,
        sortOrder = 0
    )
}
