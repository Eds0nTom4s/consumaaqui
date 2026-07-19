package ao.consuma.aqui.feature.cart.domain.service

import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.cart.domain.model.CartConflict
import ao.consuma.aqui.feature.cart.domain.model.CartMerchant
import javax.inject.Inject

class CartMerchantPolicy @Inject constructor() {
    fun conflict(cart: Cart, requested: CartMerchant): CartConflict.DifferentMerchant? =
        cart.merchant?.takeIf { it.id != requested.id }?.let {
            CartConflict.DifferentMerchant(it, requested)
        }
}
