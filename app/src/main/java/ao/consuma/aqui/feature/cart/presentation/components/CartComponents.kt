package ao.consuma.aqui.feature.cart.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaImagePlaceholder
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.cart.domain.model.CartItem
import ao.consuma.aqui.feature.cart.presentation.badge.CartBadgeViewModel
import ao.consuma.aqui.feature.cart.presentation.mapper.CartBadgeUiState
import ao.consuma.aqui.feature.cart.presentation.mapper.CartItemUiModel
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText
import ao.consuma.aqui.feature.cart.presentation.mapper.resolve

@Composable
fun CartActionRoute(
    onClick: () -> Unit,
    enabled: Boolean = true,
    viewModel: CartBadgeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CartActionButton(state, onClick, enabled)
}

@Composable
fun CartActionButton(state: CartBadgeUiState, onClick: () -> Unit, enabled: Boolean = true) {
    val accessibility = state.accessibilityDescription.resolve()
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTag(NavigationTestTags.CART_BADGE)
            .semantics { contentDescription = accessibility }
    ) {
        BadgedBox(badge = {
            if (state.visible) Badge { Text(state.displayText.orEmpty()) }
        }) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null)
        }
    }
}

@Composable
fun CartMerchantHeader(name: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth().testTag(NavigationTestTags.CART_MERCHANT)) {
        Column(Modifier.padding(ConsumaSpacing.md), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.cart_single_merchant_notice), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItemUiModel,
    mutating: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier.fillMaxWidth().testTag("${NavigationTestTags.CART_ITEM}_${item.id}")
    ) {
        Column(Modifier.padding(ConsumaSpacing.md), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                ConsumaImagePlaceholder(Modifier.size(ConsumaSize.avatarLarge), contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text(item.productName, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.cart_unit_price, item.unitPriceText), style = MaterialTheme.typography.bodySmall)
                    Text(item.totalPriceText, style = MaterialTheme.typography.titleSmall)
                }
            }
            item.configurationText.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            item.noteText?.let { Text(stringResource(R.string.cart_note, it), style = MaterialTheme.typography.bodySmall) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CartQuantitySelector(item, mutating, onDecrease, onIncrease)
                Box(Modifier.weight(1f))
                IconButton(
                    onClick = onEdit, enabled = !mutating,
                    modifier = Modifier.testTag(NavigationTestTags.CART_ITEM_EDIT)
                ) { Icon(Icons.Default.Edit, stringResource(R.string.cart_edit)) }
                IconButton(
                    onClick = onRemove, enabled = !mutating,
                    modifier = Modifier.testTag(NavigationTestTags.CART_ITEM_REMOVE)
                ) { Icon(Icons.Default.Delete, stringResource(R.string.cart_remove)) }
            }
        }
    }
}

@Composable
fun CartQuantitySelector(
    item: CartItemUiModel,
    mutating: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val quantityAccessibility = stringResource(R.string.product_quantity_accessibility, item.quantity)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onDecrease,
            enabled = item.canDecrease && !mutating,
            modifier = Modifier.testTag(NavigationTestTags.CART_ITEM_DECREASE)
        ) { Icon(Icons.Default.Close, stringResource(R.string.product_quantity_decrease)) }
        Text(
            item.quantity.toString(),
            modifier = Modifier.semantics { contentDescription = quantityAccessibility }
        )
        IconButton(
            onClick = onIncrease,
            enabled = item.canIncrease && !mutating,
            modifier = Modifier.testTag(NavigationTestTags.CART_ITEM_INCREASE)
        ) { Icon(Icons.Default.Add, stringResource(R.string.product_quantity_increase)) }
    }
}

@Composable
fun CartSummaryCard(itemCount: Int, distinctCount: Int, subtotal: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth().testTag(NavigationTestTags.CART_SUBTOTAL)) {
        Column(Modifier.padding(ConsumaSpacing.md), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
            SummaryRow(stringResource(R.string.cart_items), itemCount.toString())
            SummaryRow(stringResource(R.string.cart_distinct_items), distinctCount.toString())
            SummaryRow(stringResource(R.string.cart_estimated_subtotal), subtotal)
            Text(stringResource(R.string.cart_price_notice), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.cart_checkout_future), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label); Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RemoveCartItemDialog(productName: String, processing: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier.testTag(NavigationTestTags.CART_REMOVE_DIALOG),
        onDismissRequest = { if (!processing) onDismiss() },
        title = { Text(stringResource(R.string.cart_remove_dialog_title)) },
        text = { Text(stringResource(R.string.cart_remove_dialog_message, productName)) },
        confirmButton = { TextButton(onConfirm, enabled = !processing) { Text(stringResource(R.string.cart_remove)) } },
        dismissButton = { TextButton(onDismiss, enabled = !processing) { Text(stringResource(R.string.cart_keep_item)) } }
    )
}

@Composable
fun ClearCartDialog(processing: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier.testTag(NavigationTestTags.CART_CLEAR_DIALOG),
        onDismissRequest = { if (!processing) onDismiss() },
        title = { Text(stringResource(R.string.cart_clear_dialog_title)) },
        text = { Text(stringResource(R.string.cart_clear_dialog_message)) },
        confirmButton = { TextButton(onConfirm, enabled = !processing) { Text(stringResource(R.string.cart_clear)) } },
        dismissButton = { TextButton(onDismiss, enabled = !processing) { Text(stringResource(R.string.cart_keep_items)) } }
    )
}

@Composable
fun CartConflictDialog(
    currentMerchantName: String,
    requestedMerchantName: String,
    processing: Boolean,
    onKeep: () -> Unit,
    onReplace: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.testTag(NavigationTestTags.CART_CONFLICT),
        onDismissRequest = { if (!processing) onKeep() },
        title = { Text(stringResource(R.string.cart_conflict_title)) },
        text = { Text(stringResource(R.string.cart_conflict_message, currentMerchantName, requestedMerchantName)) },
        confirmButton = {
            TextButton(onReplace, enabled = !processing, modifier = Modifier.testTag(NavigationTestTags.CART_CONFLICT_REPLACE)) {
                Text(stringResource(R.string.cart_conflict_replace))
            }
        },
        dismissButton = {
            TextButton(onKeep, enabled = !processing, modifier = Modifier.testTag(NavigationTestTags.CART_CONFLICT_KEEP)) {
                Text(stringResource(R.string.cart_conflict_keep))
            }
        }
    )
}

private val componentPreviewItem = CartItemUiModel(
    id = "preview-item",
    merchantId = "preview-merchant",
    productId = "preview-product",
    productName = "Muamba da Casa",
    hasImage = false,
    imageUrl = null,
    configurationText = listOf("Tamanho: Grande", "Adicionais: Queijo, Ovo"),
    noteText = "Sem cebola",
    quantity = 1,
    unitPriceText = "4.500 Kz",
    totalPriceText = "4.500 Kz",
    canDecrease = false,
    canIncrease = true,
    accessibilityDescription = CartUiText.Resource(
        R.string.cart_item_accessibility,
        listOf("Muamba da Casa", 1, "4.500 Kz", "4.500 Kz")
    )
)

@Preview(showBackground = true) @Composable private fun CartItemSimplePreview() {
    ConsumaAquiTheme { CartItemCard(componentPreviewItem, false, {}, {}, {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CartItemMaximumPreview() {
    ConsumaAquiTheme {
        CartItemCard(
            componentPreviewItem.copy(quantity = CartItem.MAX_QUANTITY, canDecrease = true, canIncrease = false),
            false, {}, {}, {}, {}
        )
    }
}
@Preview(showBackground = true) @Composable private fun CartItemMutatingPreview() {
    ConsumaAquiTheme { CartItemCard(componentPreviewItem, true, {}, {}, {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CartConflictPreview() {
    ConsumaAquiTheme { CartConflictDialog("Sabor da Maianga", "Café de Angola", false, {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CartConflictProcessingPreview() {
    ConsumaAquiTheme { CartConflictDialog("Sabor da Maianga", "Café de Angola", true, {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CartActionEmptyPreview() {
    ConsumaAquiTheme {
        CartActionButton(
            CartBadgeUiState(accessibilityDescription = CartUiText.Resource(R.string.cart_badge_empty_accessibility)),
            {}
        )
    }
}
@Preview(showBackground = true) @Composable private fun CartActionOnePreview() {
    ConsumaAquiTheme {
        CartActionButton(
            CartBadgeUiState(1, "1", true, CartUiText.Plural(R.plurals.cart_badge_accessibility, 1)),
            {}
        )
    }
}
@Preview(showBackground = true) @Composable private fun CartActionOverflowPreview() {
    ConsumaAquiTheme {
        CartActionButton(
            CartBadgeUiState(100, "99+", true, CartUiText.Resource(R.string.cart_badge_overflow_accessibility)),
            {}
        )
    }
}
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun CartActionDarkPreview() {
    ConsumaAquiTheme {
        CartActionButton(
            CartBadgeUiState(99, "99", true, CartUiText.Plural(R.plurals.cart_badge_accessibility, 99)),
            {}
        )
    }
}
