package ao.consuma.aqui.feature.cart.domain.service

import ao.consuma.aqui.feature.cart.domain.command.AddCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.ProductCartSnapshot
import ao.consuma.aqui.feature.cart.domain.model.CartMerchant
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.catalog.domain.model.ConfiguredProduct
import javax.inject.Inject

/** Integration boundary used by Product Detail without exposing CartItem construction. */
class AddConfiguredProductToCart @Inject constructor(
    private val repository: CartRepository
) {
    suspend operator fun invoke(
        merchant: CartMerchant,
        configuredProduct: ConfiguredProduct,
        snapshot: ProductCartSnapshot
    ): AddCartItemResult = repository.addItem(
        AddCartItemCommand(merchant, configuredProduct, snapshot)
    )
}
