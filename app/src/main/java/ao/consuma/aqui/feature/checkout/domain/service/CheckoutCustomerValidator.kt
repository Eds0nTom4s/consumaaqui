package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.checkout.domain.model.CheckoutContact
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutCustomer
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import javax.inject.Inject

sealed interface CustomerValidationResult {
    data class Valid(val customer: CheckoutCustomer) : CustomerValidationResult
    data class Invalid(val error: CheckoutError) : CustomerValidationResult
}

class CheckoutCustomerValidator @Inject constructor() {
    fun validate(fullName: String, phone: String, email: String?): CustomerValidationResult {
        val normalizedName = fullName.trim()
        if (normalizedName.length !in 2..120) {
            return CustomerValidationResult.Invalid(CheckoutError.InvalidCustomer)
        }
        val normalizedPhone = phone.filterNot(Char::isWhitespace)
        val digitCount = normalizedPhone.removePrefix("+").length
        if (digitCount !in 9..15 || !PHONE.matches(normalizedPhone)) {
            return CustomerValidationResult.Invalid(CheckoutError.InvalidPhone)
        }
        // Locale-independent lowercase is an explicit normalization decision for this mock phase.
        val normalizedEmail = email?.trim()?.takeIf(String::isNotEmpty)?.lowercase()
        if (normalizedEmail != null && !isBasicEmailValid(normalizedEmail)) {
            return CustomerValidationResult.Invalid(CheckoutError.InvalidEmail)
        }
        return CustomerValidationResult.Valid(
            CheckoutCustomer(normalizedName, CheckoutContact(normalizedPhone, normalizedEmail))
        )
    }

    private fun isBasicEmailValid(value: String): Boolean {
        if (value.length > 254 || value.any(Char::isWhitespace)) return false
        val at = value.indexOf('@')
        return at > 0 && at == value.lastIndexOf('@') && at < value.lastIndex &&
            value.substring(at + 1).contains('.') && !value.endsWith('.')
    }

    private companion object {
        val PHONE = Regex("\\+?[0-9]+")
    }
}
