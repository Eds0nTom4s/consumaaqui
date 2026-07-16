package ao.consuma.aqui.feature.catalog.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.catalog.domain.model.CatalogProduct
import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.catalog.domain.model.ProductConfiguration
import ao.consuma.aqui.feature.catalog.domain.model.ProductOption
import ao.consuma.aqui.feature.catalog.domain.model.ProductOptionGroup
import ao.consuma.aqui.feature.catalog.domain.model.SelectionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductConfigurationValidatorTest {
    private val validator = ProductConfigurationValidator()

    @Test fun `factory selects only available defaults`() {
        val configuration = ProductConfigurationFactory.create(product())
        assertEquals(setOf("small"), configuration.selections["size"])
        assertEquals(1, configuration.quantity)
        assertEquals("", configuration.note)
        assertEquals(setOf("size"), configuration.selections.keys)
    }

    @Test fun `factory leaves required group pending when no default exists`() {
        val withoutDefault = product().copy(optionGroups = listOf(
            ProductOptionGroup(
                "choice", "Choice", null, SelectionRule(1, 1),
                listOf(ProductOption("one", "One", null, null, true, 0, false)), 0
            )
        ))
        val initial = ProductConfigurationFactory.create(withoutDefault)
        assertTrue(initial.selections.isEmpty())
        assertTrue(validator.validate(withoutDefault, initial) is ProductConfigurationResult.Invalid)
    }

    @Test fun `factory for product without options is immediately valid when available`() {
        val simple = product().copy(optionGroups = emptyList())
        val result = validator.validate(simple, ProductConfigurationFactory.create(simple))
        assertTrue(result is ProductConfigurationResult.Valid)
    }

    @Test fun `valid configuration trims note and calculates estimated total`() {
        val result = validator.validate(
            product(),
            ProductConfiguration(
                productId = "product",
                selections = mapOf("size" to setOf("large"), "extras" to setOf("egg")),
                quantity = 2,
                note = "  sem talheres  "
            )
        ) as ProductConfigurationResult.Valid
        with(result.configuredProduct) {
            assertEquals("merchant", merchantId)
            assertEquals(2, quantity)
            assertEquals("sem talheres", note)
            assertEquals(1_000, unitPrice.amountMinor)
            assertEquals(300, optionsPrice.amountMinor)
            assertEquals(1_300, totalUnitPrice.amountMinor)
            assertEquals(2_600, totalPrice.amountMinor)
        }
    }

    @Test fun `blank note becomes absent`() {
        val result = validator.validate(
            product(),
            ProductConfiguration("product", mapOf("size" to setOf("small")), 1, "  ")
        ) as ProductConfigurationResult.Valid
        assertNull(result.configuredProduct.note)
    }

    @Test fun `required minimum and maximum are validated`() {
        val missing = invalid(ProductConfiguration("product", emptyMap(), 1, ""))
        assertTrue(missing.any { it is ProductConfigurationError.MinimumSelectionsNotMet })
        val excessive = invalid(
            ProductConfiguration("product", mapOf("size" to setOf("small", "large")), 1, "")
        )
        assertTrue(excessive.any { it is ProductConfigurationError.MaximumSelectionsExceeded })
    }

    @Test fun `unknown group unknown option and unavailable option are rejected`() {
        val errors = invalid(
            ProductConfiguration(
                "product",
                mapOf(
                    "unknown" to setOf("value"),
                    "size" to setOf("missing"),
                    "extras" to setOf("bacon")
                ),
                1,
                ""
            )
        )
        assertTrue(errors.any { it is ProductConfigurationError.UnknownGroup })
        assertTrue(errors.any { it is ProductConfigurationError.UnknownOption })
        assertTrue(errors.any { it is ProductConfigurationError.UnavailableOption })
    }

    @Test fun `unavailable product and mismatched product id are rejected`() {
        val errors = (validator.validate(
            product().copy(availability = ProductAvailability.OutOfStock),
            ProductConfiguration("another", mapOf("size" to setOf("small")), 1, "")
        ) as ProductConfigurationResult.Invalid).errors
        assertTrue(ProductConfigurationError.ProductMismatch in errors)
        assertTrue(ProductConfigurationError.ProductUnavailable in errors)
    }

    @Test fun `required group with no available options is explicit`() {
        val product = product().copy(
            optionGroups = listOf(
                ProductOptionGroup(
                    "required", "Required", null, SelectionRule(1, 1),
                    listOf(ProductOption("blocked", "Blocked", null, null, false, 0, false)),
                    0
                )
            )
        )
        val errors = (validator.validate(
            product,
            ProductConfiguration("product", emptyMap(), 1, "")
        ) as ProductConfigurationResult.Invalid).errors
        assertTrue(ProductConfigurationError.NoAvailableOptions("required") in errors)
    }

    @Test fun `option from another group is reported as unknown in selected group`() {
        val errors = invalid(ProductConfiguration("product", mapOf("size" to setOf("egg")), 1, ""))
        assertTrue(ProductConfigurationError.UnknownOption("size", "egg") in errors)
    }

    @Test fun `optional empty group and multiple valid groups produce no errors`() {
        val result = validator.validate(
            product(),
            ProductConfiguration("product", mapOf("size" to setOf("large")), 99, "a".repeat(250))
        )
        assertTrue(result is ProductConfigurationResult.Valid)
    }

    @Test fun `errors preserve deterministic first validation cause`() {
        val result = validator.validate(
            product().copy(availability = ProductAvailability.Unavailable),
            ProductConfiguration("wrong", emptyMap(), 1, "")
        ) as ProductConfigurationResult.Invalid
        assertEquals(ProductConfigurationError.ProductMismatch, result.errors.first())
    }

    private fun invalid(configuration: ProductConfiguration) =
        (validator.validate(product(), configuration) as ProductConfigurationResult.Invalid).errors

    private fun product() = CatalogProduct(
        id = "product",
        merchantId = "merchant",
        categoryId = "category",
        name = "Product",
        shortDescription = null,
        fullDescription = null,
        imageUrl = null,
        basePrice = MoneyAmount(1_000, "AOA"),
        compareAtPrice = null,
        availability = ProductAvailability.Available,
        preparationMinutes = 10,
        tags = emptySet(),
        optionGroups = listOf(
            ProductOptionGroup(
                id = "size",
                name = "Size",
                description = null,
                selectionRule = SelectionRule(1, 1),
                options = listOf(
                    ProductOption("small", "Small", null, MoneyAmount(0, "AOA"), true, 0, true),
                    ProductOption("large", "Large", null, MoneyAmount(200, "AOA"), true, 1, false)
                ),
                sortOrder = 0
            ),
            ProductOptionGroup(
                id = "extras",
                name = "Extras",
                description = null,
                selectionRule = SelectionRule(0, 2),
                options = listOf(
                    ProductOption("egg", "Egg", null, MoneyAmount(100, "AOA"), true, 0, false),
                    ProductOption("bacon", "Bacon", null, MoneyAmount(150, "AOA"), false, 1, false)
                ),
                sortOrder = 1
            )
        ),
        featured = false,
        sortOrder = 0
    )
}
