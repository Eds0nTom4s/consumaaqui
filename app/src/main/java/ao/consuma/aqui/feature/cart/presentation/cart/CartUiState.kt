package ao.consuma.aqui.feature.cart.presentation.cart

import ao.consuma.aqui.feature.cart.presentation.mapper.CartItemUiModel
import ao.consuma.aqui.feature.cart.presentation.mapper.CartMerchantUiModel
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText
import androidx.compose.runtime.Immutable

@Immutable
sealed interface CartPendingOperation {
    data class UpdatingQuantity(val itemId: String) : CartPendingOperation
    data class Removing(val itemId: String) : CartPendingOperation
    data object Clearing : CartPendingOperation
}

@Immutable
sealed interface CartUiState {
    data object Loading : CartUiState
    data class Empty(val message: CartUiText) : CartUiState
    data class Content(
        val cartId: String,
        val merchant: CartMerchantUiModel,
        val items: List<CartItemUiModel>,
        val itemCount: Int,
        val distinctItemCount: Int,
        val subtotalText: String,
        val subtotalAccessibilityText: String,
        val version: Long,
        val pendingOperation: CartPendingOperation? = null,
        val pendingRemovalItemId: String? = null,
        val showClearConfirmation: Boolean = false
    ) : CartUiState {
        val isMutating: Boolean get() = pendingOperation != null
    }
    data class Error(val message: CartUiText, val canRetry: Boolean) : CartUiState
}
