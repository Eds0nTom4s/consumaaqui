package ao.consuma.aqui.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ao.consuma.aqui.core.appstate.ConsumaAppState

@Composable
fun ConsumaAppShell(
    appState: ConsumaAppState,
    isDesignSystemCatalogEnabled: Boolean,
    onNavigateToLocationSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(NavigationTestTags.APP_SHELL),
        bottomBar = {
            if (appState.isBottomNavigationVisible) {
                ConsumaBottomNavigation(
                    appState = appState
                )
            }
        },
        snackbarHost = { SnackbarHost(appState.snackbarHostState) }
    ) { paddingValues ->
        AppShellNavigation(
            appState = appState,
            isDesignSystemCatalogEnabled = isDesignSystemCatalogEnabled,
            onNavigateToLocationSetup = onNavigateToLocationSetup,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
