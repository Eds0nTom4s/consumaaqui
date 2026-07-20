package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.checkout.domain.model.DeliveryAddress
import javax.inject.Inject

sealed interface AddressValidationResult {
    data class Valid(val address: DeliveryAddress, val instructions: String?) : AddressValidationResult
    data object Invalid : AddressValidationResult
}

class CheckoutAddressValidator @Inject constructor() {
    fun validate(address: DeliveryAddress, instructions: String?): AddressValidationResult {
        val province = address.province.trim()
        val municipality = address.municipality.trim()
        val street = address.streetOrReference.trim()
        val district = address.districtOrArea.normalizedOptional()
        val building = address.buildingOrHouse.normalizedOptional()
        val reference = address.referencePoint.normalizedOptional()
        val normalizedInstructions = instructions.normalizedOptional()
        if (province.isEmpty() || province.length > 120 ||
            municipality.isEmpty() || municipality.length > 120 ||
            street.isEmpty() || street.length > 200 ||
            district.isTooLong(160) || building.isTooLong(160) || reference.isTooLong(200) ||
            normalizedInstructions.isTooLong(300)
        ) return AddressValidationResult.Invalid
        if ((address.latitude == null) != (address.longitude == null)) return AddressValidationResult.Invalid
        if (address.latitude != null && (!address.latitude.isFinite() || address.latitude !in -90.0..90.0)) {
            return AddressValidationResult.Invalid
        }
        if (address.longitude != null && (!address.longitude.isFinite() || address.longitude !in -180.0..180.0)) {
            return AddressValidationResult.Invalid
        }
        return AddressValidationResult.Valid(
            DeliveryAddress(
                province,
                municipality,
                district,
                street,
                building,
                reference,
                address.latitude,
                address.longitude
            ),
            normalizedInstructions
        )
    }

    private fun String?.normalizedOptional() = this?.trim()?.takeIf(String::isNotEmpty)
    private fun String?.isTooLong(max: Int) = this != null && length > max
}
