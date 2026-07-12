package ao.consuma.aqui.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ao.consuma.aqui.core.appstate.ConsumaAppState
import ao.consuma.aqui.core.appstate.rememberConsumaAppState
import ao.consuma.aqui.feature.bootstrap.InitializationGateway
import ao.consuma.aqui.feature.bootstrap.SplashScreen
import ao.consuma.aqui.feature.location.LocationSetupRoute
import ao.consuma.aqui.feature.onboarding.OnboardingScreen

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
                onNavigateToOnboarding = {
                    rootNavController.navigate(AppDestination.Onboarding.route) {
                        popUpTo(AppDestination.Initialization.route) { inclusive = true }
                    }
                },
                onNavigateToAppShell = {
                    rootNavController.navigate(AppDestination.AppShell.route) {
                        popUpTo(AppDestination.Initialization.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    rootNavController.navigate(AppDestination.LocationSetup.route) {
                        popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.LocationSetup.route) {
            LocationSetupRoute(
                onSetupComplete = {
                    rootNavController.navigate(AppDestination.AppShell.route) {
                        popUpTo(AppDestination.LocationSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.AppShell.route) {
            val appState = rememberConsumaAppState()
            ConsumaAppShell(
                appState = appState,
                isDesignSystemCatalogEnabled = isDesignSystemCatalogEnabled,
                onNavigateToLocationSetup = {
                    rootNavController.navigate(AppDestination.LocationSetup.route) {
                        popUpTo(AppDestination.AppShell.route) { inclusive = false }
                    }
                }
            )
        }
    }
}
