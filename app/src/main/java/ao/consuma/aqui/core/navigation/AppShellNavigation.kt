package ao.consuma.aqui.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ao.consuma.aqui.core.appstate.ConsumaAppState
import ao.consuma.aqui.feature.about.AboutScreen
import ao.consuma.aqui.feature.developer.DesignSystemCatalogScreen
import ao.consuma.aqui.feature.help.HelpScreen
import ao.consuma.aqui.feature.home.presentation.HomeRoute
import ao.consuma.aqui.feature.discovery.presentation.merchant.MerchantOverviewRoute
import ao.consuma.aqui.feature.catalog.presentation.catalog.CatalogRoute
import ao.consuma.aqui.feature.catalog.presentation.product.ProductDetailRoute
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.location.LocationSettingsScreen
import ao.consuma.aqui.feature.location.LocationSetupMode
import ao.consuma.aqui.feature.location.LocationSetupRoute
import ao.consuma.aqui.feature.more.MoreScreen
import ao.consuma.aqui.feature.orders.OrdersScreen
import ao.consuma.aqui.feature.search.SearchRoute
import ao.consuma.aqui.feature.settings.SettingsScreen

@Composable
fun AppShellNavigation(
    appState: ConsumaAppState,
    isDesignSystemCatalogEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val configuredMessage = stringResource(R.string.product_configured_message)
    NavHost(
        navController = appState.navController,
        startDestination = AppDestination.Home.route,
        modifier = modifier
    ) {
        composable(AppDestination.Home.route) {
            HomeRoute(
                onNavigateToLocationSettings = { appState.navigateTo(AppDestination.LocationSettings) },
                onNavigateToMerchant = { merchantId ->
                    appState.navController.navigate(AppDestination.merchantOverview(merchantId))
                },
                onNavigateToSearch = { appState.navigateToTopLevelDestination(AppDestination.Search) }
            )
        }
        composable(AppDestination.Search.route) {
            SearchRoute(
                onNavigateToMerchant = { merchantId ->
                    appState.navController.navigate(AppDestination.merchantOverview(merchantId))
                }
            )
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
        composable(
            route = AppDestination.MerchantOverview.route,
            arguments = listOf(navArgument("merchantId") { type = NavType.StringType })
        ) {
            MerchantOverviewRoute(
                onNavigateBack = appState::navigateBack,
                onNavigateToCatalog = { merchantId ->
                    appState.navController.navigate(AppDestination.catalog(merchantId))
                }
            )
        }
        composable(
            route = AppDestination.Catalog.route,
            arguments = listOf(navArgument("merchantId") { type = NavType.StringType })
        ) { entry ->
            CatalogRoute(
                merchantId = entry.arguments?.getString("merchantId").orEmpty(),
                onNavigateBack = appState::navigateBack,
                onNavigateToProduct = { merchantId, productId ->
                    appState.navController.navigate(AppDestination.productDetail(merchantId, productId)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = AppDestination.ProductDetail.route,
            arguments = listOf(
                navArgument("merchantId") { type = NavType.StringType },
                navArgument("productId") { type = NavType.StringType }
            )
        ) { entry ->
            ProductDetailRoute(
                merchantId = entry.arguments?.getString("merchantId").orEmpty(),
                productId = entry.arguments?.getString("productId").orEmpty(),
                onNavigateBack = appState::navigateBack,
                onProductConfigured = {
                    appState.showSnackbar(configuredMessage)
                    appState.navigateBack()
                }
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
