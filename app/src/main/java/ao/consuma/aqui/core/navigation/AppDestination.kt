package ao.consuma.aqui.core.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import ao.consuma.aqui.R

sealed class AppDestination(val route: String) {
    data object Splash : AppDestination("splash")
    data object Initialization : AppDestination("initialization")
    data object Onboarding : AppDestination("onboarding")
    data object LocationSetupInitial : AppDestination("location_setup_initial")
    data object LocationSetupEdit : AppDestination("location_setup_edit")
    data object AppShell : AppDestination("app_shell")
    data object Home : AppDestination("home")
    data object Search : AppDestination("search")
    data object Orders : AppDestination("orders")
    data object More : AppDestination("more")
    data object Settings : AppDestination("settings")
    data object Help : AppDestination("help")
    data object About : AppDestination("about")
    data object LocationSettings : AppDestination("location_settings")
    data object MerchantOverview : AppDestination("merchant/{merchantId}")
    data object CatalogPlaceholder : AppDestination("merchant/{merchantId}/catalog")
    data object DesignSystemCatalog : AppDestination("design_system_catalog")

    companion object {
        val all: List<AppDestination> = listOf(
            Splash,
            Initialization,
            Onboarding,
            LocationSetupInitial,
            LocationSetupEdit,
            AppShell,
            Home,
            Search,
            Orders,
            More,
            Settings,
            Help,
            About,
            LocationSettings,
            MerchantOverview,
            CatalogPlaceholder,
            DesignSystemCatalog
        )

        fun merchantOverview(merchantId: String): String = "merchant/${Uri.encode(merchantId)}"
        fun catalogPlaceholder(merchantId: String): String = "merchant/${Uri.encode(merchantId)}/catalog"
    }
}

data class TopLevelDestination(
    val destination: AppDestination,
    @StringRes val label: Int,
    val icon: ImageVector,
    val testTag: String
)

val topLevelDestinations = listOf(
    TopLevelDestination(
        destination = AppDestination.Home,
        label = R.string.nav_home,
        icon = Icons.Default.Home,
        testTag = NavigationTestTags.BOTTOM_NAV_HOME
    ),
    TopLevelDestination(
        destination = AppDestination.Search,
        label = R.string.nav_search,
        icon = Icons.Default.Search,
        testTag = NavigationTestTags.BOTTOM_NAV_SEARCH
    ),
    TopLevelDestination(
        destination = AppDestination.Orders,
        label = R.string.nav_orders,
        icon = Icons.Default.ShoppingCart,
        testTag = NavigationTestTags.BOTTOM_NAV_ORDERS
    ),
    TopLevelDestination(
        destination = AppDestination.More,
        label = R.string.nav_more,
        icon = Icons.Default.Menu,
        testTag = NavigationTestTags.BOTTOM_NAV_MORE
    )
)

fun AppDestination.isTopLevel(): Boolean =
    this in topLevelDestinations.map { it.destination }

fun AppDestination.bottomNavigationVisible(): Boolean = isTopLevel()
