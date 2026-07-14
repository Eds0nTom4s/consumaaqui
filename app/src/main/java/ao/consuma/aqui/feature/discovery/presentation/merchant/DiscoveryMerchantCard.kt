package ao.consuma.aqui.feature.discovery.presentation.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.*
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.feature.discovery.presentation.mapper.*

@Composable
fun MerchantCompactCard(
    model: MerchantCompactUiModel,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val accessibility = model.accessibilityLabel.resolve()
    ConsumaClickableCard(
        onClick = onClick,
        modifier = modifier.testTag(testTag).semantics {
            contentDescription = accessibility
            role = Role.Button
        }
    ) {
        ConsumaImagePlaceholder(
            modifier = Modifier.fillMaxWidth().height(ConsumaSize.merchantCompactImageHeight),
            contentDescription = null
        )
        Column(Modifier.padding(ConsumaSpacing.sm), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)) {
            Text(model.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(model.categoryLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            ConsumaStatusChip(model.availabilityLabel.resolve(), model.availabilitySemantic)
            model.primaryFulfillmentLabel?.let { Text(it.resolve(), style = MaterialTheme.typography.labelSmall) }
            model.convenienceText?.let { Text(it.resolve(), style = MaterialTheme.typography.bodySmall) }
            model.promotionText?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun MerchantListCard(
    model: MerchantListItemUiModel,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val accessibility = model.accessibilityLabel.resolve()
    ConsumaClickableCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag(testTag).semantics {
            contentDescription = accessibility
            role = Role.Button
        }
    ) {
        Row(Modifier.fillMaxWidth().padding(ConsumaSpacing.md), horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)) {
            ConsumaImagePlaceholder(
                modifier = Modifier.size(ConsumaSize.iconXLarge),
                contentDescription = null
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(model.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    ConsumaStatusChip(model.availabilityLabel.resolve(), model.availabilitySemantic)
                }
                Text(model.categoryLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (model.subtitle.isNotBlank()) Text(model.subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                model.fulfillmentText?.let { Text(it.resolve(), style = MaterialTheme.typography.labelMedium) }
                model.convenienceText?.let { Text(it.resolve(), style = MaterialTheme.typography.bodySmall) }
                model.promotionText?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

private val previewListMerchant = MerchantListItemUiModel(
    "preview", "Sabor da Maianga", "Sabores angolanos preparados no dia", "Restaurantes", false,
    UiText.Resource(R.string.home_availability_open), ConsumaStatusSemantic.SUCCESS,
    UiText.Resource(R.string.home_fulfillment_delivery), UiText.Dynamic("450 m · 25 min · 4,7 (125)"), "DESTAQUE",
    UiText.Resource(R.string.merchant_card_accessibility, listOf("Sabor da Maianga", "Restaurantes"))
)

@Preview(showBackground = true) @Composable private fun ListOpenPreview() { ConsumaAquiTheme { MerchantListCard(previewListMerchant, {}, "preview") } }
@Preview(showBackground = true) @Composable private fun ListClosedPreview() { ConsumaAquiTheme { MerchantListCard(previewListMerchant.copy(availabilityLabel = UiText.Resource(R.string.home_availability_closed), availabilitySemantic = ConsumaStatusSemantic.ERROR), {}, "preview") } }
@Preview(showBackground = true) @Composable private fun ListPromotionPreview() { ConsumaAquiTheme { MerchantListCard(previewListMerchant.copy(promotionText = "MENU DO DIA"), {}, "preview") } }
@Preview(showBackground = true) @Composable private fun ListWithoutImagePreview() { ConsumaAquiTheme { MerchantListCard(previewListMerchant.copy(hasImage = false), {}, "preview") } }
@Preview(showBackground = true) @Composable private fun ListWithoutRatingPreview() { ConsumaAquiTheme { MerchantListCard(previewListMerchant.copy(convenienceText = UiText.Dynamic("450 m · 25 min")), {}, "preview") } }
