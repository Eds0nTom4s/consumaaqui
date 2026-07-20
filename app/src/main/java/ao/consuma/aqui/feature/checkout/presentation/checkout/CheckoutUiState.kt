package ao.consuma.aqui.feature.checkout.presentation.checkout

import androidx.compose.runtime.Immutable
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCapabilitiesUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCartSummaryUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCustomerReviewUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutFulfillmentReviewUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutQuoteUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutStepUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiText

@Immutable
data class CustomerFormUiState(
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val fullNameError: CheckoutUiText? = null,
    val phoneError: CheckoutUiText? = null,
    val emailError: CheckoutUiText? = null
)

@Immutable
data class PickupFormUiState(
    val contactName: String = "",
    val contactPhone: String = "",
    val contactNameError: CheckoutUiText? = null,
    val contactPhoneError: CheckoutUiText? = null
)

@Immutable
data class DeliveryFormUiState(
    val province: String = "",
    val municipality: String = "",
    val districtOrArea: String = "",
    val streetOrReference: String = "",
    val buildingOrHouse: String = "",
    val referencePoint: String = "",
    val recipientName: String = "",
    val recipientPhone: String = "",
    val instructions: String = "",
    val provinceError: CheckoutUiText? = null,
    val municipalityError: CheckoutUiText? = null,
    val streetError: CheckoutUiText? = null,
    val recipientNameError: CheckoutUiText? = null,
    val recipientPhoneError: CheckoutUiText? = null,
    val instructionsError: CheckoutUiText? = null
) {
    val instructionsCount: Int get() = instructions.length
}

sealed interface CheckoutPendingOperation {
    data object Starting : CheckoutPendingOperation
    data object SavingCustomer : CheckoutPendingOperation
    data object SavingFulfillment : CheckoutPendingOperation
    data object SavingDetails : CheckoutPendingOperation
    data object RequestingQuote : CheckoutPendingOperation
    data object Confirming : CheckoutPendingOperation
    data object Restarting : CheckoutPendingOperation
}

@Immutable
sealed interface CheckoutUiState {
    data object Initializing : CheckoutUiState

    data class Content(
        val sessionId: String,
        val currentStep: CheckoutStep,
        val availableSteps: List<CheckoutStepUiModel>,
        val selectedFulfillment: FulfillmentMethod?,
        val capabilities: CheckoutCapabilitiesUiModel,
        val customerForm: CustomerFormUiState,
        val pickupForm: PickupFormUiState,
        val deliveryForm: DeliveryFormUiState,
        val cartSummary: CheckoutCartSummaryUiModel,
        val quote: CheckoutQuoteUiModel?,
        val customerReview: CheckoutCustomerReviewUiModel?,
        val fulfillmentReview: CheckoutFulfillmentReviewUiModel?,
        val fulfillmentError: CheckoutUiText?,
        val pendingOperation: CheckoutPendingOperation?,
        val canGoBack: Boolean,
        val canContinue: Boolean,
        val canConfirm: Boolean
    ) : CheckoutUiState {
        val isMutating: Boolean get() = pendingOperation != null
    }

    data class CartConflict(
        val message: CheckoutUiText,
        val canRestart: Boolean
    ) : CheckoutUiState

    data class Error(
        val message: CheckoutUiText,
        val canRetry: Boolean
    ) : CheckoutUiState

    data object ConfirmedMock : CheckoutUiState
}
