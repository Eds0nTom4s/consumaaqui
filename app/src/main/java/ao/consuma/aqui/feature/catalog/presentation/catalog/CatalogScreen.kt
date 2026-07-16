package ao.consuma.aqui.feature.catalog.presentation.catalog

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaErrorState
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingState
import ao.consuma.aqui.core.designsystem.components.ConsumaOfflineBanner
import ao.consuma.aqui.core.designsystem.components.ConsumaSearchField
import ao.consuma.aqui.core.designsystem.components.ConsumaTextButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.catalog.presentation.components.CatalogCategorySelector
import ao.consuma.aqui.feature.catalog.presentation.components.CatalogProductCard
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogCategoryUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogContentUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogProductCardUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiText
import ao.consuma.aqui.feature.catalog.presentation.mapper.resolve

@Composable
fun CatalogScreen(
    uiState: CatalogUiState,
    onNavigateBack: () -> Unit,
    onEvent: (CatalogUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.CATALOG),
        topBar = {
            ConsumaTopAppBar(
                title = stringResource(R.string.catalog_screen_title),
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { onEvent(CatalogUiEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.catalog_refresh))
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            CatalogUiState.Loading -> ConsumaLoadingState(Modifier.padding(padding).testTag(NavigationTestTags.CATALOG_LOADING))
            CatalogUiState.CatalogNotFound -> CatalogStandaloneEmpty(
                R.string.catalog_not_found,
                R.string.catalog_not_found_description,
                onNavigateBack,
                Modifier.padding(padding)
            )
            CatalogUiState.InvalidMerchant -> CatalogStandaloneEmpty(
                R.string.catalog_invalid_merchant,
                R.string.catalog_invalid_merchant_description,
                onNavigateBack,
                Modifier.padding(padding)
            )
            is CatalogUiState.Error -> ConsumaErrorState(
                uiState.message.resolve(),
                if (uiState.canRetry) {{ onEvent(CatalogUiEvent.Retry) }} else null,
                Modifier.padding(padding).testTag(NavigationTestTags.CATALOG_ERROR)
            )
            is CatalogUiState.Content -> CatalogBody(uiState.data, null, onEvent, Modifier.padding(padding))
            is CatalogUiState.Empty -> if (uiState.data == null) {
                CatalogStandaloneEmpty(
                    R.string.catalog_unavailable,
                    R.string.catalog_unavailable_description,
                    onNavigateBack,
                    Modifier.padding(padding)
                )
            } else {
                CatalogBody(uiState.data, uiState.reason, onEvent, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun CatalogBody(
    data: CatalogContentUiModel,
    emptyReason: CatalogEmptyReason?,
    onEvent: (CatalogUiEvent) -> Unit,
    modifier: Modifier
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag(NavigationTestTags.CATALOG_LIST),
        contentPadding = PaddingValues(vertical = ConsumaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        item("header") {
            Column(Modifier.padding(horizontal = ConsumaSpacing.lg)) {
                Text(data.title, style = MaterialTheme.typography.headlineSmall)
                data.description?.let {
                    Spacer(Modifier.height(ConsumaSpacing.xs))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (data.isOffline) item("offline") {
            Column(Modifier.padding(horizontal = ConsumaSpacing.lg)) {
                ConsumaOfflineBanner(Modifier.testTag(NavigationTestTags.CATALOG_OFFLINE))
                Text(stringResource(R.string.catalog_offline_notice), style = MaterialTheme.typography.bodySmall)
            }
        }
        item("search") {
            Column(Modifier.padding(horizontal = ConsumaSpacing.lg)) {
                ConsumaSearchField(
                    query = data.query,
                    onQueryChange = { onEvent(CatalogUiEvent.QueryChanged(it)) },
                    label = stringResource(R.string.catalog_search_label),
                    placeholder = stringResource(R.string.catalog_search_placeholder),
                    modifier = Modifier.testTag(NavigationTestTags.CATALOG_SEARCH)
                )
                if (data.query.isNotBlank()) {
                    ConsumaTextButton(stringResource(R.string.catalog_clear_search), { onEvent(CatalogUiEvent.ClearQuery) })
                }
            }
        }
        item("categories") {
            CatalogCategorySelector(data.categories, data.selectedCategoryId, {
                onEvent(CatalogUiEvent.CategorySelected(it))
            })
        }
        if (data.isRefreshing) item("refreshing") {
            Text(stringResource(R.string.catalog_refreshing), Modifier.padding(horizontal = ConsumaSpacing.lg))
        }
        if (emptyReason != null) item("empty") {
            val (title, description) = emptyText(emptyReason)
            ConsumaEmptyState(
                title = stringResource(title),
                description = stringResource(description),
                actionText = if (data.query.isNotBlank()) stringResource(R.string.catalog_clear_search) else null,
                onActionClick = if (data.query.isNotBlank()) {{ onEvent(CatalogUiEvent.ClearQuery) }} else null,
                modifier = Modifier.fillMaxWidth().height(280.dp).testTag(NavigationTestTags.CATALOG_EMPTY)
            )
        } else {
            data.resultContext?.let { context -> item("context") {
                Text(context.resolve(), Modifier.padding(horizontal = ConsumaSpacing.lg), style = MaterialTheme.typography.labelLarge)
            } }
            items(data.products, key = { it.id }) { product ->
                CatalogProductCard(
                    product,
                    { onEvent(CatalogUiEvent.ProductSelected(product.id)) },
                    Modifier.padding(horizontal = ConsumaSpacing.lg)
                )
            }
        }
    }
}

@Composable
private fun CatalogStandaloneEmpty(title: Int, description: Int, onBack: () -> Unit, modifier: Modifier) {
    ConsumaEmptyState(
        stringResource(title),
        modifier.testTag(NavigationTestTags.CATALOG_EMPTY),
        stringResource(description),
        stringResource(R.string.back),
        onBack
    )
}

private fun emptyText(reason: CatalogEmptyReason): Pair<Int, Int> = when (reason) {
    CatalogEmptyReason.CATALOG_EMPTY -> R.string.catalog_empty to R.string.catalog_empty_description
    CatalogEmptyReason.CATEGORY_EMPTY -> R.string.catalog_category_empty to R.string.catalog_category_empty_description
    CatalogEmptyReason.SEARCH_EMPTY -> R.string.catalog_search_empty to R.string.catalog_search_empty_description
    CatalogEmptyReason.CATALOG_UNAVAILABLE -> R.string.catalog_unavailable to R.string.catalog_unavailable_description
}

private val previewCategory = CatalogCategoryUiModel(null, CatalogUiText.Resource(R.string.catalog_category_all), true)
private val previewProduct = CatalogProductCardUiModel(
    "product", "merchant", "Muamba da Casa", "Produto demonstrativo", false, "4.500 Kz", "5.000 Kz",
    CatalogUiText.Resource(R.string.product_available), ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic.SUCCESS,
    CatalogUiText.Resource(R.string.catalog_preparation_minutes, listOf(20)), true, true, "Pratos",
    CatalogUiText.Dynamic("Muamba da Casa, 4.500 Kz")
)
private fun previewCatalog(query: String = "", offline: Boolean = false) = CatalogContentUiModel(
    "merchant", "Sabor da Maianga", "Refeições preparadas no dia", query,
    listOf(previewCategory, CatalogCategoryUiModel("pratos", CatalogUiText.Dynamic("Pratos"), true)),
    null, listOf(previewProduct), query.isNotBlank(), false, offline,
    CatalogUiText.Plural(R.plurals.catalog_results_count, 1)
)

@Preview(showBackground = true) @Composable private fun CatalogLoadingPreview() { ConsumaAquiTheme { CatalogScreen(CatalogUiState.Loading, {}, {}) } }
@Preview(showBackground = true) @Composable private fun CatalogContentPreview() { ConsumaAquiTheme { CatalogScreen(CatalogUiState.Content(previewCatalog()), {}, {}) } }
@Preview(showBackground = true) @Composable private fun CatalogSelectedCategoryPreview() { ConsumaAquiTheme { CatalogScreen(CatalogUiState.Content(previewCatalog().copy(selectedCategoryId = "pratos")), {}, {}) } }
@Preview(showBackground = true) @Composable private fun CatalogSearchPreview() { ConsumaAquiTheme { CatalogScreen(CatalogUiState.Content(previewCatalog("muamba")), {}, {}) } }
@Preview(showBackground = true) @Composable private fun CatalogEmptyPreview() { ConsumaAquiTheme { CatalogScreen(CatalogUiState.Empty(previewCatalog(), CatalogEmptyReason.CATEGORY_EMPTY), {}, {}) } }
@Preview(showBackground = true) @Composable private fun CatalogErrorPreview() { ConsumaAquiTheme { CatalogScreen(CatalogUiState.Error(CatalogUiText.Resource(R.string.catalog_error_generic), true), {}, {}) } }
@Preview(showBackground = true) @Composable private fun CatalogOfflinePreview() { ConsumaAquiTheme { CatalogScreen(CatalogUiState.Content(previewCatalog(offline = true)), {}, {}) } }
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES) @Composable private fun CatalogDarkPreview() { ConsumaAquiTheme { CatalogScreen(CatalogUiState.Content(previewCatalog()), {}, {}) } }
