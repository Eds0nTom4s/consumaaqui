package ao.consuma.aqui.core.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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
        assertFalse(AppDestination.Catalog.isTopLevel())
        assertFalse(AppDestination.ProductDetail.isTopLevel())
        assertFalse(AppDestination.Cart.isTopLevel())
        assertFalse(AppDestination.DesignSystemCatalog.isTopLevel())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `catalog route rejects blank merchant id`() {
        AppDestination.catalog("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `product route rejects blank product id`() {
        AppDestination.productDetail("merchant", "")
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

    @Test
    fun `cart is secondary and checkout destination is absent`() {
        assertTrue(AppDestination.all.none { destination ->
            destination.route.contains("checkout", ignoreCase = true)
        })
        assertTrue(AppDestination.Cart in AppDestination.all)
        assertFalse(AppDestination.Catalog.isTopLevel())
        assertFalse(AppDestination.ProductDetail.isTopLevel())
        assertFalse(AppDestination.Cart.isTopLevel())
    }

}
