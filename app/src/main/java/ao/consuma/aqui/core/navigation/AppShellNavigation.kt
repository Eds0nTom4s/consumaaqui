package ao.consuma.aqui.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ao.consuma.aqui.core.appstate.ConsumaAppState
import ao.consuma.aqui.feature.about.AboutScreen
import ao.consuma.aqui.feature.developer.DesignSystemCatalogScreen
import ao.consuma.aqui.feature.help.HelpScreen
import ao.consuma.aqui.feature.home.HomeScreen
import ao.consuma.aqui.feature.location.LocationSettingsScreen
import ao.consuma.aqui.feature.location.LocationSetupMode
import ao.consuma.aqui.feature.location.LocationSetupRoute
import ao.consuma.aqui.feature.more.MoreScreen
import ao.consuma.aqui.feature.orders.OrdersScreen
import ao.consuma.aqui.feature.search.SearchScreen
import ao.consuma.aqui.feature.settings.SettingsScreen

@Composable
fun AppShellNavigation(
    appState: ConsumaAppState,
    isDesignSystemCatalogEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = appState.navController,
        startDestination = AppDestination.Home.route,
        modifier = modifier
    ) {
        composable(AppDestination.Home.route) {
            HomeScreen(
                onNavigateToLocationSettings = { appState.navigateTo(AppDestination.LocationSettings) }
            )
        }
        composable(AppDestination.Search.route) {
            SearchScreen()
        }
        composable(AppDestination.Orders.route) {
            OrdersScreen(
                onNavigateToHome = { appState.navigateToTopLevelDestination(AppDestination.Home) }
            )
        }
        composable(AppDestination.More.route) {
            MoreScreen(
                onNavigateToSettings = { appState.navigateTo(AppDestination.Settings) },
                onNavigateToHelp = { appState.navigateTo(AppDestination.Help) },
                onNavigateToAbout = { appState.navigateTo(AppDestination.About) },
                onNavigateToLocationSettings = { appState.navigateTo(AppDestination.LocationSettings) },
                onNavigateToDesignSystem = if (isDesignSystemCatalogEnabled) {
                    { appState.navigateTo(AppDestination.DesignSystemCatalog) }
                } else null,
                isDesignSystemCatalogEnabled = isDesignSystemCatalogEnabled
            )
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen(
                onNavigateBack = appState::navigateBack
            )
        }
        composable(AppDestination.Help.route) {
            HelpScreen(
                onNavigateBack = appState::navigateBack
            )
        }
        composable(AppDestination.About.route) {
            AboutScreen(
                onNavigateBack = appState::navigateBack
            )
        }
        composable(AppDestination.LocationSettings.route) {
            LocationSettingsScreen(
                onNavigateBack = appState::navigateBack,
                onNavigateToLocationSetup = { appState.navigateTo(AppDestination.LocationSetupEdit) }
            )
        }
        composable(AppDestination.LocationSetupEdit.route) {
            LocationSetupRoute(
                mode = LocationSetupMode.EDIT,
                onSetupComplete = appState::navigateBack
            )
        }
        if (isDesignSystemCatalogEnabled) {
            composable(AppDestination.DesignSystemCatalog.route) {
                DesignSystemCatalogScreen(
                    onNavigateBack = appState::navigateBack
                )
            }
        }
    }
}
