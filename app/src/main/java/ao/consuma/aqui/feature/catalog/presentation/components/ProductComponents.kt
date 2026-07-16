package ao.consuma.aqui.feature.catalog.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaCard
import ao.consuma.aqui.core.designsystem.components.ConsumaIconButton
import ao.consuma.aqui.core.designsystem.components.ConsumaPriceText
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.catalog.domain.model.ProductConfiguration
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductConfigurationErrorUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductOptionGroupUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductOptionUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.ProductPriceSummaryUiModel
import ao.consuma.aqui.feature.catalog.presentation.mapper.resolve

@Composable
fun ProductOptionGroupSection(
    group: ProductOptionGroupUiModel,
    selectedOptionIds: Set<String>,
    errorModel: ProductConfigurationErrorUiModel?,
    onOptionToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupError = errorModel?.message?.resolve()
    val groupDescription = stringResource(
        if (group.required) R.string.product_group_required_accessibility
        else R.string.product_group_optional_accessibility,
        group.name
    )
    ConsumaCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${NavigationTestTags.PRODUCT_OPTION_GROUP}_${group.id}")
            .semantics {
                contentDescription = groupDescription
                if (groupError != null) error(groupError)
            }
    ) {
        Column(Modifier.padding(ConsumaSpacing.md), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(group.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(if (group.required) R.string.product_required else R.string.product_optional),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            group.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(group.ruleLabel.resolve(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            group.options.forEach { option ->
                ProductOptionRow(group.singleChoice, option, option.id in selectedOptionIds) {
                    onOptionToggled(option.id)
                }
            }
            groupError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ProductOptionRow(
    singleChoice: Boolean,
    option: ProductOptionUiModel,
    selected: Boolean,
    onClick: () -> Unit
) {
    val unavailable = stringResource(R.string.product_option_unavailable)
    val selectionDescription = when {
        !option.available -> unavailable
        selected -> stringResource(R.string.filter_selected)
        else -> stringResource(R.string.filter_not_selected)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${NavigationTestTags.PRODUCT_OPTION}_${option.id}")
            .semantics {
                stateDescription = selectionDescription
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (singleChoice) {
            RadioButton(selected = selected, onClick = onClick, enabled = option.available)
        } else {
            Checkbox(checked = selected, onCheckedChange = { onClick() }, enabled = option.available)
        }
        Column(Modifier.weight(1f)) {
            Text(option.name, style = MaterialTheme.typography.bodyLarge)
            option.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (!option.available) Text(unavailable, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        option.additionalPriceText?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quantityDescription = stringResource(R.string.product_quantity_accessibility, quantity)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(NavigationTestTags.PRODUCT_QUANTITY)
            .semantics { contentDescription = quantityDescription },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.product_quantity), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ConsumaIconButton(
                Icons.Default.Close,
                stringResource(R.string.product_quantity_decrease),
                onDecrease,
                enabled = quantity > ProductConfiguration.MIN_QUANTITY
            )
            Text(quantity.toString(), style = MaterialTheme.typography.titleLarge)
            ConsumaIconButton(
                Icons.Default.Add,
                stringResource(R.string.product_quantity_increase),
                onIncrease,
                enabled = quantity < ProductConfiguration.MAX_QUANTITY
            )
        }
    }
}

@Composable
fun ProductPriceSummary(summary: ProductPriceSummaryUiModel, modifier: Modifier = Modifier) {
    val totalDescription = stringResource(R.string.product_total_accessibility, summary.totalPriceText)
    ConsumaCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag(NavigationTestTags.PRODUCT_TOTAL)
            .semantics { contentDescription = totalDescription }
    ) {
        Column(Modifier.padding(ConsumaSpacing.md), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.product_base_price))
                Text(summary.unitPriceText)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.product_options_price))
                Text(summary.optionsPriceText)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.product_estimated_total), style = MaterialTheme.typography.titleMedium)
                ConsumaPriceText(summary.totalPriceText)
            }
            Text(stringResource(R.string.product_price_estimate_notice), style = MaterialTheme.typography.bodySmall)
        }
    }
}
