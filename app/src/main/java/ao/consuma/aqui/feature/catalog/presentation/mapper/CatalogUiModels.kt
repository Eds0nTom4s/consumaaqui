package ao.consuma.aqui.feature.catalog.presentation.mapper

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic

sealed interface CatalogUiText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : CatalogUiText
    data class Plural(@PluralsRes val id: Int, val quantity: Int, val args: List<Any> = listOf(quantity)) : CatalogUiText
    data class Joined(val values: List<CatalogUiText>, val separator: String = ", ") : CatalogUiText
    data class Dynamic(val value: String) : CatalogUiText
}

@Composable
fun CatalogUiText.resolve(): String = when (this) {
    is CatalogUiText.Resource -> stringResource(id, *args.toTypedArray())
    is CatalogUiText.Plural -> pluralStringResource(id, quantity, *args.toTypedArray())
    is CatalogUiText.Joined -> values.map { it.resolve() }.joinToString(separator)
    is CatalogUiText.Dynamic -> value
}

data class CatalogCategoryUiModel(
    val id: String?,
    val name: CatalogUiText,
    val available: Boolean
)

data class CatalogProductCardUiModel(
    val id: String,
    val merchantId: String,
    val name: String,
    val description: String?,
    val hasImage: Boolean,
    val priceText: String,
    val compareAtPriceText: String?,
    val availabilityLabel: CatalogUiText,
    val availabilitySemantic: ConsumaStatusSemantic,
    val preparationText: CatalogUiText?,
    val hasOptions: Boolean,
    val featured: Boolean,
    val categoryName: String,
    val accessibilityDescription: CatalogUiText
)

data class CatalogContentUiModel(
    val merchantId: String,
    val title: String,
    val description: String?,
    val query: String,
    val categories: List<CatalogCategoryUiModel>,
    val selectedCategoryId: String?,
    val products: List<CatalogProductCardUiModel>,
    val isSearchMode: Boolean,
    val isRefreshing: Boolean,
    val isOffline: Boolean,
    val resultContext: CatalogUiText?
)

data class ProductOptionUiModel(
    val id: String,
    val name: String,
    val description: String?,
    val additionalPriceText: String?,
    val available: Boolean,
    val defaultSelected: Boolean
)

data class ProductOptionGroupUiModel(
    val id: String,
    val name: String,
    val description: String?,
    val required: Boolean,
    val minimumSelections: Int,
    val maximumSelections: Int,
    val singleChoice: Boolean,
    val ruleLabel: CatalogUiText,
    val options: List<ProductOptionUiModel>
)

data class ProductDetailUiModel(
    val id: String,
    val merchantId: String,
    val name: String,
    val fullDescription: String?,
    val hasImage: Boolean,
    val priceText: String,
    val compareAtPriceText: String?,
    val availabilityLabel: CatalogUiText,
    val availabilitySemantic: ConsumaStatusSemantic,
    val preparationText: CatalogUiText?,
    val optionGroups: List<ProductOptionGroupUiModel>,
    val canConfigure: Boolean
)

data class ProductConfigurationErrorUiModel(
    val groupId: String?,
    val message: CatalogUiText
)

data class ProductPriceSummaryUiModel(
    val unitPriceText: String,
    val optionsPriceText: String,
    val totalPriceText: String
)
