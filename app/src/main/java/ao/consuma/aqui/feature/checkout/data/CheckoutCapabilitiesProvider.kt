package ao.consuma.aqui.feature.checkout.data

import ao.consuma.aqui.feature.checkout.domain.model.CheckoutCapabilities
import javax.inject.Inject

/** Checkout-owned mock fixture aligned with existing merchant ids; Discovery is not queried. */
class CheckoutCapabilitiesProvider @Inject constructor() {
    fun forMerchant(merchantId: String): CheckoutCapabilities = fixtures[merchantId]
        ?: CheckoutCapabilities(pickupAvailable = true, deliveryAvailable = true)

    private companion object {
        val fixtures = mapOf(
            "sabor-maianga" to CheckoutCapabilities(true, true),
            "doce-embondeiro" to CheckoutCapabilities(true, true),
            "cafe-horizonte" to CheckoutCapabilities(true, false),
            "mercado-talatona" to CheckoutCapabilities(true, true),
            "servicos-viana" to CheckoutCapabilities(false, false),
            "cantinho-kilamba" to CheckoutCapabilities(true, false),
            "fonte-fresca" to CheckoutCapabilities(false, true),
            "paes-mutamba" to CheckoutCapabilities(true, false)
        )
    }
}
