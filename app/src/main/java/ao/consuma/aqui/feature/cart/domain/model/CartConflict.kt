package ao.consuma.aqui.feature.cart.domain.model

sealed interface CartConflict {
    data class DifferentMerchant(
        val currentMerchant: CartMerchant,
        val requestedMerchant: CartMerchant
    ) : CartConflict
}
