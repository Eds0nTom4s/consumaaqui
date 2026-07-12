package ao.consuma.aqui.feature.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags

@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.HELP),
        topBar = {
            ConsumaTopAppBar(
                title = stringResource(R.string.help_title),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(ConsumaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.help_placeholder_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.help_placeholder_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelpScreenPreview() {
    ConsumaAquiTheme {
        HelpScreen(onNavigateBack = {})
    }
}
