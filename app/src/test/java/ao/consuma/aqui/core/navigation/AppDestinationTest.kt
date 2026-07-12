package ao.consuma.aqui.core.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {

    @Test
    fun `home search orders and more are top level destinations`() {
        assertTrue(AppDestination.Home.isTopLevel())
        assertTrue(AppDestination.Search.isTopLevel())
        assertTrue(AppDestination.Orders.isTopLevel())
        assertTrue(AppDestination.More.isTopLevel())
    }

    @Test
    fun `secondary destinations are not top level`() {
        assertFalse(AppDestination.Settings.isTopLevel())
        assertFalse(AppDestination.Help.isTopLevel())
        assertFalse(AppDestination.About.isTopLevel())
        assertFalse(AppDestination.DesignSystemCatalog.isTopLevel())
    }

    @Test
    fun `root and shell destinations are not top level`() {
        assertFalse(AppDestination.Splash.isTopLevel())
        assertFalse(AppDestination.Initialization.isTopLevel())
        assertFalse(AppDestination.AppShell.isTopLevel())
    }

    @Test
    fun `bottom navigation is visible only on top level destinations`() {
        assertTrue(AppDestination.Home.bottomNavigationVisible())
        assertTrue(AppDestination.Search.bottomNavigationVisible())
        assertTrue(AppDestination.Orders.bottomNavigationVisible())
        assertTrue(AppDestination.More.bottomNavigationVisible())
        assertFalse(AppDestination.Settings.bottomNavigationVisible())
        assertFalse(AppDestination.Help.bottomNavigationVisible())
        assertFalse(AppDestination.About.bottomNavigationVisible())
        assertFalse(AppDestination.DesignSystemCatalog.bottomNavigationVisible())
    }

    @Test
    fun `top level destinations contain four items`() {
        assertEquals(4, topLevelDestinations.size)
    }

    companion object {
        private fun assertEquals(expected: Int, actual: Int) {
            org.junit.Assert.assertEquals(expected, actual)
        }
    }
}
