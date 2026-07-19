package ao.consuma.aqui.feature.cart.presentation.cart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CartRoute(
    onNavigateBack: () -> Unit,
    onExploreMerchants: () -> Unit,
    onContinueShopping: (String) -> Unit,
    onEditItem: (String, String, String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CartUiEffect.EditItem -> onEditItem(effect.merchantId, effect.productId, effect.cartItemId)
                is CartUiEffect.ContinueShopping -> onContinueShopping(effect.merchantId)
                CartUiEffect.ExploreMerchants -> onExploreMerchants()
                is CartUiEffect.Message -> onShowMessage(when (val text = effect.text) {
                    is ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText.Resource ->
                        context.getString(text.id, *text.args.toTypedArray())
                    is ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText.Plural ->
                        context.resources.getQuantityString(text.id, text.quantity, *text.args.toTypedArray())
                    is ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText.Dynamic -> text.value
                })
            }
        }
    }
    CartScreen(state, onNavigateBack, viewModel::onEvent, modifier)
}
