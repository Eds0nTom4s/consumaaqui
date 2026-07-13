package ao.consuma.aqui.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import ao.consuma.aqui.core.designsystem.components.ConsumaClickableCard
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaErrorState
import ao.consuma.aqui.core.designsystem.components.ConsumaFilterChip
import ao.consuma.aqui.core.designsystem.components.ConsumaImagePlaceholder
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingState
import ao.consuma.aqui.core.designsystem.components.ConsumaOfflineBanner
import ao.consuma.aqui.core.designsystem.components.ConsumaSearchField
import ao.consuma.aqui.core.designsystem.components.ConsumaSectionHeader
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusChip
import ao.consuma.aqui.core.designsystem.components.ConsumaTextButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.HOME),
        topBar = {
            ConsumaTopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(
                        onClick = { onEvent(HomeUiEvent.Refresh) },
                        modifier = Modifier.testTag(NavigationTestTags.HOME_REFRESH)
                    ) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.home_refresh))
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            HomeUiState.Loading -> ConsumaLoadingState(
                Modifier.padding(paddingValues).testTag(NavigationTestTags.HOME_LOADING)
            )
            is HomeUiState.Error -> ConsumaErrorState(
                message = uiState.message.resolve(),
                onRetry = if (uiState.canRetry) {{ onEvent(HomeUiEvent.Retry) }} else null,
                modifier = Modifier.padding(paddingValues).testTag(NavigationTestTags.HOME_ERROR)
            )
            is HomeUiState.Empty -> HomeDiscoveryList(
                location = uiState.location,
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                query = uiState.query,
                nearby = emptyList(),
                featured = emptyList(),
                isOffline = false,
                isRefreshing = false,
                onEvent = onEvent,
                empty = true,
                modifier = Modifier.padding(paddingValues)
            )
            is HomeUiState.Content -> HomeDiscoveryList(
                location = uiState.location,
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                query = uiState.query,
                nearby = uiState.nearbyMerchants,
                featured = uiState.featuredMerchants,
                isOffline = uiState.isOffline,
                isRefreshing = uiState.isRefreshing,
                onEvent = onEvent,
                empty = false,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun HomeDiscoveryList(
    location: LocationUiModel?,
    categories: List<CategoryUiModel>,
    selectedCategoryId: String?,
    query: String,
    nearby: List<MerchantCardUiModel>,
    featured: List<MerchantCardUiModel>,
    isOffline: Boolean,
    isRefreshing: Boolean,
    onEvent: (HomeUiEvent) -> Unit,
    empty: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(ConsumaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        item("welcome") {
            Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.headlineSmall)
        }
        if (isOffline) item("offline") {
            ConsumaOfflineBanner(Modifier.testTag(NavigationTestTags.HOME_OFFLINE))
        }
        item("location") { HomeLocation(location) { onEvent(HomeUiEvent.LocationSelected) } }
        item("search") {
            ConsumaSearchField(
                query = query,
                onQueryChange = { onEvent(HomeUiEvent.SearchChanged(it)) },
                placeholder = stringResource(R.string.home_search_placeholder),
                modifier = Modifier.testTag(NavigationTestTags.HOME_SEARCH)
            )
        }
        item("categories") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
                items(categories, key = { it.id ?: "all" }) { category ->
                    ConsumaFilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onEvent(HomeUiEvent.CategorySelected(category.id)) },
                        label = category.label,
                        modifier = Modifier
                            .testTag("${NavigationTestTags.HOME_CATEGORY}_${category.id ?: "all"}")
                            .semantics { contentDescription = category.label }
                    )
                }
            }
        }
        if (isRefreshing) item("refreshing") {
            Text(stringResource(R.string.home_refreshing), modifier = Modifier.testTag(NavigationTestTags.HOME_REFRESHING))
        }
        if (empty) item("empty") {
            Box(Modifier.fillParentMaxHeight(0.45f).testTag(NavigationTestTags.HOME_EMPTY)) {
                ConsumaEmptyState(
                    title = stringResource(R.string.home_empty_title),
                    description = stringResource(R.string.home_empty_description),
                    actionText = stringResource(R.string.home_clear_filters),
                    onActionClick = {
                        onEvent(HomeUiEvent.SearchChanged(""))
                        onEvent(HomeUiEvent.CategorySelected(null))
                    }
                )
            }
        } else {
            item("nearby-title") {
                ConsumaSectionHeader(title = stringResource(if (location == null) R.string.home_discovery_title else R.string.home_nearby_title))
            }
            items(nearby, key = { "nearby-${it.id}" }) { merchant ->
                HomeMerchantCard(model = merchant, onClick = { onEvent(HomeUiEvent.MerchantSelected(merchant.id)) })
            }
            if (featured.isNotEmpty()) {
                item("featured-title") { ConsumaSectionHeader(stringResource(R.string.home_featured_title)) }
                items(featured, key = { "featured-${it.id}" }) { merchant ->
                    HomeMerchantCard(model = merchant, onClick = { onEvent(HomeUiEvent.MerchantSelected(merchant.id)) })
                }
            }
        }
    }
}

@Composable
private fun HomeLocation(location: LocationUiModel?, onClick: () -> Unit) {
    ConsumaClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(NavigationTestTags.HOME_LOCATION_CARD)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ConsumaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
        ) {
            Icon(Icons.Default.LocationOn, null)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (location == null) R.string.home_no_location_title else R.string.home_current_location),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(location?.displayName ?: stringResource(R.string.home_no_location_discovery))
            }
            ConsumaTextButton(
                text = stringResource(if (location == null) R.string.home_choose_location else R.string.home_change_location),
                onClick = onClick,
                modifier = Modifier.testTag(if (location == null) NavigationTestTags.HOME_CHOOSE_LOCATION else NavigationTestTags.HOME_CHANGE_LOCATION)
            )
        }
    }
}

@Composable
fun HomeMerchantCard(model: MerchantCardUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val fulfillmentLabels = mutableListOf<String>()
    for (label in model.fulfillmentLabels) fulfillmentLabels += label.resolve()
    ConsumaClickableCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag("${NavigationTestTags.HOME_MERCHANT}_${model.id}")
            .semantics { contentDescription = model.accessibilityDescription }
    ) {
        ConsumaImagePlaceholder(
            contentDescription = stringResource(R.string.home_merchant_image_placeholder, model.name),
            modifier = Modifier.fillMaxWidth().height(ConsumaSize.merchantImageHeight)
        )
        Column(Modifier.padding(ConsumaSpacing.md), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(model.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                ConsumaStatusChip(model.availabilityLabel.resolve(), model.availabilitySemantic)
            }
            Text(model.categoryLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (model.subtitle.isNotBlank()) Text(model.subtitle, style = MaterialTheme.typography.bodyMedium)
            Text(fulfillmentLabels.joinToString(" · "), style = MaterialTheme.typography.labelMedium)
            Text(listOfNotNull(model.distanceText, model.preparationTimeText, model.ratingText).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            model.promotionText?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun UiText.resolve(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
}

private val previewMerchant = MerchantCardUiModel("preview", "Sabor da Maianga", "Sabores angolanos", "Restaurantes", false, "450 m", "25 min", UiText.Resource(R.string.home_availability_open), ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic.SUCCESS, listOf(UiText.Resource(R.string.home_fulfillment_delivery)), "4,7 (125)", "5.500 Kz", "DESTAQUE", "Sabor da Maianga, aberto")

@Preview(showBackground = true) @Composable private fun HomeContentPreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Content(LocationUiModel("Luanda — Maianga"), listOf(CategoryUiModel(null, "Todos"), CategoryUiModel("restaurant", "Restaurantes")), null, listOf(previewMerchant), listOf(previewMerchant), "", false, false), {}) } }
@Preview(showBackground = true) @Composable private fun HomeLoadingPreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Loading, {}) } }
@Preview(showBackground = true) @Composable private fun HomeEmptyPreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Empty(null, listOf(CategoryUiModel(null, "Todos")), "", null), {}) } }
@Preview(showBackground = true) @Composable private fun HomeErrorPreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Error(UiText.Resource(R.string.home_error_generic), true), {}) } }
@Preview(showBackground = true) @Composable private fun HomeOfflinePreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Content(null, listOf(CategoryUiModel(null, "Todos")), null, listOf(previewMerchant), emptyList(), "", false, true), {}) } }
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES) @Composable private fun HomeDarkPreview() { ConsumaAquiTheme { HomeMerchantCard(previewMerchant, {}) } }
@Preview(showBackground = true) @Composable private fun HomeWithoutLocationPreview() { ConsumaAquiTheme { HomeScreen(HomeUiState.Content(null, listOf(CategoryUiModel(null, "Todos")), null, listOf(previewMerchant.copy(distanceText = null)), emptyList(), "", false, false), {}) } }
@Preview(showBackground = true) @Composable private fun MerchantOpenPreview() { ConsumaAquiTheme { HomeMerchantCard(previewMerchant, {}) } }
@Preview(showBackground = true) @Composable private fun MerchantClosedPreview() { ConsumaAquiTheme { HomeMerchantCard(previewMerchant.copy(availabilityLabel = UiText.Resource(R.string.home_availability_closed), availabilitySemantic = ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic.ERROR), {}) } }
@Preview(showBackground = true) @Composable private fun MerchantPromotionPreview() { ConsumaAquiTheme { HomeMerchantCard(previewMerchant.copy(promotionText = "DESTAQUE"), {}) } }
@Preview(showBackground = true) @Composable private fun MerchantWithoutImagePreview() { ConsumaAquiTheme { HomeMerchantCard(previewMerchant.copy(hasImage = false), {}) } }
@Preview(showBackground = true) @Composable private fun MerchantWithoutRatingPreview() { ConsumaAquiTheme { HomeMerchantCard(previewMerchant.copy(ratingText = null), {}) } }
