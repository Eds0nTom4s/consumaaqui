package ao.consuma.aqui.feature.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaAvatar
import ao.consuma.aqui.core.designsystem.components.ConsumaDivider
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaErrorState
import ao.consuma.aqui.core.designsystem.components.ConsumaFilterChip
import ao.consuma.aqui.core.designsystem.components.ConsumaInlineMessage
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingState
import ao.consuma.aqui.core.designsystem.components.ConsumaMerchantCardPlaceholder
import ao.consuma.aqui.core.designsystem.components.ConsumaOfflineBanner
import ao.consuma.aqui.core.designsystem.components.ConsumaPriceText
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaSearchField
import ao.consuma.aqui.core.designsystem.components.ConsumaSecondaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaSectionHeader
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusChip
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.components.ConsumaTextButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTextField
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags

@Composable
fun DesignSystemCatalogScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            ConsumaTopAppBar(
                title = stringResource(R.string.catalog_title),
                onBackClick = onNavigateBack
            )
        },
        modifier = Modifier.testTag(NavigationTestTags.DESIGN_SYSTEM_CATALOG)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(ConsumaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.lg)
        ) {
            ConsumaOfflineBanner()

            ConsumaSectionHeader(title = stringResource(R.string.catalog_colors_title))
            Text(text = stringResource(R.string.catalog_colors_description), style = MaterialTheme.typography.bodyMedium)

            ConsumaSectionHeader(title = stringResource(R.string.catalog_typography_title))
            Text(text = stringResource(R.string.catalog_headline_large), style = MaterialTheme.typography.headlineLarge)
            Text(text = stringResource(R.string.catalog_title_medium), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.catalog_body_medium), style = MaterialTheme.typography.bodyMedium)
            Text(text = stringResource(R.string.catalog_label_small), style = MaterialTheme.typography.labelSmall)

            ConsumaDivider()

            ConsumaSectionHeader(title = stringResource(R.string.catalog_buttons_title))
            ConsumaPrimaryButton(text = stringResource(R.string.catalog_primary_button), onClick = { }, fullWidth = true, modifier = Modifier.testTag("primary_button"))
            ConsumaSecondaryButton(text = stringResource(R.string.catalog_secondary_button), onClick = { }, fullWidth = true)
            ConsumaTextButton(text = stringResource(R.string.catalog_text_button), onClick = { })
            ConsumaPrimaryButton(text = stringResource(R.string.catalog_loading_button), onClick = { }, loading = true)

            ConsumaDivider()

            ConsumaSectionHeader(title = stringResource(R.string.catalog_inputs_title))
            ConsumaTextField(value = "", onValueChange = {}, label = stringResource(R.string.catalog_text_field))
            ConsumaSearchField(query = "", onQueryChange = {}, modifier = Modifier.testTag("search_field"))

            ConsumaDivider()

            ConsumaSectionHeader(title = stringResource(R.string.catalog_cards_title))
            ConsumaMerchantCardPlaceholder(title = stringResource(R.string.catalog_merchant), subtitle = stringResource(R.string.catalog_location), actionText = stringResource(R.string.catalog_view), onActionClick = {})

            ConsumaDivider()

            ConsumaSectionHeader(title = stringResource(R.string.catalog_chips_title))
            ConsumaFilterChip(selected = true, onClick = {}, label = stringResource(R.string.catalog_filter_nearby))
            ConsumaStatusChip(text = stringResource(R.string.catalog_success), semantic = ConsumaStatusSemantic.SUCCESS)
            ConsumaStatusChip(text = stringResource(R.string.catalog_warning), semantic = ConsumaStatusSemantic.WARNING)

            ConsumaDivider()

            ConsumaSectionHeader(title = stringResource(R.string.catalog_avatars_title))
            ConsumaAvatar(initials = "CA")

            ConsumaDivider()

            ConsumaSectionHeader(title = stringResource(R.string.catalog_prices_title))
            ConsumaPriceText(price = stringResource(R.string.catalog_current_price), oldPrice = stringResource(R.string.catalog_old_price))

            ConsumaDivider()

            ConsumaSectionHeader(title = stringResource(R.string.catalog_messages_title))
            ConsumaInlineMessage(message = stringResource(R.string.catalog_inline_message), semantic = ConsumaStatusSemantic.INFO)

            ConsumaDivider()

            ConsumaSectionHeader(title = stringResource(R.string.catalog_screen_states_title))
            ConsumaLoadingState(modifier = Modifier.fillMaxWidth())
            ConsumaEmptyState(title = stringResource(R.string.empty_state_preview_title), description = stringResource(R.string.empty_state_preview_description))
            ConsumaErrorState(message = stringResource(R.string.error_state_preview_message), onRetry = {})
        }
    }
}
