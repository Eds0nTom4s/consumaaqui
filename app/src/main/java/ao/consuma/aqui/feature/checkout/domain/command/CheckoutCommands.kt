package ao.consuma.aqui.feature.checkout.domain.command

import ao.consuma.aqui.feature.checkout.domain.model.DeliveryAddress
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.model.PickupTimePreference

data class StartCheckoutCommand(
    val expectedCartId: String? = null,
    val expectedCartVersion: Long? = null,
    val scenario: MockCheckoutScenario = MockCheckoutScenario.SUCCESS
)

data class UpdateCustomerCommand(
    val sessionId: String,
    val fullName: String,
    val phone: String,
    val email: String?
)

data class SelectFulfillmentCommand(
    val sessionId: String,
    val method: FulfillmentMethod
)

data class UpdateDeliveryAddressCommand(
    val sessionId: String,
    val address: DeliveryAddress,
    val recipientName: String,
    val recipientPhone: String,
    val instructions: String?
)

data class UpdatePickupDetailsCommand(
    val sessionId: String,
    val contactName: String,
    val contactPhone: String,
    val desiredTime: PickupTimePreference = PickupTimePreference.AsSoonAsPossible
)

data class RequestQuoteCommand(
    val sessionId: String,
    val expectedCartId: String,
    val expectedCartVersion: Long
)

data class ConfirmCheckoutCommand(
    val sessionId: String,
    val expectedCartId: String,
    val expectedCartVersion: Long
)

enum class MockCheckoutScenario {
    SUCCESS,
    QUOTE_ERROR,
    QUOTE_EXPIRED,
    CART_CHANGED,
    FULFILLMENT_UNAVAILABLE,
    UNKNOWN_ERROR
}
