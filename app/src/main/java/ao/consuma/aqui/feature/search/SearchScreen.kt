package ao.consuma.aqui.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.*
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy
import ao.consuma.aqui.feature.discovery.domain.model.FulfillmentOption
import ao.consuma.aqui.feature.discovery.presentation.mapper.*
import ao.consuma.aqui.feature.discovery.presentation.merchant.MerchantListCard

@Composable
fun SearchScreen(uiState: SearchUiState, onEvent: (SearchUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.SEARCH),
        topBar = { ConsumaTopAppBar(title = stringResource(R.string.search_title)) }
    ) { padding ->
        when (uiState) {
            SearchUiState.Loading -> ConsumaLoadingState(Modifier.padding(padding).testTag(NavigationTestTags.SEARCH_LOADING))
            is SearchUiState.Content -> SearchBody(uiState.data, null, false, onEvent, Modifier.padding(padding))
            is SearchUiState.OfflineContent -> SearchBody(uiState.data, null, true, onEvent, Modifier.padding(padding))
            is SearchUiState.Empty -> SearchBody(
                SearchResultsUiModel(uiState.criteria, emptyList(), 0, UiText.Plural(R.plurals.search_results_count, 0), false, uiState.criteria.query.trim().isEmpty()),
                uiState.message, false, onEvent, Modifier.padding(padding)
            )
            is SearchUiState.Error -> ConsumaErrorState(
                message = uiState.message.resolve(),
                onRetry = if (uiState.canRetry) {{ onEvent(SearchUiEvent.Retry) }} else null,
                modifier = Modifier.padding(padding).testTag(NavigationTestTags.SEARCH_ERROR)
            )
        }
    }
}

@Composable
private fun SearchBody(
    data: SearchResultsUiModel,
    emptyMessage: UiText?,
    offline: Boolean,
    onEvent: (SearchUiEvent) -> Unit,
    modifier: Modifier
) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var sortExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val criteria = data.criteria
    val currentSort = criteria.sortOptions.firstOrNull { it.value == criteria.orderBy }?.label?.resolve()
        ?: stringResource(R.string.search_order_featured)
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag(NavigationTestTags.SEARCH_LIST),
        contentPadding = PaddingValues(ConsumaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        if (offline) item("offline") {
            Column(verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)) {
                ConsumaOfflineBanner(Modifier.testTag(NavigationTestTags.SEARCH_OFFLINE))
                Text(stringResource(R.string.search_offline_stale), style = MaterialTheme.typography.bodySmall)
                ConsumaTextButton(stringResource(R.string.home_refresh), { onEvent(SearchUiEvent.Refresh) })
            }
        }
        item("query") {
            ConsumaSearchField(
                query = criteria.query,
                onQueryChange = { onEvent(SearchUiEvent.QueryChanged(it)) },
                label = stringResource(R.string.search_field_label),
                placeholder = stringResource(R.string.home_search_placeholder),
                modifier = Modifier.testTag(NavigationTestTags.SEARCH_FIELD)
            )
        }
        item("quick-filters") { QuickFilters(criteria, onEvent) }
        item("filter-actions") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ConsumaTextButton(
                    stringResource(R.string.search_filter_action, criteria.filters.activeCount),
                    { filtersExpanded = !filtersExpanded },
                    Modifier.testTag(NavigationTestTags.SEARCH_FILTER_ACTION)
                )
                ConsumaTextButton(
                    stringResource(R.string.search_sort_action_current, currentSort),
                    { sortExpanded = !sortExpanded },
                    Modifier.testTag(NavigationTestTags.SEARCH_SORT_ACTION)
                )
            }
        }
        if (criteria.filters.activeCount > 0) item("active-filters") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    pluralStringResource(R.plurals.search_active_filters, criteria.filters.activeCount, criteria.filters.activeCount),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag(NavigationTestTags.SEARCH_ACTIVE_FILTERS)
                )
                ConsumaTextButton(stringResource(R.string.home_clear_filters), { onEvent(SearchUiEvent.ClearFilters) })
            }
        }
        if (filtersExpanded) item("categories") {
            Column(verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
                Text(stringResource(R.string.search_category_title), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
                    items(criteria.categories, key = { it.id ?: "all" }) { category ->
                        val selected = criteria.filters.categoryId == category.id
                        ConsumaFilterChip(
                            selected, { onEvent(SearchUiEvent.CategorySelected(category.id)) }, category.label.resolve(),
                            Modifier.testTag("${NavigationTestTags.SEARCH_CATEGORY}_${category.id ?: "all"}")
                                .announcedSelection(selected)
                        )
                    }
                }
            }
        }
        if (sortExpanded) item("sort") {
            Column(verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
                Text(stringResource(R.string.search_order_title), style = MaterialTheme.typography.titleSmall)
                LazyRow(
                    modifier = Modifier.testTag(NavigationTestTags.SEARCH_SORT_LIST),
                    horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)
                ) {
                    items(criteria.sortOptions, key = { it.value.name }) { option ->
                        val selected = criteria.orderBy == option.value
                        ConsumaFilterChip(
                            selected = selected,
                            onClick = { onEvent(SearchUiEvent.SortSelected(option.value)) },
                            label = option.label.resolve(),
                            enabled = option.enabled,
                            modifier = Modifier.testTag("${NavigationTestTags.SEARCH_SORT_OPTION}_${option.value.name}")
                                .announcedSelection(selected)
                        )
                    }
                }
                if (!criteria.hasLocation) Text(stringResource(R.string.search_nearest_requires_location), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (data.isRefreshing) item("refreshing") { Text(stringResource(R.string.search_refreshing), Modifier.testTag(NavigationTestTags.SEARCH_REFRESHING)) }
        if (emptyMessage != null) item("empty") { SearchEmpty(criteria, emptyMessage, onEvent) }
        else {
            item("context") {
                Column {
                    Text(if (data.isExplorationMode) stringResource(R.string.search_explore_title) else stringResource(R.string.search_results_title), style = MaterialTheme.typography.titleMedium)
                    Text(data.resultContext.resolve(), style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(data.merchants, key = { it.id }) { merchant ->
                MerchantListCard(
                    merchant,
                    { onEvent(SearchUiEvent.MerchantSelected(merchant.id)) },
                    "${NavigationTestTags.SEARCH_MERCHANT}_${merchant.id}"
                )
            }
        }
    }
}

@Composable
private fun QuickFilters(criteria: SearchCriteriaUiState, onEvent: (SearchUiEvent) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
        item {
            val selected = criteria.filters.activeCount == 0
            ConsumaFilterChip(selected, { onEvent(SearchUiEvent.ClearFilters) }, stringResource(R.string.search_filter_all), Modifier.announcedSelection(selected))
        }
        item {
            val selected = criteria.filters.onlyOpen
            ConsumaFilterChip(
                selected,
                { onEvent(SearchUiEvent.OpenNowChanged(!selected)) },
                stringResource(R.string.search_filter_open),
                Modifier.testTag(NavigationTestTags.SEARCH_OPEN_NOW).announcedSelection(selected)
            )
        }
        item { FulfillmentChip(FulfillmentOption.DELIVERY, criteria, onEvent, R.string.search_filter_delivery) }
        item { FulfillmentChip(FulfillmentOption.PICKUP, criteria, onEvent, R.string.search_filter_pickup) }
        item { FulfillmentChip(FulfillmentOption.SERVICE, criteria, onEvent, R.string.search_filter_service) }
    }
}

@Composable
private fun FulfillmentChip(value: FulfillmentOption, criteria: SearchCriteriaUiState, onEvent: (SearchUiEvent) -> Unit, label: Int) {
    val selected = value in criteria.filters.fulfillmentOptions
    ConsumaFilterChip(
        selected, { onEvent(SearchUiEvent.FulfillmentToggled(value)) }, stringResource(label),
        Modifier.testTag("${NavigationTestTags.SEARCH_FULFILLMENT}_${value.name}")
            .announcedSelection(selected)
    )
}

@Composable
private fun SearchEmpty(criteria: SearchCriteriaUiState, message: UiText, onEvent: (SearchUiEvent) -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = ConsumaSize.feedbackMinHeight).testTag(NavigationTestTags.SEARCH_EMPTY),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.search_no_results_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(ConsumaSpacing.sm))
        Text(message.resolve(), style = MaterialTheme.typography.bodyMedium)
        if (criteria.filters.activeCount > 0) ConsumaTextButton(stringResource(R.string.home_clear_filters), { onEvent(SearchUiEvent.ClearFilters) })
        if (criteria.query.isNotBlank()) ConsumaTextButton(stringResource(R.string.search_clear_query), { onEvent(SearchUiEvent.ClearQuery) })
    }
}

@Composable
private fun Modifier.announcedSelection(selected: Boolean): Modifier {
    val description = stringResource(if (selected) R.string.filter_selected else R.string.filter_not_selected)
    return semantics { stateDescription = description }
}

private val previewMerchant = MerchantListItemUiModel(
    "sabor-maianga", "Sabor da Maianga", "Sabores angolanos preparados no dia", "Restaurantes", false,
    UiText.Resource(R.string.home_availability_open), ConsumaStatusSemantic.SUCCESS,
    UiText.Joined(listOf(UiText.Resource(R.string.home_fulfillment_delivery), UiText.Resource(R.string.home_fulfillment_pickup))),
    UiText.Dynamic("450 m · 25 min · 4,7 (125)"), "DESTAQUE",
    UiText.Resource(R.string.merchant_card_accessibility, listOf("Sabor da Maianga", "Restaurantes"))
)
private val previewCriteria = SearchCriteriaUiState(
    categories = listOf(
        CategoryUiModel(null, UiText.Resource(R.string.search_filter_all)),
        CategoryUiModel("restaurant", UiText.Dynamic("Restaurantes"))
    ),
    sortOptions = DiscoveryOrderBy.entries.map { SearchSortOptionUiModel(it, UiText.Dynamic(it.name), true) },
    hasLocation = true
)
private fun previewData(criteria: SearchCriteriaUiState = previewCriteria) = SearchResultsUiModel(criteria, listOf(previewMerchant), 1, UiText.Plural(R.plurals.search_exploration_count, 1), false, criteria.query.isBlank())

@Preview(showBackground = true) @Composable private fun ExplorationPreview() { ConsumaAquiTheme { SearchScreen(SearchUiState.Content(previewData()), {}) } }
@Preview(showBackground = true) @Composable private fun ResultsPreview() { ConsumaAquiTheme { SearchScreen(SearchUiState.Content(previewData(previewCriteria.copy(query = "sabor"))), {}) } }
@Preview(showBackground = true) @Composable private fun ActiveFiltersPreview() { ConsumaAquiTheme { SearchScreen(SearchUiState.Content(previewData(previewCriteria.copy(filters = SearchFiltersUiModel(onlyOpen = true, fulfillmentOptions = setOf(FulfillmentOption.DELIVERY))))), {}) } }
@Preview(showBackground = true) @Composable private fun EmptyPreview() { ConsumaAquiTheme { SearchScreen(SearchUiState.Empty(previewCriteria.copy(query = "inexistente"), UiText.Resource(R.string.search_empty_query_description)), {}) } }
@Preview(showBackground = true) @Composable private fun ErrorPreview() { ConsumaAquiTheme { SearchScreen(SearchUiState.Error(UiText.Resource(R.string.search_error_generic), true), {}) } }
@Preview(showBackground = true) @Composable private fun OfflinePreview() { ConsumaAquiTheme { SearchScreen(SearchUiState.OfflineContent(previewData()), {}) } }
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES) @Composable private fun SearchDarkPreview() { ConsumaAquiTheme { SearchScreen(SearchUiState.Content(previewData()), {}) } }
