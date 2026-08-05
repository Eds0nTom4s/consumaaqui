package ao.consuma.aqui.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import ao.consuma.aqui.feature.developer.DiscoverySourceViewModel
import ao.consuma.aqui.feature.help.HelpScreen
import ao.consuma.aqui.feature.home.presentation.HomeRoute
import ao.consuma.aqui.feature.discovery.presentation.merchant.MerchantOverviewRoute
import ao.consuma.aqui.feature.catalog.presentation.catalog.CatalogRoute
import ao.consuma.aqui.feature.catalog.presentation.product.ProductDetailRoute
import ao.consuma.aqui.feature.cart.presentation.cart.CartRoute
import ao.consuma.aqui.feature.checkout.presentation.checkout.CheckoutRoute
import ao.consuma.aqui.feature.checkout.presentation.confirmation.CheckoutConfirmationRoute
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.location.LocationSettingsScreen
import ao.consuma.aqui.feature.location.LocationSetupMode
import ao.consuma.aqui.feature.location.LocationSetupRoute
import ao.consuma.aqui.feature.more.MoreScreen
import ao.consuma.aqui.feature.orders.OrdersScreen
import ao.consuma.aqui.feature.search.SearchRoute
import ao.consuma.aqui.feature.settings.SettingsScreen
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppShellNavigation(
    appState: ConsumaAppState,
    isDesignSystemCatalogEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val navigateToCart = { appState.navController.navigate(AppDestination.Cart.route) { launchSingleTop = true } }
    val cartPreservedMessage = stringResource(R.string.checkout_cart_preserved_message)
    val returnToCart = {
        if (!appState.navController.popBackStack(AppDestination.Cart.route, inclusive = false)) {
            appState.navController.navigate(AppDestination.Cart.route) { launchSingleTop = true }
        }
    }
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
                onNavigateToSearch = { appState.navigateToTopLevelDestination(AppDestination.Search) },
                onNavigateToCart = navigateToCart
            )
        }
        composable(AppDestination.Search.route) {
            SearchRoute(
                onNavigateToMerchant = { merchantId ->
                    appState.navController.navigate(AppDestination.merchantOverview(merchantId))
                },
                onNavigateToCart = navigateToCart
            )
        }
        composable(AppDestination.Orders.route) {
            OrdersScreen(
                onNavigateToHome = { appState.navigateToTopLevelDestination(AppDestination.Home) }
            )
        }
        composable(AppDestination.More.route) {
            val discoverySourceViewModel: DiscoverySourceViewModel = hiltViewModel()
            val discoverySource by discoverySourceViewModel.source.collectAsStateWithLifecycle()
            MoreScreen(
                onNavigateToSettings = { appState.navigateTo(AppDestination.Settings) },
                onNavigateToHelp = { appState.navigateTo(AppDestination.Help) },
                onNavigateToAbout = { appState.navigateTo(AppDestination.About) },
                onNavigateToLocationSettings = { appState.navigateTo(AppDestination.LocationSettings) },
                onNavigateToDesignSystem = if (isDesignSystemCatalogEnabled) {
                    { appState.navigateTo(AppDestination.DesignSystemCatalog) }
                } else null,
                isDesignSystemCatalogEnabled = isDesignSystemCatalogEnabled,
                discoverySourceSelectable = discoverySourceViewModel.selectable,
                discoverySource = discoverySource,
                onDiscoverySourceSelected = discoverySourceViewModel::select
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
                },
                onNavigateToCart = navigateToCart
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
                },
                onNavigateToCart = navigateToCart
            )
        }
        composable(
            route = AppDestination.ProductDetail.route,
            arguments = listOf(
                navArgument("merchantId") { type = NavType.StringType },
                navArgument("productId") { type = NavType.StringType },
                navArgument("cartItemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            ProductDetailRoute(
                merchantId = entry.arguments?.getString("merchantId").orEmpty(),
                productId = entry.arguments?.getString("productId").orEmpty(),
                onNavigateBack = appState::navigateBack,
                onCartResult = {},
                onShowMessage = appState::showSnackbar,
                onSuccess = appState::navigateBack,
                onNavigateToCart = navigateToCart
            )
        }
        composable(AppDestination.Cart.route) {
            CartRoute(
                onNavigateBack = appState::navigateBack,
                onExploreMerchants = { appState.navigateToTopLevelDestination(AppDestination.Home) },
                onContinueShopping = { merchantId ->
                    appState.navController.navigate(AppDestination.catalog(merchantId)) {
                        popUpTo(AppDestination.Cart.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToCheckout = {
                    appState.navController.navigate(AppDestination.Checkout.route) {
                        launchSingleTop = true
                    }
                },
                onEditItem = { merchantId, productId, cartItemId ->
                    appState.navController.navigate(
                        AppDestination.productDetail(merchantId, productId, cartItemId)
                    ) { launchSingleTop = true }
                },
                onShowMessage = appState::showSnackbar
            )
        }
        composable(AppDestination.Checkout.route) {
            CheckoutRoute(
                onNavigateBack = appState::navigateBack,
                onNavigateToCart = returnToCart,
                onNavigateToConfirmation = {
                    appState.navController.navigate(AppDestination.CheckoutConfirmation.route) {
                        popUpTo(AppDestination.Checkout.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onShowMessage = appState::showSnackbar
            )
        }
        composable(AppDestination.CheckoutConfirmation.route) {
            CheckoutConfirmationRoute(
                onNavigateToCart = {
                    returnToCart()
                    appState.showSnackbar(cartPreservedMessage)
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
