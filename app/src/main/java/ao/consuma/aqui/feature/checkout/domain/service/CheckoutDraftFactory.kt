package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.checkout.domain.model.CheckoutDraft
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutResult
import java.time.Clock
import java.util.Collections
import javax.inject.Inject

class CheckoutDraftFactory @Inject constructor(
    private val clock: Clock,
    private val idGenerator: CheckoutIdGenerator,
    private val customerValidator: CheckoutCustomerValidator
) {
    fun create(
        session: CheckoutSession,
        expectedCartId: String,
        expectedCartVersion: Long
    ): CheckoutResult<CheckoutDraft> {
        if (session.status != CheckoutSessionStatus.ReadyForReview) {
            return CheckoutResult.Failure(CheckoutError.InvalidState)
        }
        if (session.cartId != expectedCartId || session.cartVersion != expectedCartVersion) {
            return CheckoutResult.Failure(CheckoutError.InvalidState)
        }
        val customer = session.customer ?: return CheckoutResult.Failure(CheckoutError.InvalidCustomer)
        if (customerValidator.validate(
                customer.fullName,
                customer.contact.phoneNumber,
                customer.contact.email
            ) !is CustomerValidationResult.Valid
        ) return CheckoutResult.Failure(CheckoutError.InvalidCustomer)
        val fulfillment = session.fulfillment
            ?: return CheckoutResult.Failure(CheckoutError.InvalidState)
        val quote = session.quote ?: return CheckoutResult.Failure(CheckoutError.QuoteUnavailable)
        if (quote.sessionId != session.id || quote.cartVersion != session.cartVersion ||
            quote.subtotal != session.cartSubtotal || quote.currencyCode != session.currencyCode
        ) return CheckoutResult.Failure(CheckoutError.QuoteUnavailable)
        if (quote.expiresAt?.isAfter(clock.instant()) == false) {
            return CheckoutResult.Failure(CheckoutError.QuoteExpired)
        }
        return CheckoutResult.Success(
            CheckoutDraft(
                idGenerator.createId(),
                session.id,
                session.cartId,
                session.cartVersion,
                session.merchantId,
                session.merchantName,
                Collections.unmodifiableList(session.items.toList()),
                customer,
                fulfillment,
                quote,
                idGenerator.createId(),
                clock.instant()
            )
        )
    }
}
