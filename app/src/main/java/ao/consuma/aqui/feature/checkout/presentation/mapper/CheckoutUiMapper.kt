package ao.consuma.aqui.feature.checkout.presentation.mapper

import ao.consuma.aqui.R
import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutChargeType
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutDraft
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import java.time.Clock
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CheckoutUiMapper @Inject constructor(
    private val clock: Clock
) {
    fun steps(current: CheckoutStep): List<CheckoutStepUiModel> = visibleSteps.map { step ->
        CheckoutStepUiModel(
            step = step,
            title = CheckoutUiText.Resource(stepTitle(step)),
            completed = visibleSteps.indexOf(step) < visibleSteps.indexOf(current),
            current = step == current
        )
    }

    fun capabilities(session: CheckoutSession) = CheckoutCapabilitiesUiModel(
        session.capabilities.pickupAvailable,
        session.capabilities.deliveryAvailable
    )

    fun cart(session: CheckoutSession) = CheckoutCartSummaryUiModel(
        merchantName = session.merchantName,
        items = session.items.map { item ->
            CheckoutItemUiModel(
                id = item.cartItemId,
                productName = item.productName,
                quantity = item.quantity,
                selections = item.selections.map {
                    CheckoutItemSelectionUiModel(it.groupName, it.optionName)
                },
                note = item.note,
                totalPrice = money(item.totalPrice)
            )
        },
        itemCount = session.cartItemCount,
        subtotal = money(session.cartSubtotal)
    )

    fun quote(session: CheckoutSession, forceExpired: Boolean = false): CheckoutQuoteUiModel? =
        session.quote?.let { value ->
            val expiresAt = value.expiresAt
            CheckoutQuoteUiModel(
                subtotal = money(value.subtotal),
                charges = value.charges.filter { it.amount.amountMinor > 0 }.map { charge ->
                    CheckoutChargeUiModel(
                        label = CheckoutUiText.Resource(when (charge.type) {
                            CheckoutChargeType.DELIVERY_ESTIMATE -> R.string.checkout_delivery_charge
                            CheckoutChargeType.SERVICE_ESTIMATE -> R.string.checkout_service_charge
                        }),
                        amount = money(charge.amount)
                    )
                },
                total = money(value.total),
                preparationMinutes = value.estimatedPreparationMinutes,
                deliveryMinutes = value.estimatedDeliveryMinutes,
                expiresAtText = expiresAt?.atZone(clock.zone)?.format(timeFormatter),
                expired = forceExpired || expiresAt?.isAfter(clock.instant()) == false
            )
        }

    fun customer(session: CheckoutSession): CheckoutCustomerReviewUiModel? = session.customer?.let {
        CheckoutCustomerReviewUiModel(it.fullName, it.contact.phoneNumber, it.contact.email)
    }

    fun fulfillment(session: CheckoutSession): CheckoutFulfillmentReviewUiModel? =
        when (val value = session.fulfillment) {
            null -> null
            is CheckoutFulfillment.Pickup -> CheckoutFulfillmentReviewUiModel.Pickup(
                value.details.contactName,
                value.details.contactPhone
            )
            is CheckoutFulfillment.Delivery -> CheckoutFulfillmentReviewUiModel.Delivery(
                recipientName = value.details.recipientName,
                recipientPhone = value.details.recipientPhone,
                addressLines = listOfNotNull(
                    value.details.address.province,
                    value.details.address.municipality,
                    value.details.address.districtOrArea,
                    value.details.address.streetOrReference,
                    value.details.address.buildingOrHouse,
                    value.details.address.referencePoint
                ).filter(String::isNotBlank),
                instructions = value.details.instructions
            )
        }

    fun confirmation(draft: CheckoutDraft) = CheckoutConfirmationUiModel(
        clientReference = draft.clientReference.take(8).uppercase(),
        merchantName = draft.merchantName,
        total = money(draft.quote.total),
        method = draft.fulfillment.method
    )

    fun error(value: CheckoutError): CheckoutUiText.Resource = CheckoutUiText.Resource(
        when (value) {
            CheckoutError.EmptyCart -> R.string.checkout_error_empty_cart
            CheckoutError.InvalidCart -> R.string.checkout_error_invalid_cart
            CheckoutError.CartChanged -> R.string.checkout_cart_changed
            CheckoutError.InvalidCustomer -> R.string.checkout_error_invalid_customer
            CheckoutError.InvalidPhone -> R.string.checkout_error_invalid_phone
            CheckoutError.InvalidEmail -> R.string.checkout_error_invalid_email
            CheckoutError.FulfillmentUnavailable -> R.string.checkout_error_fulfillment_unavailable
            CheckoutError.InvalidAddress -> R.string.checkout_error_invalid_address
            CheckoutError.InvalidPickupDetails -> R.string.checkout_error_invalid_pickup
            CheckoutError.QuoteUnavailable -> R.string.checkout_error_quote
            CheckoutError.QuoteExpired -> R.string.checkout_quote_expired
            CheckoutError.CurrencyMismatch -> R.string.checkout_error_currency
            CheckoutError.PriceOverflow -> R.string.checkout_error_price
            CheckoutError.SessionNotFound -> R.string.checkout_error_session
            CheckoutError.SessionAlreadyConfirmed -> R.string.checkout_error_already_confirmed
            CheckoutError.InvalidState -> R.string.checkout_error_invalid_state
            CheckoutError.Unknown -> R.string.checkout_error_generic
        }
    )

    fun money(value: MoneyAmount): String {
        val whole = value.amountMinor / 100
        val fraction = value.amountMinor % 100
        val grouped = whole.toString().reversed().chunked(3).joinToString(".").reversed()
        val amount = if (fraction == 0L) grouped else {
            "$grouped,${fraction.toString().padStart(2, '0')}"
        }
        return "$amount ${if (value.currencyCode == "AOA") "Kz" else value.currencyCode}"
    }

    private fun stepTitle(step: CheckoutStep): Int = when (step) {
        CheckoutStep.FULFILLMENT -> R.string.checkout_step_fulfillment
        CheckoutStep.CUSTOMER -> R.string.checkout_step_customer
        CheckoutStep.DETAILS -> R.string.checkout_step_details
        CheckoutStep.REVIEW -> R.string.checkout_step_review
        CheckoutStep.CONFIRMATION -> R.string.checkout_confirmation_title
    }

    companion object {
        val visibleSteps = listOf(
            CheckoutStep.FULFILLMENT,
            CheckoutStep.CUSTOMER,
            CheckoutStep.DETAILS,
            CheckoutStep.REVIEW
        )
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
