package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutCharge
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutChargeType
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutQuote
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutQuoteSource
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

sealed interface QuoteCalculationResult {
    data class Success(val quote: CheckoutQuote) : QuoteCalculationResult
    data class Failure(val error: CheckoutError) : QuoteCalculationResult
}

/** Deterministic demonstration policy; it performs no distance or logistics calculation. */
class CheckoutQuoteCalculator @Inject constructor(
    private val clock: Clock,
    private val idGenerator: CheckoutIdGenerator
) {
    fun calculate(session: CheckoutSession, expiredFixture: Boolean = false): QuoteCalculationResult {
        val fulfillment = session.fulfillment
            ?: return QuoteCalculationResult.Failure(CheckoutError.InvalidState)
        if (session.cartSubtotal.currencyCode != session.currencyCode) {
            return QuoteCalculationResult.Failure(CheckoutError.CurrencyMismatch)
        }
        val charges = when (fulfillment) {
            is CheckoutFulfillment.Pickup -> emptyList()
            is CheckoutFulfillment.Delivery -> listOf(
                CheckoutCharge(
                    CheckoutChargeType.DELIVERY_ESTIMATE,
                    MoneyAmount(deliveryFee(fulfillment.details.address.municipality), session.currencyCode)
                )
            )
        }
        return try {
            val total = charges.fold(session.cartSubtotal) { value, charge -> value.add(charge.amount) }
            val now = clock.instant()
            QuoteCalculationResult.Success(
                CheckoutQuote(
                    id = idGenerator.createId(),
                    sessionId = session.id,
                    cartVersion = session.cartVersion,
                    subtotal = session.cartSubtotal,
                    charges = charges.toList(),
                    total = total,
                    currencyCode = session.currencyCode,
                    estimatedPreparationMinutes = when (fulfillment) {
                        is CheckoutFulfillment.Pickup -> 20
                        is CheckoutFulfillment.Delivery -> 25
                    },
                    estimatedDeliveryMinutes = when (fulfillment) {
                        is CheckoutFulfillment.Pickup -> null
                        is CheckoutFulfillment.Delivery -> 35
                    },
                    expiresAt = if (expiredFixture) now.minusSeconds(1) else now.plus(QUOTE_VALIDITY),
                    source = CheckoutQuoteSource.MockFixedPolicy
                )
            )
        } catch (_: ArithmeticException) {
            QuoteCalculationResult.Failure(CheckoutError.PriceOverflow)
        } catch (_: IllegalArgumentException) {
            QuoteCalculationResult.Failure(CheckoutError.CurrencyMismatch)
        }
    }

    private fun deliveryFee(municipality: String): Long =
        if (municipality.trim().equals("Luanda", ignoreCase = true)) 150_000L else 200_000L

    companion object {
        val QUOTE_VALIDITY: Duration = Duration.ofMinutes(10)
    }
}
