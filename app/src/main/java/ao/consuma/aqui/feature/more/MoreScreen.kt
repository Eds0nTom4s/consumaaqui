package ao.consuma.aqui.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.BuildConfig
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaClickableCard
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags

@Composable
fun MoreScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToLocationSettings: () -> Unit,
    onNavigateToDesignSystem: (() -> Unit)?,
    isDesignSystemCatalogEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.MORE),
        topBar = {
            ConsumaTopAppBar(title = stringResource(R.string.more_title))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(ConsumaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
        ) {
            ConsumaClickableCard(
                onClick = onNavigateToLocationSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NavigationTestTags.MORE_LOCATION)
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.more_location)) },
                    leadingContent = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null
                        )
                    }
                )
            }
            ConsumaClickableCard(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.more_settings)) },
                    leadingContent = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null
                        )
                    }
                )
            }
            ConsumaClickableCard(
                onClick = onNavigateToHelp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.more_help)) },
                    leadingContent = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null
                        )
                    }
                )
            }
            ConsumaClickableCard(
                onClick = onNavigateToAbout,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.more_about)) },
                    leadingContent = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null
                        )
                    }
                )
            }

            if (isDesignSystemCatalogEnabled) {
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.more_developer_tools_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (onNavigateToDesignSystem != null) {
                    ConsumaClickableCard(
                        onClick = onNavigateToDesignSystem,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.more_design_system)) },
                            leadingContent = {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
                ConsumaClickableCard(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.more_build_info)) },
                        supportingContent = {
                            Column {
                                Text(stringResource(R.string.environment_label, BuildConfig.ENVIRONMENT))
                                Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME))
                                Text(stringResource(R.string.version_code_label, BuildConfig.VERSION_CODE))
                            }
                        },
                        leadingContent = {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoreScreenPreview() {
    ConsumaAquiTheme {
        MoreScreen(
            onNavigateToSettings = {},
            onNavigateToHelp = {},
            onNavigateToAbout = {},
            onNavigateToLocationSettings = {},
            onNavigateToDesignSystem = {},
            isDesignSystemCatalogEnabled = true
        )
    }
}
