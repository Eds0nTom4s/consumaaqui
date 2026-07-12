package ao.consuma.aqui.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ao.consuma.aqui.feature.bootstrap.SplashScreen
import ao.consuma.aqui.feature.placeholder.FoundationReadyScreen

@Composable
fun ConsumaAquiNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash.route
    ) {
        composable(AppDestination.Splash.route) {
            SplashScreen(
                onNavigateToNext = {
                    navController.navigate(AppDestination.FoundationReady.route) {
                        popUpTo(AppDestination.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.FoundationReady.route) {
            FoundationReadyScreen()
        }
    }
}
