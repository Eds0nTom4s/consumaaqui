package ao.consuma.aqui.feature.checkout.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaCard
import ao.consuma.aqui.core.designsystem.components.ConsumaInlineMessage
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.components.ConsumaTextButton
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.presentation.checkout.CheckoutUiEvent
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCartSummaryUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCustomerReviewUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutFulfillmentReviewUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutQuoteUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.resolve
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutItemUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutChargeUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiText
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme

@Composable
fun CheckoutItemsSummary(
    summary: CheckoutCartSummaryUiModel,
    modifier: Modifier = Modifier
) {
    ConsumaCard(modifier.fillMaxWidth().testTag(NavigationTestTags.CHECKOUT_ITEMS)) {
        Column(
            Modifier.fillMaxWidth().padding(ConsumaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)
        ) {
            Text(stringResource(R.string.checkout_items_title), style = MaterialTheme.typography.titleMedium)
            summary.items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider()
                Text(item.productName, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.checkout_item_quantity, item.quantity))
                item.selections.forEach { selection ->
                    Text(
                        stringResource(
                            R.string.checkout_item_selection,
                            selection.groupName,
                            selection.optionName
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item.note?.let {
                    Text(stringResource(R.string.checkout_item_note, it), style = MaterialTheme.typography.bodySmall)
                }
                SummaryRow(stringResource(R.string.checkout_total_estimated), item.totalPrice)
            }
        }
    }
}

@Composable
fun CheckoutQuoteSummary(
    quote: CheckoutQuoteUiModel,
    onRefresh: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val totalDescription = stringResource(R.string.checkout_total_accessibility, quote.total)
    ConsumaCard(modifier.fillMaxWidth().testTag(NavigationTestTags.CHECKOUT_QUOTE)) {
        Column(
            Modifier.fillMaxWidth().padding(ConsumaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)
        ) {
            Text(stringResource(R.string.checkout_quote_title), style = MaterialTheme.typography.titleMedium)
            SummaryRow(
                stringResource(R.string.checkout_subtotal_estimated),
                quote.subtotal,
                Modifier.testTag(NavigationTestTags.CHECKOUT_SUBTOTAL)
            )
            quote.charges.forEach { charge ->
                SummaryRow(
                    charge.label.resolve(),
                    charge.amount,
                    Modifier.testTag(NavigationTestTags.CHECKOUT_CHARGES)
                )
            }
            HorizontalDivider()
            SummaryRow(
                stringResource(R.string.checkout_total_estimated),
                quote.total,
                Modifier.testTag(NavigationTestTags.CHECKOUT_TOTAL)
                    .semantics { contentDescription = totalDescription },
                emphasize = true
            )
            quote.preparationMinutes?.let {
                Text(stringResource(R.string.checkout_preparation_estimate, it))
            }
            quote.deliveryMinutes?.let {
                Text(stringResource(R.string.checkout_delivery_estimate, it))
            }
            if (quote.expired) {
                ConsumaInlineMessage(
                    stringResource(R.string.checkout_quote_expired),
                    ConsumaStatusSemantic.ERROR
                )
                if (onRefresh != null) ConsumaTextButton(
                    stringResource(R.string.checkout_refresh_quote),
                    onRefresh,
                    Modifier.testTag(NavigationTestTags.CHECKOUT_REFRESH_QUOTE)
                )
            } else if (quote.expiresAtText != null) {
                Text(
                    stringResource(R.string.checkout_quote_limited),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(R.string.checkout_quote_expires_at, quote.expiresAtText),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CheckoutReviewSection(
    cart: CheckoutCartSummaryUiModel,
    customer: CheckoutCustomerReviewUiModel?,
    fulfillment: CheckoutFulfillmentReviewUiModel?,
    quote: CheckoutQuoteUiModel?,
    onEvent: (CheckoutUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().testTag(NavigationTestTags.CHECKOUT_REVIEW),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        Text(stringResource(R.string.checkout_review_title), style = MaterialTheme.typography.titleLarge)
        ConsumaCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(ConsumaSpacing.md)) {
                Text(stringResource(R.string.checkout_merchant_label), style = MaterialTheme.typography.labelLarge)
                Text(cart.merchantName, style = MaterialTheme.typography.titleMedium)
            }
        }
        CheckoutItemsSummary(cart)
        ReviewCustomer(customer) { onEvent(CheckoutUiEvent.EditStep(CheckoutStep.CUSTOMER)) }
        ReviewFulfillment(fulfillment) {
            onEvent(CheckoutUiEvent.EditStep(CheckoutStep.FULFILLMENT))
        }
        ConsumaTextButton(
            stringResource(R.string.checkout_edit_details),
            { onEvent(CheckoutUiEvent.EditStep(CheckoutStep.DETAILS)) }
        )
        quote?.let { CheckoutQuoteSummary(it, onRefresh = null) }
        ConsumaInlineMessage(
            stringResource(R.string.checkout_financial_notice),
            ConsumaStatusSemantic.WARNING
        )
    }
}

@Composable
private fun ReviewCustomer(
    customer: CheckoutCustomerReviewUiModel?,
    onEdit: () -> Unit
) {
    if (customer == null) return
    ConsumaCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(ConsumaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)
        ) {
            Text(stringResource(R.string.checkout_customer_review_title), style = MaterialTheme.typography.titleMedium)
            Text(customer.fullName)
            Text(customer.phone)
            customer.email?.let { Text(it) }
            ConsumaTextButton(stringResource(R.string.checkout_edit_customer), onEdit)
        }
    }
}

@Composable
private fun ReviewFulfillment(
    fulfillment: CheckoutFulfillmentReviewUiModel?,
    onEdit: () -> Unit
) {
    if (fulfillment == null) return
    ConsumaCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(ConsumaSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)
        ) {
            Text(stringResource(R.string.checkout_fulfillment_review_title), style = MaterialTheme.typography.titleMedium)
            when (fulfillment) {
                is CheckoutFulfillmentReviewUiModel.Pickup -> {
                    Text(stringResource(R.string.checkout_pickup_review), fontWeight = FontWeight.SemiBold)
                    Text(fulfillment.contactName)
                    Text(fulfillment.contactPhone)
                    Text(stringResource(R.string.checkout_pickup_asap))
                }
                is CheckoutFulfillmentReviewUiModel.Delivery -> {
                    Text(stringResource(R.string.checkout_delivery_review), fontWeight = FontWeight.SemiBold)
                    Text(fulfillment.recipientName)
                    Text(fulfillment.recipientPhone)
                    Text(stringResource(R.string.checkout_address_title), style = MaterialTheme.typography.labelLarge)
                    fulfillment.addressLines.forEach { Text(it) }
                    fulfillment.instructions?.let {
                        Text(stringResource(R.string.checkout_instructions_title), style = MaterialTheme.typography.labelLarge)
                        Text(it)
                    }
                    ConsumaInlineMessage(
                        stringResource(R.string.checkout_delivery_mock_notice),
                        ConsumaStatusSemantic.INFO
                    )
                }
            }
            ConsumaTextButton(stringResource(R.string.checkout_edit_fulfillment), onEdit)
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private val previewCartSummary = CheckoutCartSummaryUiModel(
    "Sabor da Maianga",
    listOf(CheckoutItemUiModel("item", "Muamba da Casa", 1, emptyList(), null, "4.500 Kz")),
    1,
    "4.500 Kz"
)
private val previewQuoteSummary = CheckoutQuoteUiModel(
    "4.500 Kz",
    listOf(
        CheckoutChargeUiModel(
            CheckoutUiText.Resource(R.string.checkout_delivery_charge),
            "1.500 Kz"
        )
    ),
    "6.000 Kz",
    25,
    35,
    "20:30",
    false
)

@Preview(showBackground = true) @Composable private fun CheckoutItemsSummaryPreview() {
    ConsumaAquiTheme { CheckoutItemsSummary(previewCartSummary) }
}
@Preview(showBackground = true) @Composable private fun CheckoutQuoteSummaryPreview() {
    ConsumaAquiTheme { CheckoutQuoteSummary(previewQuoteSummary, {}) }
}
