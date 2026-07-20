package ao.consuma.aqui.feature.checkout.presentation.checkout

import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiText

sealed interface CheckoutUiEvent {
    data object Initialize : CheckoutUiEvent
    data class FulfillmentSelected(val method: FulfillmentMethod) : CheckoutUiEvent
    data class CustomerNameChanged(val value: String) : CheckoutUiEvent
    data class CustomerPhoneChanged(val value: String) : CheckoutUiEvent
    data class CustomerEmailChanged(val value: String) : CheckoutUiEvent
    data class PickupNameChanged(val value: String) : CheckoutUiEvent
    data class PickupPhoneChanged(val value: String) : CheckoutUiEvent
    data class DeliveryProvinceChanged(val value: String) : CheckoutUiEvent
    data class DeliveryMunicipalityChanged(val value: String) : CheckoutUiEvent
    data class DeliveryAreaChanged(val value: String) : CheckoutUiEvent
    data class DeliveryStreetChanged(val value: String) : CheckoutUiEvent
    data class DeliveryBuildingChanged(val value: String) : CheckoutUiEvent
    data class DeliveryReferenceChanged(val value: String) : CheckoutUiEvent
    data class DeliveryRecipientNameChanged(val value: String) : CheckoutUiEvent
    data class DeliveryRecipientPhoneChanged(val value: String) : CheckoutUiEvent
    data class DeliveryInstructionsChanged(val value: String) : CheckoutUiEvent
    data object Continue : CheckoutUiEvent
    data object BackStep : CheckoutUiEvent
    data object RequestQuote : CheckoutUiEvent
    data object Confirm : CheckoutUiEvent
    data object RestartCheckout : CheckoutUiEvent
    data object ReviewCart : CheckoutUiEvent
    data object RefreshQuote : CheckoutUiEvent
    data class EditStep(val step: CheckoutStep) : CheckoutUiEvent
}

sealed interface CheckoutUiEffect {
    data object NavigateToCart : CheckoutUiEffect
    data object NavigateToConfirmation : CheckoutUiEffect
    data class Message(val text: CheckoutUiText) : CheckoutUiEffect
}
