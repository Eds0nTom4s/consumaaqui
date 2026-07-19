package ao.consuma.aqui.feature.cart.domain.repository

import ao.consuma.aqui.feature.cart.domain.command.AddCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.ClearCartCommand
import ao.consuma.aqui.feature.cart.domain.command.RemoveCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.ReplaceCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.UpdateCartItemQuantityCommand
import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.cart.domain.result.CartResult
import kotlinx.coroutines.flow.StateFlow

interface CartRepository {
    val cart: StateFlow<Cart>

    suspend fun addItem(command: AddCartItemCommand): AddCartItemResult
    suspend fun replaceCartWithItem(command: AddCartItemCommand): CartResult<Cart>
    suspend fun updateQuantity(command: UpdateCartItemQuantityCommand): CartResult<Cart>
    suspend fun removeItem(command: RemoveCartItemCommand): CartResult<Cart>
    suspend fun replaceItem(command: ReplaceCartItemCommand): CartResult<Cart>
    suspend fun clearCart(command: ClearCartCommand = ClearCartCommand): CartResult<Cart>
}
