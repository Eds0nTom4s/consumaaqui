package ao.consuma.aqui.core.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import ao.consuma.aqui.R
import ao.consuma.aqui.core.appstate.ConsumaAppState

@Composable
fun ConsumaBottomNavigation(
    appState: ConsumaAppState,
    modifier: Modifier = Modifier
) {
    val currentDestination = appState.currentTopLevelDestination

    NavigationBar(
        modifier = modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION)
    ) {
        appState.topLevelDestinations.forEach { destination ->
            val selected = currentDestination == destination.destination
            val label = stringResource(destination.label)
            val selectedDescription = stringResource(R.string.nav_selected_description, label)
            val unselectedDescription = stringResource(R.string.nav_unselected_description, label)
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null
                    )
                },
                label = { Text(text = label) },
                selected = selected,
                onClick = { appState.navigateToTopLevelDestination(destination.destination) },
                modifier = Modifier
                    .testTag(destination.testTag)
                    .semantics {
                        contentDescription = label
                        stateDescription = if (selected) selectedDescription else unselectedDescription
                    }
            )
        }
    }
}
