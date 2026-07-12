package ao.consuma.aqui.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaSearchField
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.SEARCH),
        topBar = {
            ConsumaTopAppBar(title = stringResource(R.string.search_title))
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
            ConsumaSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(R.string.search_placeholder)
            )
            if (query.isBlank()) {
                ConsumaEmptyState(
                    title = stringResource(R.string.search_empty_title),
                    description = stringResource(R.string.search_empty_description)
                )
            } else {
                Text(
                    text = stringResource(R.string.search_query_preview, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    ConsumaAquiTheme {
        SearchScreen()
    }
}
