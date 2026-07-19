package ao.consuma.aqui.feature.catalog.presentation.product

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaErrorState
import ao.consuma.aqui.core.designsystem.components.ConsumaImagePlaceholder
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingState
import ao.consuma.aqui.core.designsystem.components.ConsumaOfflineBanner
import ao.consuma.aqui.core.designsystem.components.ConsumaPriceText
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusChip
import ao.consuma.aqui.core.designsystem.components.ConsumaTextField
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.designsystem.tokens.ConsumaElevation
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.catalog.domain.model.ProductConfiguration
import ao.consuma.aqui.feature.catalog.presentation.components.ProductOptionGroupSection
import ao.consuma.aqui.feature.catalog.presentation.components.ProductPriceSummary
import ao.consuma.aqui.feature.catalog.presentation.components.QuantitySelector
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiText
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductDetailUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductOptionGroupUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductOptionUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductPriceSummaryUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.resolve
import ao.consuma.aqui.feature.cart.presentation.components.CartConflictDialog

@Composable
fun ProductDetailScreen(
    uiState: ProductDetailUiState,
    onNavigateBack: () -> Unit,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    cartAction: @Composable RowScope.() -> Unit = {}
) {
    val content = (uiState as? ProductDetailUiState.Content)?.data
    val unavailable = uiState as? ProductDetailUiState.Unavailable
    Scaffold(
        modifier = modifier.testTag(NavigationTestTags.PRODUCT_DETAIL),
        topBar = {
            ConsumaTopAppBar(
                stringResource(if (content?.mode == ProductDetailMode.EDIT) R.string.product_edit_title else R.string.product_detail_title),
                onBackClick = onNavigateBack,
                actions = cartAction
            )
        },
        bottomBar = {
            if (content != null) {
                Surface(shadowElevation = ConsumaElevation.medium) {
                    Column(Modifier.fillMaxWidth().padding(ConsumaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
                        Text(
                            stringResource(R.string.product_total_value, content.priceSummary.totalPriceText),
                            style = MaterialTheme.typography.titleMedium
                        )
                        ConsumaPrimaryButton(
                            stringResource(if (content.mode == ProductDetailMode.EDIT) R.string.product_save_changes else R.string.product_add),
                            { onEvent(ProductDetailUiEvent.Add) },
                            Modifier.testTag(NavigationTestTags.PRODUCT_ADD),
                            enabled = content.canAdd && !content.isSubmitting,
                            fullWidth = true
                        )
                    }
                }
            } else if (unavailable != null) {
                Surface(shadowElevation = ConsumaElevation.medium) {
                    ConsumaPrimaryButton(
                        stringResource(R.string.product_add),
                        {},
                        Modifier.fillMaxWidth().padding(ConsumaSpacing.lg).testTag(NavigationTestTags.PRODUCT_ADD),
                        enabled = false,
                        fullWidth = true
                    )
                }
            }
        }
    ) { padding ->
        when (uiState) {
            ProductDetailUiState.Loading -> ConsumaLoadingState(
                Modifier.padding(padding).testTag(NavigationTestTags.PRODUCT_LOADING)
            )
            ProductDetailUiState.NotFound -> ProductEmpty(
                R.string.product_not_found,
                R.string.product_not_found_description,
                onNavigateBack,
                Modifier.padding(padding)
            )
            ProductDetailUiState.InvalidArguments -> ProductEmpty(
                R.string.product_invalid_arguments,
                R.string.product_invalid_arguments_description,
                onNavigateBack,
                Modifier.padding(padding)
            )
            is ProductDetailUiState.Error -> ConsumaErrorState(
                uiState.message.resolve(),
                if (uiState.canRetry) {{ onEvent(ProductDetailUiEvent.Retry) }} else null,
                Modifier.padding(padding).testTag(NavigationTestTags.PRODUCT_ERROR)
            )
            is ProductDetailUiState.Unavailable -> UnavailableProduct(
                uiState.product,
                uiState.isOffline,
                Modifier.padding(padding)
            )
            is ProductDetailUiState.Content -> ProductBody(uiState.data, onEvent, Modifier.padding(padding))
        }
    }
    content?.conflict?.let { conflict ->
        CartConflictDialog(
            currentMerchantName = conflict.currentMerchantName,
            requestedMerchantName = conflict.requestedMerchantName,
            processing = conflict.processing,
            onKeep = { onEvent(ProductDetailUiEvent.KeepCurrentCart) },
            onReplace = { onEvent(ProductDetailUiEvent.ReplaceCart) }
        )
    }
}

@Composable
private fun ProductBody(
    data: ProductDetailContentUiModel,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(NavigationTestTags.PRODUCT_LIST),
        contentPadding = PaddingValues(ConsumaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.lg)
    ) {
        if (data.isOffline) item("offline") {
            Column(verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)) {
                ConsumaOfflineBanner(Modifier.testTag(NavigationTestTags.PRODUCT_OFFLINE))
                Text(stringResource(R.string.product_offline_notice), style = MaterialTheme.typography.bodySmall)
            }
        }
        item("identity") { ProductIdentity(data.product) }
        items(data.product.optionGroups, key = { it.id }) { group ->
            ProductOptionGroupSection(
                group = group,
                selectedOptionIds = data.selections[group.id].orEmpty(),
                errorModel = data.validationErrors.firstOrNull { it.groupId == group.id },
                onOptionToggled = { onEvent(ProductDetailUiEvent.OptionToggled(group.id, it)) }
            )
        }
        item("quantity") {
            QuantitySelector(
                data.quantity,
                { onEvent(ProductDetailUiEvent.QuantityDecreased) },
                { onEvent(ProductDetailUiEvent.QuantityIncreased) }
            )
        }
        item("note") {
            ConsumaTextField(
                value = data.note,
                onValueChange = { onEvent(ProductDetailUiEvent.NoteChanged(it)) },
                label = stringResource(R.string.product_note_label),
                placeholder = stringResource(R.string.product_note_placeholder),
                supportingText = pluralStringResource(
                    R.plurals.product_note_counter,
                    data.note.length,
                    data.note.length,
                    ProductConfiguration.MAX_NOTE_LENGTH
                ),
                singleLine = false,
                modifier = Modifier.testTag(NavigationTestTags.PRODUCT_NOTE)
            )
        }
        data.validationErrors.filter { it.groupId == null }.forEachIndexed { index, error ->
            item("global-error-$index") {
                Text(error.message.resolve(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item("summary") { ProductPriceSummary(data.priceSummary) }
        if (data.configurationAdjusted) item("adjusted") {
            Text(
                stringResource(R.string.product_configuration_adjusted),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun ProductIdentity(product: ProductDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)) {
        ConsumaImagePlaceholder(
            Modifier.fillMaxWidth().height(ConsumaSize.merchantOverviewBannerHeight),
            contentDescription = if (product.hasImage) stringResource(R.string.catalog_product_image, product.name) else null
        )
        Text(product.name, style = MaterialTheme.typography.headlineSmall)
        ConsumaPriceText(product.priceText, oldPrice = product.compareAtPriceText)
        ConsumaStatusChip(product.availabilityLabel.resolve(), product.availabilitySemantic)
        product.fullDescription?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        product.preparationText?.let { Text(it.resolve(), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun UnavailableProduct(product: ProductDetailUiModel, offline: Boolean, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(NavigationTestTags.PRODUCT_LIST),
        contentPadding = PaddingValues(ConsumaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.lg)
    ) {
        if (offline) item("offline") {
            ConsumaOfflineBanner(Modifier.testTag(NavigationTestTags.PRODUCT_OFFLINE))
        }
        item("identity") { ProductIdentity(product) }
        item("unavailable") {
            Text(stringResource(R.string.product_unavailable_configuration), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ProductEmpty(title: Int, description: Int, onBack: () -> Unit, modifier: Modifier) {
    ConsumaEmptyState(
        stringResource(title),
        modifier,
        stringResource(description),
        stringResource(R.string.back),
        onBack
    )
}

private val previewOption = ProductOptionUiModel("small", "Pequeno", null, null, true, true)
private val previewGroup = ProductOptionGroupUiModel(
    "size", "Tamanho", "Seleccione um tamanho", true, 1, 1, true,
    CatalogUiText.Resource(R.string.product_rule_choose_one), listOf(previewOption)
)
private val previewDetail = ProductDetailUiModel(
    "product", "merchant", "Muamba da Casa", "Descrição demonstrativa", false, "4.500 Kz", "5.000 Kz",
    CatalogUiText.Resource(R.string.product_available), ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic.SUCCESS,
    CatalogUiText.Resource(R.string.catalog_preparation_minutes, listOf(20)), listOf(previewGroup), true
)
private val previewContent = ProductDetailContentUiModel(
    previewDetail, mapOf("size" to setOf("small")), 1, "", emptyList(),
    ProductPriceSummaryUiModel("4.500 Kz", "0 Kz", "4.500 Kz"), true, false
)

@Preview(showBackground = true) @Composable private fun ProductLoadingPreview() { ConsumaAquiTheme { ProductDetailScreen(ProductDetailUiState.Loading, {}, {}) } }
@Preview(showBackground = true) @Composable private fun ProductSimplePreview() { ConsumaAquiTheme { ProductDetailScreen(ProductDetailUiState.Content(previewContent.copy(product = previewDetail.copy(optionGroups = emptyList()))), {}, {}) } }
@Preview(showBackground = true) @Composable private fun ProductGroupsPreview() { ConsumaAquiTheme { ProductDetailScreen(ProductDetailUiState.Content(previewContent), {}, {}) } }
@Preview(showBackground = true) @Composable private fun ProductUnavailablePreview() { ConsumaAquiTheme { ProductDetailScreen(ProductDetailUiState.Unavailable(previewDetail.copy(canConfigure = false), false), {}, {}) } }
@Preview(showBackground = true) @Composable private fun ProductInvalidPreview() { ConsumaAquiTheme { ProductDetailScreen(ProductDetailUiState.Content(previewContent.copy(validationErrors = listOf(ao.consuma.aqui.feature.catalog.presentation.mapper.ProductConfigurationErrorUiModel("size", CatalogUiText.Plural(R.plurals.product_error_minimum, 1))))), {}, {}) } }
@Preview(showBackground = true) @Composable private fun ProductOfflinePreview() { ConsumaAquiTheme { ProductDetailScreen(ProductDetailUiState.Content(previewContent.copy(isOffline = true)), {}, {}) } }
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES) @Composable private fun ProductDarkPreview() { ConsumaAquiTheme { ProductDetailScreen(ProductDetailUiState.Content(previewContent), {}, {}) } }
@Preview(showBackground = true, fontScale = 1.5f) @Composable private fun ProductLargeFontPreview() { ConsumaAquiTheme { ProductDetailScreen(ProductDetailUiState.Content(previewContent), {}, {}) } }
