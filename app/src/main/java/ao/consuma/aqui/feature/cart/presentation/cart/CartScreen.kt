package ao.consuma.aqui.feature.cart.presentation.cart

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaErrorState
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingState
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTextButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.cart.presentation.components.CartItemCard
import ao.consuma.aqui.feature.cart.presentation.components.CartMerchantHeader
import ao.consuma.aqui.feature.cart.presentation.components.CartSummaryCard
import ao.consuma.aqui.feature.cart.presentation.components.ClearCartDialog
import ao.consuma.aqui.feature.cart.presentation.components.RemoveCartItemDialog
import ao.consuma.aqui.feature.cart.presentation.mapper.CartItemUiModel
import ao.consuma.aqui.feature.cart.presentation.mapper.CartMerchantUiModel
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText
import ao.consuma.aqui.feature.cart.presentation.mapper.resolve

@Composable
fun CartScreen(
    uiState: CartUiState,
    onNavigateBack: () -> Unit,
    onEvent: (CartUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(NavigationTestTags.CART),
        topBar = { ConsumaTopAppBar(stringResource(R.string.cart_title), onBackClick = onNavigateBack) }
    ) { padding ->
        when (uiState) {
            CartUiState.Loading -> ConsumaLoadingState(Modifier.padding(padding))
            is CartUiState.Empty -> CartEmpty(uiState, onEvent, Modifier.padding(padding))
            is CartUiState.Content -> CartContent(uiState, onEvent, Modifier.padding(padding))
            is CartUiState.Error -> ConsumaErrorState(
                uiState.message.resolve(),
                if (uiState.canRetry) {{ onEvent(CartUiEvent.Retry) }} else null,
                Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun CartEmpty(state: CartUiState.Empty, onEvent: (CartUiEvent) -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxSize().testTag(NavigationTestTags.CART_EMPTY), contentAlignment = Alignment.Center) {
        ConsumaEmptyState(
            title = stringResource(R.string.cart_empty_title),
            description = state.message.resolve(),
            actionText = stringResource(R.string.cart_explore_merchants),
            onActionClick = { onEvent(CartUiEvent.ExploreMerchants) }
        )
    }
}

@Composable
private fun CartContent(state: CartUiState.Content, onEvent: (CartUiEvent) -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(NavigationTestTags.CART_LIST),
        contentPadding = PaddingValues(ConsumaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        item("merchant") { CartMerchantHeader(state.merchant.name) }
        items(state.items, key = { it.id }) { item ->
            val mutating = when (val pending = state.pendingOperation) {
                is CartPendingOperation.UpdatingQuantity -> pending.itemId == item.id
                is CartPendingOperation.Removing -> pending.itemId == item.id
                CartPendingOperation.Clearing -> true
                null -> false
            }
            CartItemCard(
                item = item,
                mutating = mutating || state.isMutating,
                onIncrease = { onEvent(CartUiEvent.IncreaseQuantity(item.id)) },
                onDecrease = { onEvent(CartUiEvent.DecreaseQuantity(item.id)) },
                onEdit = { onEvent(CartUiEvent.EditItem(item.id)) },
                onRemove = { onEvent(CartUiEvent.RequestRemoveItem(item.id)) }
            )
        }
        item("estimate") {
            Text(stringResource(R.string.cart_estimate_label), style = MaterialTheme.typography.labelLarge)
        }
        item("summary") { CartSummaryCard(state.itemCount, state.distinctItemCount, state.subtotalText) }
        item("continue") {
            ConsumaPrimaryButton(
                stringResource(R.string.cart_continue_shopping),
                { onEvent(CartUiEvent.ContinueShopping) },
                Modifier.testTag(NavigationTestTags.CART_CONTINUE),
                enabled = !state.isMutating
            )
        }
        item("clear") {
            ConsumaTextButton(
                stringResource(R.string.cart_clear),
                { onEvent(CartUiEvent.RequestClearCart) },
                Modifier.testTag(NavigationTestTags.CART_CLEAR),
                enabled = !state.isMutating
            )
        }
    }
    state.pendingRemovalItemId?.let { itemId ->
        val item = state.items.find { it.id == itemId } ?: return@let
        RemoveCartItemDialog(
            item.productName,
            state.pendingOperation is CartPendingOperation.Removing,
            { onEvent(CartUiEvent.ConfirmRemoveItem) },
            { onEvent(CartUiEvent.CancelRemoveItem) }
        )
    }
    if (state.showClearConfirmation) ClearCartDialog(
        state.pendingOperation == CartPendingOperation.Clearing,
        { onEvent(CartUiEvent.ConfirmClearCart) },
        { onEvent(CartUiEvent.CancelClearCart) }
    )
}

private val previewItem = CartItemUiModel(
    "item", "merchant", "product", "Muamba da Casa", false, null,
    listOf("Tamanho: Grande", "Molho: Picante"), "Sem cebola", 2,
    "4.500 Kz", "9.000 Kz", true, true, CartUiText.Dynamic("Muamba da Casa")
)
private val previewContent = CartUiState.Content(
    "cart", CartMerchantUiModel("merchant", "Sabor da Maianga"), listOf(previewItem),
    2, 1, "9.000 Kz", "Subtotal estimado 9.000 Kz", 1
)

@Preview(showBackground = true) @Composable private fun CartEmptyPreview() { ConsumaAquiTheme { CartScreen(CartUiState.Empty(CartUiText.Resource(R.string.cart_empty_description)), {}, {}) } }
@Preview(showBackground = true) @Composable private fun CartContentPreview() { ConsumaAquiTheme { CartScreen(previewContent, {}, {}) } }
@Preview(showBackground = true) @Composable private fun CartMultiplePreview() { ConsumaAquiTheme { CartScreen(previewContent.copy(items = listOf(previewItem, previewItem.copy(id = "item2", noteText = null))), {}, {}) } }
@Preview(showBackground = true) @Composable private fun CartMutatingPreview() { ConsumaAquiTheme { CartScreen(previewContent.copy(pendingOperation = CartPendingOperation.UpdatingQuantity("item")), {}, {}) } }
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES) @Composable private fun CartDarkPreview() { ConsumaAquiTheme { CartScreen(previewContent, {}, {}) } }
@Preview(showBackground = true, fontScale = 1.5f) @Composable private fun CartLargeFontPreview() { ConsumaAquiTheme { CartScreen(previewContent, {}, {}) } }
