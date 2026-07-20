package ao.consuma.aqui.feature.checkout.domain.model

import ao.consuma.aqui.core.domain.model.MoneyAmount
import java.time.Instant

enum class CheckoutChargeType { DELIVERY_ESTIMATE, SERVICE_ESTIMATE }

data class CheckoutCharge(
    val type: CheckoutChargeType,
    val amount: MoneyAmount
)

data class CheckoutTotals(
    val subtotal: MoneyAmount,
    val charges: List<CheckoutCharge>,
    val total: MoneyAmount
) {
    init {
        require(charges.all { it.amount.currencyCode == subtotal.currencyCode }) {
            "Charge currency must match subtotal"
        }
        val expected = charges.fold(subtotal) { value, charge -> value.add(charge.amount) }
        require(total == expected) { "Checkout total must equal subtotal plus charges" }
    }
}

sealed interface CheckoutQuoteSource {
    data object MockFixedPolicy : CheckoutQuoteSource
}

data class CheckoutQuote(
    val id: String,
    val sessionId: String,
    val cartVersion: Long,
    val subtotal: MoneyAmount,
    val charges: List<CheckoutCharge>,
    val total: MoneyAmount,
    val currencyCode: String,
    val estimatedPreparationMinutes: Int?,
    val estimatedDeliveryMinutes: Int?,
    val expiresAt: Instant?,
    val source: CheckoutQuoteSource
) {
    init {
        require(id.isNotBlank() && sessionId.isNotBlank()) { "Quote identity is required" }
        require(cartVersion >= 0) { "Cart version cannot be negative" }
        require(currencyCode == subtotal.currencyCode && total.currencyCode == currencyCode) {
            "Quote currency must be consistent"
        }
        require(estimatedPreparationMinutes == null || estimatedPreparationMinutes >= 0)
        require(estimatedDeliveryMinutes == null || estimatedDeliveryMinutes >= 0)
        CheckoutTotals(subtotal, charges, total)
    }

    val totals: CheckoutTotals get() = CheckoutTotals(subtotal, charges, total)
}
