package ao.consuma.aqui.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ao.consuma.aqui.core.appstate.ConsumaAppState
import ao.consuma.aqui.core.appstate.rememberConsumaAppState
import ao.consuma.aqui.feature.bootstrap.InitializationGateway
import ao.consuma.aqui.feature.bootstrap.SplashScreen

@Composable
fun RootNavigation(
    rootNavController: NavHostController,
    isDesignSystemCatalogEnabled: Boolean
) {
    NavHost(
        navController = rootNavController,
        startDestination = AppDestination.Splash.route
    ) {
        composable(AppDestination.Splash.route) {
            SplashScreen(
                onNavigateToNext = {
                    rootNavController.navigate(AppDestination.Initialization.route) {
                        popUpTo(AppDestination.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.Initialization.route) {
            InitializationGateway(
                onInitializationComplete = {
                    rootNavController.navigate(AppDestination.AppShell.route) {
                        popUpTo(AppDestination.Initialization.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.AppShell.route) {
            val appState = rememberConsumaAppState()
            ConsumaAppShell(
                appState = appState,
                isDesignSystemCatalogEnabled = isDesignSystemCatalogEnabled
            )
        }
    }
}
