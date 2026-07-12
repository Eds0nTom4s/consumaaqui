package ao.consuma.aqui.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {
    @Test
    fun `release destination after splash is institutional and catalog is disabled`() {
        assertEquals(
            AppDestination.Institutional,
            NavigationPolicy.destinationAfterSplash("RELEASE")
        )
        assertFalse(NavigationPolicy.isDesignSystemCatalogEnabled("RELEASE"))
    }

    @Test
    fun `debug and staging destination after splash is catalog`() {
        listOf("DEBUG", "STAGING").forEach { environment ->
            assertEquals(
                AppDestination.DesignSystemCatalog,
                NavigationPolicy.destinationAfterSplash(environment)
            )
            assertTrue(NavigationPolicy.isDesignSystemCatalogEnabled(environment))
        }
    }
}
