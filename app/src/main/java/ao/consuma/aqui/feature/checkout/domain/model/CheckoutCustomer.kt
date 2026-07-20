package ao.consuma.aqui.feature.checkout.domain.model

data class CheckoutCustomer(
    val fullName: String,
    val contact: CheckoutContact
) {
    init {
        require(fullName == fullName.trim() && fullName.length in 2..120) {
            "Customer name must be normalized"
        }
    }
}

data class CheckoutContact(
    val phoneNumber: String,
    val email: String?
) {
    init {
        require(phoneNumber.isNotBlank()) { "Phone number is required" }
        require(email == null || email == email.trim().lowercase()) { "Email must be normalized" }
    }
}
