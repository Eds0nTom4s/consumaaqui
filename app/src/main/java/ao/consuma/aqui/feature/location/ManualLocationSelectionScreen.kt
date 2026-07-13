package ao.consuma.aqui.feature.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaSearchField
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.launch.domain.MockLocation

@Composable
fun ManualLocationSelectionScreen(
    query: String,
    locations: List<MockLocation>,
    selectedLocation: MockLocation?,
    onQueryChange: (String) -> Unit,
    onLocationSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.LOCATION_SETUP),
        topBar = {
            ConsumaTopAppBar(
                title = stringResource(R.string.location_manual_title),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(ConsumaSpacing.lg),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
            ) {
                Text(
                    text = stringResource(R.string.location_manual_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ConsumaSearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    placeholder = stringResource(R.string.location_search_placeholder),
                    modifier = Modifier.testTag(NavigationTestTags.LOCATION_SEARCH_FIELD)
                )
                if (locations.isEmpty()) {
                    ConsumaEmptyState(
                        title = stringResource(R.string.location_search_empty_title),
                        description = stringResource(R.string.location_search_empty_description),
                        actionText = stringResource(R.string.location_search_clear),
                        onActionClick = { onQueryChange("") }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.testTag(NavigationTestTags.LOCATION_LIST),
                        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)
                    ) {
                        items(locations, key = { it.id }) { location ->
                            LocationListItem(
                                location = location,
                                isSelected = selectedLocation?.id == location.id,
                                onClick = { onLocationSelected(location.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(ConsumaSpacing.md))
            ConsumaPrimaryButton(
                text = stringResource(R.string.location_confirm),
                onClick = onConfirm,
                fullWidth = true,
                enabled = selectedLocation != null,
                modifier = Modifier.testTag(NavigationTestTags.LOCATION_CONFIRM)
            )
        }
    }
}

@Composable
private fun LocationListItem(
    location: MockLocation,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(location.displayName) },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null
            )
        },
        trailingContent = {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(NavigationTestTags.LOCATION_LIST_ITEM)
    )
}

@Preview(showBackground = true)
@Composable
private fun ManualLocationSelectionScreenPreview() {
    ConsumaAquiTheme {
        ManualLocationSelectionScreen(
            query = "",
            locations = listOf(
                MockLocation("maianga", "Luanda", "Maianga", "Luanda — Maianga"),
                MockLocation("talatona", "Luanda", "Talatona", "Luanda — Talatona")
            ),
            selectedLocation = null,
            onQueryChange = {},
            onLocationSelected = {},
            onConfirm = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ManualLocationSelectionEmptyPreview() {
    ConsumaAquiTheme {
        ManualLocationSelectionScreen(
            query = "xyz",
            locations = emptyList(),
            selectedLocation = null,
            onQueryChange = {},
            onLocationSelected = {},
            onConfirm = {},
            onNavigateBack = {}
        )
    }
}
