package ao.consuma.aqui.feature.discovery.presentation.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags

@Composable
fun CatalogPlaceholderScreen(merchantId: String, onNavigateBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.testTag(NavigationTestTags.CATALOG_PLACEHOLDER),
        topBar = { ConsumaTopAppBar(stringResource(R.string.catalog_placeholder_title), onBackClick = onNavigateBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(ConsumaSpacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.catalog_placeholder_message), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(ConsumaSpacing.sm))
            Text(
                if (merchantId.isBlank()) stringResource(R.string.catalog_placeholder_invalid_id)
                else stringResource(R.string.home_merchant_placeholder_id, merchantId),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(ConsumaSpacing.lg))
            ConsumaPrimaryButton(stringResource(R.string.back), onNavigateBack, fullWidth = true)
        }
    }
}

@Preview(showBackground = true)
@Composable private fun CatalogPlaceholderPreview() {
    ConsumaAquiTheme { CatalogPlaceholderScreen("sabor-maianga", {}) }
}
