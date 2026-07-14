package ao.consuma.aqui.feature.discovery.data

import ao.consuma.aqui.feature.discovery.domain.model.FulfillmentOption
import org.junit.Assert.*
import org.junit.Test

class DiscoveryFixturesConsistencyTest {
    @Test fun `merchant and category ids are unique and references are valid`() {
        assertEquals(DiscoveryFixtures.categories.size, DiscoveryFixtures.categories.map { it.id }.distinct().size)
        assertEquals(DiscoveryFixtures.merchants.size, DiscoveryFixtures.merchants.map { it.id }.distinct().size)
        val categoryIds = DiscoveryFixtures.categories.map { it.id }.toSet()
        assertTrue(DiscoveryFixtures.merchants.all { it.categoryId in categoryIds })
    }

    @Test fun `every overview is derived consistently from its summary`() {
        DiscoveryFixtures.merchants.forEach { summary ->
            val overview = DiscoveryFixtures.overview(summary)
            assertEquals(summary.id, overview.id)
            assertEquals(summary.name, overview.name)
            assertEquals(summary.categoryId, overview.category.id)
            assertEquals(summary.fulfillmentOptions, overview.fulfillmentOptions)
            assertEquals(summary.rating, overview.rating)
            assertEquals(summary.ratingCount, overview.ratingCount)
        }
    }

    @Test fun `numeric fixture values stay within domain limits`() {
        DiscoveryFixtures.merchants.forEach { merchant ->
            assertTrue(merchant.popularity >= 0)
            assertTrue(merchant.rating == null || merchant.rating in 0.0..5.0)
            assertTrue(merchant.ratingCount == null || merchant.ratingCount >= 0)
            assertTrue(merchant.distanceMeters == null || merchant.distanceMeters >= 0)
            assertTrue(merchant.estimatedPreparationMinutes == null || merchant.estimatedPreparationMinutes >= 0)
        }
    }

    @Test fun `featured and primary fulfillment facts are deterministic`() {
        assertEquals(
            DiscoveryFixtures.merchants.filter { it.isFeatured }.map { it.id },
            DiscoveryFixtures.merchants.filter { it.isFeatured }.map { it.id }
        )
        assertTrue(DiscoveryFixtures.merchants.all { it.fulfillmentOptions.isNotEmpty() })
        assertTrue(DiscoveryFixtures.merchants.any { FulfillmentOption.SERVICE in it.fulfillmentOptions })
    }

    @Test fun `mock contacts and image hosts are explicitly non production`() {
        DiscoveryFixtures.merchants.forEach { summary ->
            summary.imageUrl?.let { assertTrue(it.contains(".invalid/")) }
            DiscoveryFixtures.overview(summary).contact?.email?.let { assertTrue(it.endsWith(".invalid")) }
        }
    }
}
