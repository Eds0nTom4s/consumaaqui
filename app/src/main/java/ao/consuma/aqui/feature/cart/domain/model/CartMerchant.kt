package ao.consuma.aqui.feature.cart.domain.model

data class CartMerchant(
    val id: String,
    val name: String
) {
    init {
        require(id.isNotBlank()) { "Merchant id is required" }
        require(name.isNotBlank()) { "Merchant name is required" }
    }
}
