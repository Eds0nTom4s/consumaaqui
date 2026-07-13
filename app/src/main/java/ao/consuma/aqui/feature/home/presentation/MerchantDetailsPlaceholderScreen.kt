package ao.consuma.aqui.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags

@Composable
fun MerchantDetailsPlaceholderScreen(merchantId: String, onNavigateBack: () -> Unit) {
    androidx.compose.material3.Scaffold(
        modifier = Modifier.testTag(NavigationTestTags.MERCHANT_PLACEHOLDER),
        topBar = { ConsumaTopAppBar(stringResource(R.string.home_merchant_placeholder_title), onBackClick = onNavigateBack) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(ConsumaSpacing.lg), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.home_merchant_placeholder_message), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.home_merchant_placeholder_id, merchantId), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
