package ao.consuma.aqui.feature.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaCard
import ao.consuma.aqui.core.designsystem.components.ConsumaClickableCard
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTextButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.launch.domain.LocationPreference
import ao.consuma.aqui.feature.launch.domain.MockLocation

@Composable
fun LocationSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLocationSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationSettingsViewModel = hiltViewModel()
) {
    val locationPreference by viewModel.locationPreference.collectAsState()

    LocationSettingsContent(
        locationPreference = locationPreference,
        onNavigateBack = onNavigateBack,
        onNavigateToLocationSetup = onNavigateToLocationSetup,
        onRemoveLocation = viewModel::removeLocation,
        modifier = modifier
    )
}

@Composable
private fun LocationSettingsContent(
    locationPreference: LocationPreference,
    onNavigateBack: () -> Unit,
    onNavigateToLocationSetup: () -> Unit,
    onRemoveLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.LOCATION_SETTINGS),
        topBar = {
            ConsumaTopAppBar(
                title = stringResource(R.string.location_settings_title),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(ConsumaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (locationPreference) {
                is LocationPreference.Selected -> {
                    Text(
                        text = stringResource(R.string.location_current_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    ConsumaCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = { Text(locationPreference.location.displayName) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(ConsumaSpacing.md))
                    ConsumaPrimaryButton(
                        text = stringResource(R.string.location_settings_change),
                        onClick = onNavigateToLocationSetup,
                        fullWidth = true,
                        modifier = Modifier.testTag(NavigationTestTags.LOCATION_SETTINGS_CHANGE)
                    )
                    ConsumaTextButton(
                        text = stringResource(R.string.location_settings_remove),
                        onClick = onRemoveLocation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(NavigationTestTags.LOCATION_SETTINGS_REMOVE)
                    )
                }
                else -> {
                    ConsumaEmptyState(
                        title = stringResource(R.string.location_not_configured_title),
                        description = stringResource(R.string.location_not_configured_description),
                        actionText = stringResource(R.string.location_settings_choose),
                        onActionClick = onNavigateToLocationSetup
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSettingsWithLocationPreview() {
    ConsumaAquiTheme {
        LocationSettingsContent(
            locationPreference = LocationPreference.Selected(
                MockLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga")
            ),
            onNavigateBack = {},
            onNavigateToLocationSetup = {},
            onRemoveLocation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSettingsWithoutLocationPreview() {
    ConsumaAquiTheme {
        LocationSettingsContent(
            locationPreference = LocationPreference.NotConfigured,
            onNavigateBack = {},
            onNavigateToLocationSetup = {},
            onRemoveLocation = {}
        )
    }
}
