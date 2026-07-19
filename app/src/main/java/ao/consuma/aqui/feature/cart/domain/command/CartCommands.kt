package ao.consuma.aqui.feature.cart.domain.command

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.domain.model.CartMerchant
import ao.consuma.aqui.feature.catalog.domain.model.ConfiguredProduct

data class CartOptionSnapshot(
    val groupId: String,
    val groupName: String,
    val optionId: String,
    val optionName: String,
    val additionalPrice: MoneyAmount?
)

data class ProductCartSnapshot(
    val productName: String,
    val imageUrl: String?,
    val selectedOptions: List<CartOptionSnapshot>
)

data class AddCartItemCommand(
    val merchant: CartMerchant,
    val configuredProduct: ConfiguredProduct,
    val snapshot: ProductCartSnapshot
)

data class UpdateCartItemQuantityCommand(
    val cartItemId: String,
    val quantity: Int
)

data class RemoveCartItemCommand(val cartItemId: String)

data class ReplaceCartItemCommand(
    val cartItemId: String,
    val merchant: CartMerchant,
    val configuredProduct: ConfiguredProduct,
    val snapshot: ProductCartSnapshot
)

data object ClearCartCommand
