package ao.consuma.aqui.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {

    @Test
    fun `destination after initialization is AppShell for all environments`() {
        listOf(
            NavigationPolicy.Environment.DEBUG,
            NavigationPolicy.Environment.STAGING,
            NavigationPolicy.Environment.RELEASE
        ).forEach { environment ->
            assertEquals(
                AppDestination.AppShell,
                NavigationPolicy.destinationAfterInitialization(environment)
            )
        }
    }

    @Test
    fun `design system catalog is enabled in debug and staging`() {
        assertTrue(NavigationPolicy.isDesignSystemCatalogEnabled("DEBUG"))
        assertTrue(NavigationPolicy.isDesignSystemCatalogEnabled("STAGING"))
    }

    @Test
    fun `design system catalog is disabled in release`() {
        assertFalse(NavigationPolicy.isDesignSystemCatalogEnabled("RELEASE"))
    }

    @Test
    fun `development tools are visible in debug and staging`() {
        assertTrue(NavigationPolicy.isDevelopmentToolsVisible("DEBUG"))
        assertTrue(NavigationPolicy.isDevelopmentToolsVisible("STAGING"))
    }

    @Test
    fun `development tools are hidden in release`() {
        assertFalse(NavigationPolicy.isDevelopmentToolsVisible("RELEASE"))
    }
}
