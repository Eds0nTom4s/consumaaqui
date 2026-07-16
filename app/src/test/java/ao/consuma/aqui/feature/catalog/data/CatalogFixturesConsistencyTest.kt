package ao.consuma.aqui.feature.catalog.data

import ao.consuma.aqui.feature.catalog.domain.model.ProductAvailability
import ao.consuma.aqui.feature.discovery.data.DiscoveryFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFixturesConsistencyTest {
    @Test fun `catalog merchants are existing discovery merchants`() {
        val discoveryIds = DiscoveryFixtures.merchants.map { it.id }.toSet()
        assertEquals(discoveryIds, CatalogFixtures.knownMerchantIds)
        assertTrue(CatalogFixtures.catalogs.all { it.merchantId in discoveryIds })
    }

    @Test fun `catalog availability agrees with merchant overviews`() {
        DiscoveryFixtures.merchants.forEach { merchant ->
            val overview = DiscoveryFixtures.overview(merchant)
            if (overview.catalogAvailable) {
                assertTrue(
                    CatalogFixtures.catalog(merchant.id) != null ||
                        merchant.id in CatalogFixtures.catalogUnavailableMerchantIds
                )
            } else {
                assertNull(CatalogFixtures.catalog(merchant.id))
                assertTrue(merchant.id in CatalogFixtures.catalogUnavailableMerchantIds)
            }
        }
    }

    @Test fun `at least four catalogs have four categories and three products each`() {
        val complete = CatalogFixtures.catalogs.filter { catalog ->
            catalog.categories.size >= 4 &&
                catalog.categories.all { category ->
                    catalog.products.count { it.categoryId == category.id } >= 3
                }
        }
        assertTrue(complete.size >= 4)
    }

    @Test fun `category taxonomies vary by merchant`() {
        val taxonomies = CatalogFixtures.catalogs
            .filter { it.products.isNotEmpty() }
            .map { catalog -> catalog.categories.map { it.name }.toSet() }
        assertTrue(taxonomies.distinct().size > 3)
    }

    @Test fun `ids ownership currency and ordering facts are coherent`() {
        CatalogFixtures.catalogs.forEach { catalog ->
            assertEquals(catalog.categories.size, catalog.categories.map { it.id }.distinct().size)
            assertEquals(catalog.products.size, catalog.products.map { it.id }.distinct().size)
            assertTrue(catalog.categories.all { it.merchantId == catalog.merchantId })
            assertTrue(catalog.products.all { product ->
                product.merchantId == catalog.merchantId &&
                    product.basePrice.currencyCode == catalog.currencyCode &&
                    catalog.categories.any { it.id == product.categoryId }
            })
        }
    }

    @Test fun `fixtures cover required product diversity`() {
        val products = CatalogFixtures.catalogs.flatMap { it.products }
        assertTrue(products.any { it.availability == ProductAvailability.Available })
        assertTrue(products.any { it.availability != ProductAvailability.Available })
        assertTrue(products.any { it.imageUrl == null })
        assertTrue(products.any { it.imageUrl?.contains(".invalid/") == true })
        assertTrue(products.any { it.compareAtPrice != null })
        assertTrue(products.any { it.compareAtPrice == null })
        assertTrue(products.any { it.shortDescription == null || it.fullDescription == null })
        assertTrue(products.any { it.optionGroups.isNotEmpty() })
        assertTrue(products.any { it.optionGroups.isEmpty() })
        assertTrue(products.any { it.preparationMinutes == null })
        assertTrue(products.any { it.featured })
        assertTrue(products.any { product -> product.optionGroups.any { group -> group.options.any { !it.available } } })
        assertFalse(products.any { it.basePrice.amountMinor < 0 })
    }

    @Test fun `official fixture totals and publication coverage remain stable`() {
        assertEquals(7, CatalogFixtures.catalogs.size)
        assertEquals(21, CatalogFixtures.catalogs.sumOf { it.categories.size })
        assertEquals(56, CatalogFixtures.catalogs.sumOf { it.products.size })
        assertEquals(CatalogFixtures.catalogs.size, CatalogFixtures.catalogs.map { it.catalogId }.distinct().size)
        assertTrue(CatalogFixtures.catalogs.count { catalog ->
            catalog.categories.size >= 4 && catalog.categories.all { category ->
                catalog.products.count { it.categoryId == category.id } >= 3
            }
        } >= 4)
    }

    @Test fun `option identifiers ordering and monetary facts are globally coherent`() {
        CatalogFixtures.catalogs.flatMap { it.products }.forEach { product ->
            assertTrue(product.sortOrder >= 0)
            assertTrue(product.preparationMinutes == null || product.preparationMinutes >= 0)
            product.compareAtPrice?.let { assertTrue(it.amountMinor > product.basePrice.amountMinor) }
            product.optionGroups.forEach { group ->
                assertEquals(group.options.size, group.options.map { it.id }.distinct().size)
                assertTrue(group.sortOrder >= 0)
                assertTrue(group.options.all { option ->
                    option.sortOrder >= 0 && option.additionalPrice?.currencyCode in setOf(null, product.basePrice.currencyCode)
                })
            }
        }
    }
}
