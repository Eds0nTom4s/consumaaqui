package ao.consuma.aqui.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
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
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.discovery.presentation.mapper.MerchantCompactUiModel
import ao.consuma.aqui.feature.discovery.presentation.mapper.resolve
import ao.consuma.aqui.feature.discovery.presentation.merchant.MerchantCompactCard

@Composable
fun HomeScreen(uiState: HomeUiState, onEvent: (HomeUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.HOME),
        topBar = {
            ConsumaTopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = { onEvent(HomeUiEvent.Refresh) }, modifier = Modifier.testTag(NavigationTestTags.HOME_REFRESH)) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.home_refresh))
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            HomeUiState.Loading -> ConsumaLoadingState(Modifier.padding(padding).testTag(NavigationTestTags.HOME_LOADING))
            is HomeUiState.Error -> ConsumaErrorState(
                uiState.message.resolve(),
                if (uiState.canRetry) {{ onEvent(HomeUiEvent.Retry) }} else null,
                Modifier.padding(padding).testTag(NavigationTestTags.HOME_ERROR)
            )
            is HomeUiState.Empty -> HomeDiscoveryContent(
                uiState.location, uiState.categories, uiState.selectedCategoryId, uiState.query,
                emptyList(), emptyList(), emptyList(), false, false, true, onEvent, Modifier.padding(padding)
            )
            is HomeUiState.Content -> HomeDiscoveryContent(
                uiState.location, uiState.categories, uiState.selectedCategoryId, uiState.query,
                uiState.featuredMerchants, uiState.nearbyMerchants, uiState.recommendedMerchants,
                uiState.isOffline, uiState.isRefreshing, false, onEvent, Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun HomeDiscoveryContent(
    location: LocationUiModel?,
    categories: List<CategoryUiModel>,
    selectedCategoryId: String?,
    query: String,
    featured: List<MerchantCompactUiModel>,
    nearby: List<MerchantCompactUiModel>,
    recommended: List<MerchantCompactUiModel>,
    isOffline: Boolean,
    isRefreshing: Boolean,
    empty: Boolean,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier
) {
    val scrollState = rememberLazyListState()
    LazyColumn(
        state = scrollState,
        modifier = modifier.fillMaxSize().testTag(NavigationTestTags.HOME_LIST),
        contentPadding = PaddingValues(ConsumaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        item("identity") { Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.headlineSmall) }
        if (isOffline) item("offline") { ConsumaOfflineBanner(Modifier.testTag(NavigationTestTags.HOME_OFFLINE)) }
        item("location") { HomeLocation(location) { onEvent(HomeUiEvent.LocationSelected) } }
        item("search") {
            ConsumaSearchField(
                query = query,
                onQueryChange = { onEvent(HomeUiEvent.SearchChanged(it)) },
                label = stringResource(R.string.search_field_label),
                placeholder = stringResource(R.string.home_search_placeholder),
                modifier = Modifier.testTag(NavigationTestTags.HOME_SEARCH)
            )
        }
        item("categories") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
                items(categories, key = { it.id ?: "all" }) { category ->
                    val categoryLabel = category.label.resolve()
                    ConsumaFilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onEvent(HomeUiEvent.CategorySelected(category.id)) },
                        label = categoryLabel,
                        modifier = Modifier.testTag("${NavigationTestTags.HOME_CATEGORY}_${category.id ?: "all"}")
                            .semantics { contentDescription = categoryLabel }
                    )
                }
            }
        }
        if (isRefreshing) item("refreshing") { Text(stringResource(R.string.home_refreshing), Modifier.testTag(NavigationTestTags.HOME_REFRESHING)) }
        if (empty) {
            item("empty") {
                Box(Modifier.fillParentMaxHeight(0.4f).testTag(NavigationTestTags.HOME_EMPTY)) {
                    ConsumaEmptyState(
                        stringResource(R.string.home_empty_title),
                        description = stringResource(R.string.home_empty_description),
                        actionText = stringResource(R.string.home_clear_filters),
                        onActionClick = {
                            onEvent(HomeUiEvent.SearchChanged(""))
                            onEvent(HomeUiEvent.CategorySelected(null))
                        }
                    )
                }
            }
        } else {
            if (featured.isNotEmpty()) {
                item("featured-title") {
                    ConsumaSectionHeader(stringResource(R.string.home_featured_title), actionText = stringResource(R.string.home_view_all), onActionClick = { onEvent(HomeUiEvent.ViewAll) })
                }
                item("featured") {
                    MerchantCompactRow(featured, onEvent, NavigationTestTags.HOME_FEATURED_LIST)
                }
            }
            val discovery = if (location == null) recommended else nearby
            if (discovery.isNotEmpty()) {
                item("discovery-title") {
                    ConsumaSectionHeader(
                        stringResource(if (location == null) R.string.home_recommended_title else R.string.home_nearby_title),
                        actionText = stringResource(R.string.home_view_all),
                        onActionClick = { onEvent(HomeUiEvent.ViewAll) }
                    )
                }
                item("discovery") {
                    MerchantCompactRow(discovery, onEvent, NavigationTestTags.HOME_DISCOVERY_LIST)
                }
            }
        }
    }
}

@Composable
private fun MerchantCompactRow(
    items: List<MerchantCompactUiModel>,
    onEvent: (HomeUiEvent) -> Unit,
    testTag: String
) {
    LazyRow(
        modifier = Modifier.testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        items(items, key = { it.id }) { merchant ->
            MerchantCompactCard(
                merchant,
                { onEvent(HomeUiEvent.MerchantSelected(merchant.id)) },
                "${NavigationTestTags.HOME_MERCHANT}_${merchant.id}",
                Modifier.width(ConsumaSize.merchantCompactCardWidth)
            )
        }
    }
}

@Composable
private fun HomeLocation(location: LocationUiModel?, onClick: () -> Unit) {
    ConsumaClickableCard(onClick, Modifier.fillMaxWidth().testTag(NavigationTestTags.HOME_LOCATION_CARD)) {
        Row(
            Modifier.fillMaxWidth().padding(ConsumaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)
        ) {
            Icon(Icons.Default.LocationOn, null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (location == null) R.string.home_no_location_title else R.string.home_current_location), style = MaterialTheme.typography.labelMedium)
                Text(location?.displayName ?: stringResource(R.string.home_no_location_discovery), style = MaterialTheme.typography.bodySmall)
            }
            ConsumaTextButton(
                stringResource(if (location == null) R.string.home_choose_location else R.string.home_change_location),
                onClick,
                Modifier.testTag(if (location == null) NavigationTestTags.HOME_CHOOSE_LOCATION else NavigationTestTags.HOME_CHANGE_LOCATION)
            )
        }
    }
}

@Composable private fun UiText.resolve(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
}

private val previewMerchant = MerchantCompactUiModel(
    "preview", "Sabor da Maianga", "Restaurantes", false,
    ao.consuma.aqui.feature.discovery.presentation.mapper.UiText.Resource(R.string.home_availability_open),
    ConsumaStatusSemantic.SUCCESS,
    ao.consuma.aqui.feature.discovery.presentation.mapper.UiText.Resource(R.string.home_fulfillment_delivery),
    ao.consuma.aqui.feature.discovery.presentation.mapper.UiText.Dynamic("450 m · 25 min"), "DESTAQUE",
    ao.consuma.aqui.feature.discovery.presentation.mapper.UiText.Resource(R.string.merchant_card_accessibility, listOf("Sabor da Maianga", "Restaurantes"))
)

@Preview(showBackground = true) @Composable private fun HomeContentPreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Content(LocationUiModel("Luanda — Maianga"), listOf(CategoryUiModel(null, ao.consuma.aqui.feature.discovery.presentation.mapper.UiText.Resource(R.string.search_filter_all))), null, listOf(previewMerchant), emptyList(), listOf(previewMerchant), "", false, false), {}) } }
@Preview(showBackground = true) @Composable private fun HomeLoadingPreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Loading, {}) } }
@Preview(showBackground = true) @Composable private fun HomeEmptyPreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Empty(null, listOf(CategoryUiModel(null, ao.consuma.aqui.feature.discovery.presentation.mapper.UiText.Resource(R.string.search_filter_all))), "", null), {}) } }
@Preview(showBackground = true) @Composable private fun HomeErrorPreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Error(UiText.Resource(R.string.home_error_generic), true), {}) } }
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES) @Composable private fun HomeDarkPreview() { ConsumaAquiTheme { MerchantCompactCard(previewMerchant, {}, "preview", Modifier.width(ConsumaSize.merchantCompactCardWidth)) } }
