package ao.consuma.aqui.feature.catalog.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaClickableCard
import ao.consuma.aqui.core.designsystem.components.ConsumaFilterChip
import ao.consuma.aqui.core.designsystem.components.ConsumaImagePlaceholder
import ao.consuma.aqui.core.designsystem.components.ConsumaPriceText
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusChip
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogCategoryUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogProductCardUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.resolve
import ao.consuma.aqui.feature.catalog.presentation.mapper.CatalogUiText

@Composable
fun CatalogCategorySelector(
    categories: List<CatalogCategoryUiModel>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = ConsumaSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)
    ) {
        items(categories, key = { it.id ?: "all" }) { category ->
            val selected = category.id == selectedCategoryId
            val label = category.name.resolve()
            val selectionDescription = stringResource(
                if (selected) R.string.filter_selected else R.string.filter_not_selected
            )
            ConsumaFilterChip(
                selected = selected,
                onClick = { onCategorySelected(category.id) },
                label = label,
                enabled = category.available,
                modifier = Modifier
                    .testTag("${NavigationTestTags.CATALOG_CATEGORY}_${category.id ?: "all"}")
                    .semantics {
                        contentDescription = label
                        stateDescription = selectionDescription
                    }
            )
        }
    }
}

@Composable
fun CatalogProductCard(
    product: CatalogProductCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibility = product.accessibilityDescription.resolve()
    ConsumaClickableCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("${NavigationTestTags.CATALOG_PRODUCT}_${product.id}")
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = accessibility
            }
    ) {
        ConsumaImagePlaceholder(
            modifier = Modifier.fillMaxWidth().height(ConsumaSize.merchantCompactImageHeight),
            contentDescription = if (product.hasImage) {
                stringResource(R.string.catalog_product_image, product.name)
            } else null
        )
        Column(
            modifier = Modifier.padding(ConsumaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Text(product.categoryName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (product.featured) {
                    ConsumaStatusChip(stringResource(R.string.catalog_featured), ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic.INFO)
                }
            }
            product.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ConsumaPriceText(product.priceText, oldPrice = product.compareAtPriceText)
            Row(horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                ConsumaStatusChip(product.availabilityLabel.resolve(), product.availabilitySemantic)
                product.preparationText?.let { Text(it.resolve(), style = MaterialTheme.typography.labelMedium) }
            }
            if (product.hasOptions) {
                Text(stringResource(R.string.catalog_product_has_options), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private val previewCard = CatalogProductCardUiModel(
    id = "product",
    merchantId = "merchant",
    name = "Muamba da Casa",
    description = "Produto demonstrativo",
    hasImage = true,
    priceText = "4.500 Kz",
    compareAtPriceText = null,
    availabilityLabel = CatalogUiText.Resource(R.string.product_available),
    availabilitySemantic = ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic.SUCCESS,
    preparationText = CatalogUiText.Resource(R.string.catalog_preparation_minutes, listOf(20)),
    hasOptions = false,
    featured = false,
    categoryName = "Pratos",
    accessibilityDescription = CatalogUiText.Dynamic("Muamba da Casa, 4.500 Kz")
)

@Preview(showBackground = true) @Composable private fun AvailableCardPreview() { ConsumaAquiTheme { CatalogProductCard(previewCard, {}) } }
@Preview(showBackground = true) @Composable private fun UnavailableCardPreview() { ConsumaAquiTheme { CatalogProductCard(previewCard.copy(availabilityLabel = CatalogUiText.Resource(R.string.product_out_of_stock), availabilitySemantic = ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic.ERROR), {}) } }
@Preview(showBackground = true) @Composable private fun DiscountCardPreview() { ConsumaAquiTheme { CatalogProductCard(previewCard.copy(compareAtPriceText = "5.000 Kz"), {}) } }
@Preview(showBackground = true) @Composable private fun MissingImageCardPreview() { ConsumaAquiTheme { CatalogProductCard(previewCard.copy(hasImage = false), {}) } }
@Preview(showBackground = true) @Composable private fun OptionsCardPreview() { ConsumaAquiTheme { CatalogProductCard(previewCard.copy(hasOptions = true), {}) } }
@Preview(showBackground = true) @Composable private fun FeaturedCardPreview() { ConsumaAquiTheme { CatalogProductCard(previewCard.copy(featured = true), {}) } }
