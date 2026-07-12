package ao.consuma.aqui.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ao.consuma.aqui.feature.bootstrap.SplashScreen
import ao.consuma.aqui.feature.placeholder.InstitutionalScreen
import ao.consuma.aqui.feature.placeholder.DesignSystemCatalogScreen
import ao.consuma.aqui.BuildConfig

@Composable
fun ConsumaAquiNavHost(navController: NavHostController) {
    val environment = BuildConfig.ENVIRONMENT
    val destinationAfterSplash = NavigationPolicy.destinationAfterSplash(environment)
    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash.route
    ) {
        composable(AppDestination.Splash.route) {
            SplashScreen(
                onNavigateToNext = {
                    navController.navigate(destinationAfterSplash.route) {
                        popUpTo(AppDestination.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.Institutional.route) {
            InstitutionalScreen()
        }
        if (NavigationPolicy.isDesignSystemCatalogEnabled(environment)) {
            composable(AppDestination.DesignSystemCatalog.route) {
                DesignSystemCatalogScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
