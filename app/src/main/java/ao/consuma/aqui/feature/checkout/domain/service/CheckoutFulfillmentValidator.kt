package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.checkout.domain.model.CheckoutCapabilities
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment
import ao.consuma.aqui.feature.checkout.domain.model.DeliveryAddress
import ao.consuma.aqui.feature.checkout.domain.model.DeliveryDetails
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.model.PickupDetails
import ao.consuma.aqui.feature.checkout.domain.model.PickupTimePreference
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import javax.inject.Inject

sealed interface FulfillmentValidationResult {
    data class Valid(val fulfillment: CheckoutFulfillment) : FulfillmentValidationResult
    data class Invalid(val error: CheckoutError) : FulfillmentValidationResult
}

class CheckoutFulfillmentValidator @Inject constructor(
    private val customerValidator: CheckoutCustomerValidator,
    private val addressValidator: CheckoutAddressValidator
) {
    fun isAvailable(method: FulfillmentMethod, capabilities: CheckoutCapabilities) =
        capabilities.supports(method)

    fun validatePickup(
        capabilities: CheckoutCapabilities,
        contactName: String,
        contactPhone: String,
        desiredTime: PickupTimePreference
    ): FulfillmentValidationResult {
        if (!capabilities.pickupAvailable) {
            return FulfillmentValidationResult.Invalid(CheckoutError.FulfillmentUnavailable)
        }
        val contact = customerValidator.validate(contactName, contactPhone, null)
        if (contact !is CustomerValidationResult.Valid) {
            return FulfillmentValidationResult.Invalid(CheckoutError.InvalidPickupDetails)
        }
        return FulfillmentValidationResult.Valid(
            CheckoutFulfillment.Pickup(
                PickupDetails(contact.customer.fullName, contact.customer.contact.phoneNumber, desiredTime)
            )
        )
    }

    fun validateDelivery(
        capabilities: CheckoutCapabilities,
        address: DeliveryAddress,
        recipientName: String,
        recipientPhone: String,
        instructions: String?
    ): FulfillmentValidationResult {
        if (!capabilities.deliveryAvailable) {
            return FulfillmentValidationResult.Invalid(CheckoutError.FulfillmentUnavailable)
        }
        val recipient = customerValidator.validate(recipientName, recipientPhone, null)
        if (recipient !is CustomerValidationResult.Valid) {
            return FulfillmentValidationResult.Invalid(CheckoutError.InvalidAddress)
        }
        val validatedAddress = addressValidator.validate(address, instructions)
        if (validatedAddress !is AddressValidationResult.Valid) {
            return FulfillmentValidationResult.Invalid(CheckoutError.InvalidAddress)
        }
        return FulfillmentValidationResult.Valid(
            CheckoutFulfillment.Delivery(
                DeliveryDetails(
                    validatedAddress.address,
                    validatedAddress.instructions,
                    recipient.customer.fullName,
                    recipient.customer.contact.phoneNumber
                )
            )
        )
    }
}
