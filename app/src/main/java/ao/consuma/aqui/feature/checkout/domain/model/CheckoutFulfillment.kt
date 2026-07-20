package ao.consuma.aqui.feature.checkout.domain.model

import java.time.LocalDateTime

enum class FulfillmentMethod { PICKUP, DELIVERY_MOCK }

data class CheckoutCapabilities(
    val pickupAvailable: Boolean,
    val deliveryAvailable: Boolean
) {
    fun supports(method: FulfillmentMethod): Boolean = when (method) {
        FulfillmentMethod.PICKUP -> pickupAvailable
        FulfillmentMethod.DELIVERY_MOCK -> deliveryAvailable
    }
}

sealed interface PickupTimePreference {
    data object AsSoonAsPossible : PickupTimePreference
    data class Scheduled(val localDateTime: LocalDateTime) : PickupTimePreference
}

data class PickupDetails(
    val contactName: String,
    val contactPhone: String,
    val desiredTime: PickupTimePreference
)

/**
 * Mock delivery only prepares transient data. It does not contact or start any logistics
 * provider, courier, tracking operation or real delivery.
 */
data class DeliveryDetails(
    val address: DeliveryAddress,
    val instructions: String?,
    val recipientName: String,
    val recipientPhone: String
)

data class DeliveryAddress(
    val province: String,
    val municipality: String,
    val districtOrArea: String?,
    val streetOrReference: String,
    val buildingOrHouse: String?,
    val referencePoint: String?,
    val latitude: Double?,
    val longitude: Double?
)

sealed interface CheckoutFulfillment {
    val method: FulfillmentMethod

    data class Pickup(val details: PickupDetails) : CheckoutFulfillment {
        override val method = FulfillmentMethod.PICKUP
    }

    data class Delivery(val details: DeliveryDetails) : CheckoutFulfillment {
        override val method = FulfillmentMethod.DELIVERY_MOCK
    }
}
