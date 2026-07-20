package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutCustomerValidatorTest {
    private val validator = CheckoutCustomerValidator()

    @Test fun `name is trimmed and supports accents hyphen apostrophe and compound forms`() {
        listOf("  Ána Maria  ", "Jean-Pierre", "D'Ávila").forEach { input ->
            val result = validator.validate(input, "923456789", null)
                as CustomerValidationResult.Valid
            assertEquals(input.trim(), result.customer.fullName)
        }
    }

    @Test fun `name boundaries accept 2 and 120 and reject empty 1 and 121`() {
        assertTrue(validator.validate("Ab", "923456789", null) is CustomerValidationResult.Valid)
        assertTrue(validator.validate("A".repeat(120), "923456789", null) is CustomerValidationResult.Valid)
        listOf("", " ", "A", "A".repeat(121)).forEach {
            val result = validator.validate(it, "923456789", null) as CustomerValidationResult.Invalid
            assertEquals(CheckoutError.InvalidCustomer, result.error)
        }
    }

    @Test fun `phone accepts 9 and 15 digits optional plus and removes spaces`() {
        val nine = validator.validate("Ana", "923 456 789", null) as CustomerValidationResult.Valid
        assertEquals("923456789", nine.customer.contact.phoneNumber)
        val fifteen = validator.validate("Ana", "+123 456 789 012 345", null)
            as CustomerValidationResult.Valid
        assertEquals("+123456789012345", fifteen.customer.contact.phoneNumber)
    }

    @Test fun `phone rejects lengths letters punctuation and misplaced plus`() {
        listOf("12345678", "1234567890123456", "923ABC789", "923-456-789", "92+3456789").forEach {
            val result = validator.validate("Ana", it, null) as CustomerValidationResult.Invalid
            assertEquals(CheckoutError.InvalidPhone, result.error)
        }
    }

    @Test fun `email null and blank normalize to null`() {
        assertNull((validator.validate("Ana", "923456789", null) as CustomerValidationResult.Valid)
            .customer.contact.email)
        assertNull((validator.validate("Ana", "923456789", "  ") as CustomerValidationResult.Valid)
            .customer.contact.email)
    }

    @Test fun `email is trimmed lowercased and validated basically`() {
        val valid = validator.validate("Ana", "923456789", " ANA@Example.COM ")
            as CustomerValidationResult.Valid
        assertEquals("ana@example.com", valid.customer.contact.email)
        listOf("ana", "@example.com", "ana@", "ana@example", "a b@example.com", "a@@example.com")
            .forEach {
                val result = validator.validate("Ana", "923456789", it)
                    as CustomerValidationResult.Invalid
                assertEquals(CheckoutError.InvalidEmail, result.error)
            }
    }

    @Test fun `email accepts 254 and rejects 255 characters`() {
        val domain = "@example.com"
        val email254 = "a".repeat(254 - domain.length) + domain
        val email255 = "a".repeat(255 - domain.length) + domain
        assertTrue(validator.validate("Ana", "923456789", email254) is CustomerValidationResult.Valid)
        assertEquals(
            CheckoutError.InvalidEmail,
            (validator.validate("Ana", "923456789", email255) as CustomerValidationResult.Invalid).error
        )
    }
}
