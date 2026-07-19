package ao.consuma.aqui.feature.discovery.presentation.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.*
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.discovery.presentation.mapper.*

@Composable
fun MerchantOverviewScreen(
    uiState: MerchantOverviewUiState,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCatalog: (String) -> Unit,
    modifier: Modifier = Modifier,
    cartAction: @Composable RowScope.() -> Unit = {}
) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.MERCHANT_OVERVIEW),
        topBar = {
            ConsumaTopAppBar(
                stringResource(R.string.merchant_overview_title),
                onBackClick = onNavigateBack,
                actions = cartAction
            )
        }
    ) { padding ->
        when (uiState) {
            MerchantOverviewUiState.Loading -> ConsumaLoadingState(Modifier.padding(padding).testTag(NavigationTestTags.MERCHANT_LOADING))
            is MerchantOverviewUiState.Content -> MerchantContent(uiState.merchant, false, onNavigateToCatalog, Modifier.padding(padding))
            is MerchantOverviewUiState.OfflineContent -> MerchantContent(uiState.merchant, true, onNavigateToCatalog, Modifier.padding(padding))
            is MerchantOverviewUiState.Error -> ConsumaErrorState(
                uiState.message.resolve(),
                if (uiState.canRetry) onRetry else null,
                Modifier.padding(padding).testTag(NavigationTestTags.MERCHANT_ERROR)
            )
            MerchantOverviewUiState.NotFound -> ConsumaEmptyState(
                stringResource(R.string.merchant_not_found),
                modifier = Modifier.padding(padding).testTag(NavigationTestTags.MERCHANT_NOT_FOUND),
                actionText = stringResource(R.string.back),
                onActionClick = onNavigateBack
            )
        }
    }
}

@Composable
private fun MerchantContent(
    merchant: MerchantOverviewUiModel,
    offline: Boolean,
    onNavigateToCatalog: (String) -> Unit,
    modifier: Modifier
) {
    val fulfillmentText = merchant.fulfillmentText?.resolve()
    val catalogDescription = stringResource(R.string.merchant_view_catalog_accessibility, merchant.name)
    val logoDescription = stringResource(R.string.merchant_logo_description, merchant.name)
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag(NavigationTestTags.MERCHANT_LIST),
        contentPadding = PaddingValues(bottom = ConsumaSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        if (offline) item("offline") {
            Column(Modifier.padding(horizontal = ConsumaSpacing.lg)) {
                ConsumaOfflineBanner(Modifier.testTag(NavigationTestTags.MERCHANT_OFFLINE))
                Text(stringResource(R.string.search_offline_stale), style = MaterialTheme.typography.bodySmall)
            }
        }
        item("banner") {
            ConsumaImagePlaceholder(
                modifier = Modifier.fillMaxWidth().height(ConsumaSize.merchantOverviewBannerHeight),
                contentDescription = stringResource(R.string.merchant_banner_description, merchant.name)
            )
        }
        item("identity") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ConsumaSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConsumaAvatar(
                    modifier = Modifier.semantics { contentDescription = logoDescription },
                    initials = merchant.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                    size = ConsumaSize.avatarLarge
                )
                Column(Modifier.weight(1f)) {
                    Text(merchant.name, style = MaterialTheme.typography.headlineSmall)
                    Text(merchant.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ConsumaStatusChip(merchant.availabilityLabel.resolve(), merchant.availabilitySemantic)
            }
        }
        merchant.ratingText?.let { item("rating") { OverviewLine(stringResource(R.string.merchant_rating), it.resolve()) } }
        merchant.fullDescription?.let { item("description") { Text(it, Modifier.padding(horizontal = ConsumaSpacing.lg), style = MaterialTheme.typography.bodyLarge) } }
            ?: merchant.shortDescription?.let { item("description-short") { Text(it, Modifier.padding(horizontal = ConsumaSpacing.lg), style = MaterialTheme.typography.bodyLarge) } }
        fulfillmentText?.let { item("fulfillment") { OverviewLine(stringResource(R.string.merchant_fulfillment), it) } }
        merchant.addressText?.let { item("address") { OverviewLine(stringResource(R.string.merchant_location), it) } }
        merchant.openingHoursText?.let { item("hours") { OverviewLine(stringResource(R.string.merchant_hours), it.resolve()) } }
        merchant.contactText?.let { item("contact") { OverviewLine(stringResource(R.string.merchant_contact), it) } }
        merchant.promotionText?.let { item("promotion") { OverviewLine(stringResource(R.string.merchant_promotion), it) } }
        item("catalog") {
            if (merchant.catalogAvailable) {
                ConsumaPrimaryButton(
                    stringResource(R.string.merchant_view_catalog),
                    { onNavigateToCatalog(merchant.id) },
                    Modifier.padding(horizontal = ConsumaSpacing.lg).testTag(NavigationTestTags.MERCHANT_VIEW_CATALOG)
                        .semantics { contentDescription = catalogDescription },
                    fullWidth = true
                )
            } else {
                ConsumaSecondaryButton(
                    stringResource(R.string.merchant_catalog_unavailable), {},
                    Modifier.padding(horizontal = ConsumaSpacing.lg).testTag(NavigationTestTags.MERCHANT_CATALOG_UNAVAILABLE),
                    enabled = false, fullWidth = true
                )
            }
        }
    }
}

@Composable
private fun OverviewLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = ConsumaSpacing.lg)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val fullPreview = MerchantOverviewUiModel(
    "sabor-maianga", "Sabor da Maianga", "Sabores angolanos", "Sabores angolanos preparados no dia. Informação demonstrativa.",
    "Restaurantes", false, false, UiText.Resource(R.string.home_availability_open), ConsumaStatusSemantic.SUCCESS,
    UiText.Joined(listOf(UiText.Resource(R.string.home_fulfillment_delivery), UiText.Resource(R.string.home_fulfillment_pickup))),
    "+244 900 000 000 · contacto@example.invalid", UiText.Resource(R.string.merchant_schedule_monday_saturday, listOf("08:00", "20:00")),
    "Rua demonstrativa — Maianga, Luanda", UiText.Dynamic("4,7 (125)"), "MENU DO DIA", true
)

@Preview(showBackground = true) @Composable private fun LoadingPreview() { ConsumaAquiTheme { MerchantOverviewScreen(MerchantOverviewUiState.Loading, {}, {}, {}) } }
@Preview(showBackground = true) @Composable private fun ContentPreview() { ConsumaAquiTheme { MerchantOverviewScreen(MerchantOverviewUiState.Content(fullPreview), {}, {}, {}) } }
@Preview(showBackground = true) @Composable private fun PartialPreview() { ConsumaAquiTheme { MerchantOverviewScreen(MerchantOverviewUiState.Content(fullPreview.copy(fullDescription = null, ratingText = null, contactText = null, openingHoursText = null, promotionText = null, catalogAvailable = false)), {}, {}, {}) } }
@Preview(showBackground = true) @Composable private fun OfflinePreview() { ConsumaAquiTheme { MerchantOverviewScreen(MerchantOverviewUiState.OfflineContent(fullPreview), {}, {}, {}) } }
@Preview(showBackground = true) @Composable private fun ErrorPreview() { ConsumaAquiTheme { MerchantOverviewScreen(MerchantOverviewUiState.Error(UiText.Resource(R.string.merchant_error_generic), true), {}, {}, {}) } }
@Preview(showBackground = true) @Composable private fun NotFoundPreview() { ConsumaAquiTheme { MerchantOverviewScreen(MerchantOverviewUiState.NotFound, {}, {}, {}) } }
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES) @Composable private fun DarkPreview() { ConsumaAquiTheme { MerchantOverviewScreen(MerchantOverviewUiState.Content(fullPreview), {}, {}, {}) } }
