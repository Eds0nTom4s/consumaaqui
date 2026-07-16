package ao.consuma.aqui.feature.catalog.domain.model

data class ProductConfiguration(
    val productId: String,
    val selections: Map<String, Set<String>>,
    val quantity: Int,
    val note: String
) {
    init {
        require(productId.isNotBlank()) { "Product id is required" }
        require(quantity in MIN_QUANTITY..MAX_QUANTITY) {
            "Quantity must be between $MIN_QUANTITY and $MAX_QUANTITY"
        }
        require(note.length <= MAX_NOTE_LENGTH) {
            "Note cannot exceed $MAX_NOTE_LENGTH characters"
        }
        require(selections.keys.none { it.isBlank() }) { "Option group ids cannot be blank" }
        require(selections.values.flatten().none { it.isBlank() }) { "Option ids cannot be blank" }
    }

    companion object {
        const val MIN_QUANTITY = 1
        const val MAX_QUANTITY = 99
        const val MAX_NOTE_LENGTH = 250
    }
}
