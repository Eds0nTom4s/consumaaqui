package ao.consuma.aqui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaSectionHeader
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.launch.domain.LocationPreference
import ao.consuma.aqui.feature.launch.domain.MockLocation

@Composable
fun HomeScreen(
    onNavigateToLocationSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val locationPreference by viewModel.locationPreference.collectAsState()

    HomeContent(
        locationPreference = locationPreference,
        onNavigateToLocationSettings = onNavigateToLocationSettings,
        modifier = modifier
    )
}

@Composable
private fun HomeContent(
    locationPreference: LocationPreference,
    onNavigateToLocationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.HOME),
        topBar = {
            ConsumaTopAppBar(title = stringResource(R.string.app_name))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(ConsumaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_welcome),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            when (locationPreference) {
                is LocationPreference.Selected -> {
                    LocationCard(
                        displayName = locationPreference.location.displayName,
                        onChangeLocation = onNavigateToLocationSettings
                    )
                }
                else -> {
                    NoLocationCard(
                        onChooseLocation = onNavigateToLocationSettings
                    )
                }
            }

            Spacer(modifier = Modifier.height(ConsumaSpacing.lg))
            ConsumaSectionHeader(title = stringResource(R.string.home_merchants_section_title))
            Text(
                text = stringResource(R.string.home_merchants_section_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ConsumaSpacing.lg))
            ConsumaSectionHeader(title = stringResource(R.string.home_promotions_section_title))
            Text(
                text = stringResource(R.string.home_promotions_section_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationCard(
    displayName: String,
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConsumaClickableCard(
        onClick = onChangeLocation,
        modifier = modifier
            .fillMaxWidth()
            .testTag(NavigationTestTags.HOME_LOCATION_CARD)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.home_current_location)) },
            supportingContent = { Text(displayName) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null
                )
            },
            trailingContent = {
                Text(
                    text = stringResource(R.string.home_change_location),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag(NavigationTestTags.HOME_CHANGE_LOCATION)
                )
            }
        )
    }
}

@Composable
private fun NoLocationCard(
    onChooseLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConsumaCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ConsumaSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.home_no_location_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.home_no_location_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ConsumaPrimaryButton(
                text = stringResource(R.string.home_choose_location),
                onClick = onChooseLocation,
                fullWidth = true,
                modifier = Modifier.testTag(NavigationTestTags.HOME_CHOOSE_LOCATION)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithLocationPreview() {
    ConsumaAquiTheme {
        HomeContent(
            locationPreference = LocationPreference.Selected(
                MockLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga")
            ),
            onNavigateToLocationSettings = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithoutLocationPreview() {
    ConsumaAquiTheme {
        HomeContent(
            locationPreference = LocationPreference.NotConfigured,
            onNavigateToLocationSettings = {}
        )
    }
}
