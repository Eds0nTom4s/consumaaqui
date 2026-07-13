package ao.consuma.aqui.core.appstate

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ao.consuma.aqui.core.navigation.AppDestination
import ao.consuma.aqui.core.navigation.topLevelDestinations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class ConsumaAppState(
    val navController: NavHostController,
    val snackbarHostState: SnackbarHostState,
    val coroutineScope: CoroutineScope
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState()
            .value
            ?.destination

    val currentTopLevelDestination: AppDestination?
        @Composable get() = currentDestination?.route?.let { route ->
            topLevelDestinations.find { it.destination.route == route }?.destination
        }

    val isBottomNavigationVisible: Boolean
        @Composable get() = when (currentDestination?.route) {
            AppDestination.Home.route,
            AppDestination.Search.route,
            AppDestination.Orders.route,
            AppDestination.More.route -> true
            AppDestination.Onboarding.route,
            AppDestination.LocationSetupInitial.route,
            AppDestination.LocationSetupEdit.route,
            AppDestination.LocationSettings.route,
            AppDestination.Splash.route,
            AppDestination.Initialization.route,
            AppDestination.AppShell.route -> false
            else -> false
        }

    val topLevelDestinations: List<ao.consuma.aqui.core.navigation.TopLevelDestination>
        get() = ao.consuma.aqui.core.navigation.topLevelDestinations

    fun navigateToTopLevelDestination(destination: AppDestination) {
        navController.navigate(destination.route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateTo(destination: AppDestination) {
        navController.navigate(destination.route)
    }

    fun navigateBack() {
        navController.popBackStack()
    }

    fun showSnackbar(message: String, actionLabel: String? = null) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message, actionLabel)
        }
    }
}

@Composable
fun rememberConsumaAppState(
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): ConsumaAppState = remember(navController, snackbarHostState, coroutineScope) {
    ConsumaAppState(navController, snackbarHostState, coroutineScope)
}
