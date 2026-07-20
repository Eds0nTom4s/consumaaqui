package ao.consuma.aqui.feature.checkout.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutCapabilitiesProviderTest {
    private val provider = CheckoutCapabilitiesProvider()

    @Test fun `existing merchant capabilities are controlled checkout fixtures`() {
        assertTrue(provider.forMerchant("sabor-maianga").pickupAvailable)
        assertTrue(provider.forMerchant("sabor-maianga").deliveryAvailable)
        assertTrue(provider.forMerchant("paes-mutamba").pickupAvailable)
        assertFalse(provider.forMerchant("paes-mutamba").deliveryAvailable)
        assertFalse(provider.forMerchant("fonte-fresca").pickupAvailable)
        assertTrue(provider.forMerchant("fonte-fresca").deliveryAvailable)
        assertFalse(provider.forMerchant("servicos-viana").pickupAvailable)
        assertFalse(provider.forMerchant("servicos-viana").deliveryAvailable)
    }
}
