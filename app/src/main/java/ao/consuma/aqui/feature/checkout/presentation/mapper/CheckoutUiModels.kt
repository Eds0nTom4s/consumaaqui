package ao.consuma.aqui.feature.checkout.presentation.mapper

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod

@Immutable
sealed interface CheckoutUiText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : CheckoutUiText
    data class Plural(
        @PluralsRes val id: Int,
        val quantity: Int,
        val args: List<Any> = listOf(quantity)
    ) : CheckoutUiText
    data class Dynamic(val value: String) : CheckoutUiText
}

@Immutable
data class CheckoutStepUiModel(
    val step: CheckoutStep,
    val title: CheckoutUiText,
    val completed: Boolean,
    val current: Boolean
)

@Immutable
data class CheckoutCapabilitiesUiModel(
    val pickupAvailable: Boolean,
    val deliveryAvailable: Boolean
)

@Immutable
data class CheckoutItemSelectionUiModel(val groupName: String, val optionName: String)

@Immutable
data class CheckoutItemUiModel(
    val id: String,
    val productName: String,
    val quantity: Int,
    val selections: List<CheckoutItemSelectionUiModel>,
    val note: String?,
    val totalPrice: String
)

@Immutable
data class CheckoutCartSummaryUiModel(
    val merchantName: String,
    val items: List<CheckoutItemUiModel>,
    val itemCount: Int,
    val subtotal: String
)

@Immutable
data class CheckoutChargeUiModel(
    val label: CheckoutUiText,
    val amount: String
)

@Immutable
data class CheckoutQuoteUiModel(
    val subtotal: String,
    val charges: List<CheckoutChargeUiModel>,
    val total: String,
    val preparationMinutes: Int?,
    val deliveryMinutes: Int?,
    val expiresAtText: String?,
    val expired: Boolean
)

@Immutable
data class CheckoutCustomerReviewUiModel(
    val fullName: String,
    val phone: String,
    val email: String?
)

@Immutable
sealed interface CheckoutFulfillmentReviewUiModel {
    data class Pickup(val contactName: String, val contactPhone: String) :
        CheckoutFulfillmentReviewUiModel

    data class Delivery(
        val recipientName: String,
        val recipientPhone: String,
        val addressLines: List<String>,
        val instructions: String?
    ) : CheckoutFulfillmentReviewUiModel
}

@Immutable
data class CheckoutConfirmationUiModel(
    val clientReference: String,
    val merchantName: String,
    val total: String,
    val method: FulfillmentMethod
)
