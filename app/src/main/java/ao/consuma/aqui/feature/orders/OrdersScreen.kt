package ao.consuma.aqui.feature.orders

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags

@Composable
fun OrdersScreen(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.ORDERS),
        topBar = {
            ConsumaTopAppBar(title = stringResource(R.string.orders_title))
        }
    ) { paddingValues ->
        ConsumaEmptyState(
            title = stringResource(R.string.orders_empty_title),
            description = stringResource(R.string.orders_empty_description),
            actionText = stringResource(R.string.orders_back_to_home),
            onActionClick = onNavigateToHome,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(ConsumaSpacing.lg)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OrdersScreenPreview() {
    ConsumaAquiTheme {
        OrdersScreen(onNavigateToHome = {})
    }
}
