package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.checkout.domain.model.CheckoutCapabilities
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment
import ao.consuma.aqui.feature.checkout.domain.model.DeliveryAddress
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.model.PickupTimePreference
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutFulfillmentValidatorTest {
    private val validator = CheckoutFulfillmentValidator(
        CheckoutCustomerValidator(), CheckoutAddressValidator()
    )
    private val both = CheckoutCapabilities(true, true)
    private val address = DeliveryAddress(
        "Luanda", "Luanda", "Maianga", "Rua 10", null,
        "Próximo ao Banco BIC", null, null
    )

    @Test fun `capabilities expose pickup and delivery independently`() {
        assertTrue(validator.isAvailable(FulfillmentMethod.PICKUP, both))
        assertTrue(validator.isAvailable(FulfillmentMethod.DELIVERY_MOCK, both))
        assertEquals(false, validator.isAvailable(
            FulfillmentMethod.PICKUP, CheckoutCapabilities(false, true)
        ))
        assertEquals(false, validator.isAvailable(
            FulfillmentMethod.DELIVERY_MOCK, CheckoutCapabilities(true, false)
        ))
    }

    @Test fun `pickup asap normalizes contact`() {
        val result = validator.validatePickup(
            both, " Ana Silva ", "+244 923 456 789", PickupTimePreference.AsSoonAsPossible
        ) as FulfillmentValidationResult.Valid
        val pickup = result.fulfillment as CheckoutFulfillment.Pickup
        assertEquals("Ana Silva", pickup.details.contactName)
        assertEquals("+244923456789", pickup.details.contactPhone)
        assertEquals(PickupTimePreference.AsSoonAsPossible, pickup.details.desiredTime)
    }

    @Test fun `pickup unavailable and invalid details are explicit`() {
        assertEquals(
            CheckoutError.FulfillmentUnavailable,
            (validator.validatePickup(
                CheckoutCapabilities(false, true), "Ana", "923456789",
                PickupTimePreference.AsSoonAsPossible
            ) as FulfillmentValidationResult.Invalid).error
        )
        assertEquals(
            CheckoutError.InvalidPickupDetails,
            (validator.validatePickup(
                both, "A", "invalid", PickupTimePreference.AsSoonAsPossible
            ) as FulfillmentValidationResult.Invalid).error
        )
    }

    @Test fun `delivery validates address recipient phone and instructions`() {
        val result = validator.validateDelivery(
            both, address, " Ana Silva ", "+244 923 456 789", " Portão azul "
        ) as FulfillmentValidationResult.Valid
        val delivery = result.fulfillment as CheckoutFulfillment.Delivery
        assertEquals("Ana Silva", delivery.details.recipientName)
        assertEquals("+244923456789", delivery.details.recipientPhone)
        assertEquals("Portão azul", delivery.details.instructions)
        assertEquals("Próximo ao Banco BIC", delivery.details.address.referencePoint)
    }

    @Test fun `delivery unavailable bad recipient and bad address are explicit`() {
        assertEquals(
            CheckoutError.FulfillmentUnavailable,
            (validator.validateDelivery(
                CheckoutCapabilities(true, false), address, "Ana", "923456789", null
            ) as FulfillmentValidationResult.Invalid).error
        )
        assertEquals(
            CheckoutError.InvalidAddress,
            (validator.validateDelivery(
                both, address, "A", "923456789", null
            ) as FulfillmentValidationResult.Invalid).error
        )
        assertEquals(
            CheckoutError.InvalidAddress,
            (validator.validateDelivery(
                both, address.copy(province = ""), "Ana", "923456789", null
            ) as FulfillmentValidationResult.Invalid).error
        )
    }
}
