package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.checkout.domain.model.DeliveryAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutAddressValidatorTest {
    private val validator = CheckoutAddressValidator()

    private fun address(
        province: String = " Luanda ",
        municipality: String = " Luanda ",
        street: String = " Rua 10 ",
        latitude: Double? = null,
        longitude: Double? = null
    ) = DeliveryAddress(
        province,
        municipality,
        " Maianga ",
        street,
        " Edifício azul ",
        " Próximo ao Banco BIC ",
        latitude,
        longitude
    )

    @Test fun `valid address trims fields and preserves angolan reference point`() {
        val result = validator.validate(address(), " Ligar ao chegar ") as AddressValidationResult.Valid
        assertEquals("Luanda", result.address.province)
        assertEquals("Luanda", result.address.municipality)
        assertEquals("Rua 10", result.address.streetOrReference)
        assertEquals("Maianga", result.address.districtOrArea)
        assertEquals("Edifício azul", result.address.buildingOrHouse)
        assertEquals("Próximo ao Banco BIC", result.address.referencePoint)
        assertEquals("Ligar ao chegar", result.instructions)
    }

    @Test fun `required address fields reject blanks`() {
        assertTrue(validator.validate(address(province = " "), null) is AddressValidationResult.Invalid)
        assertTrue(validator.validate(address(municipality = ""), null) is AddressValidationResult.Invalid)
        assertTrue(validator.validate(address(street = " "), null) is AddressValidationResult.Invalid)
    }

    @Test fun `optional blank address and instruction fields normalize to null`() {
        val value = address().copy(
            districtOrArea = " ", buildingOrHouse = "", referencePoint = "  "
        )
        val result = validator.validate(value, " ") as AddressValidationResult.Valid
        assertNull(result.address.districtOrArea)
        assertNull(result.address.buildingOrHouse)
        assertNull(result.address.referencePoint)
        assertNull(result.instructions)
    }

    @Test fun `coordinates must occur together and be finite within inclusive bounds`() {
        assertTrue(validator.validate(address(latitude = 1.0), null) is AddressValidationResult.Invalid)
        assertTrue(validator.validate(address(longitude = 1.0), null) is AddressValidationResult.Invalid)
        listOf(-90.0 to -180.0, 90.0 to 180.0, 0.0 to 0.0).forEach { (lat, lng) ->
            assertTrue(validator.validate(address(latitude = lat, longitude = lng), null)
                is AddressValidationResult.Valid)
        }
        listOf(-90.1 to 0.0, 90.1 to 0.0, 0.0 to -180.1, 0.0 to 180.1,
            Double.NaN to 0.0, 0.0 to Double.POSITIVE_INFINITY).forEach { (lat, lng) ->
            assertTrue(validator.validate(address(latitude = lat, longitude = lng), null)
                is AddressValidationResult.Invalid)
        }
    }

    @Test fun `instructions accept 300 and reject 301 characters`() {
        assertTrue(validator.validate(address(), "a".repeat(300)) is AddressValidationResult.Valid)
        assertTrue(validator.validate(address(), "a".repeat(301)) is AddressValidationResult.Invalid)
    }

    @Test fun `field maximums reject oversized values`() {
        assertTrue(validator.validate(address(province = "a".repeat(121)), null)
            is AddressValidationResult.Invalid)
        assertTrue(validator.validate(address(municipality = "a".repeat(121)), null)
            is AddressValidationResult.Invalid)
        assertTrue(validator.validate(address(street = "a".repeat(201)), null)
            is AddressValidationResult.Invalid)
    }
}
