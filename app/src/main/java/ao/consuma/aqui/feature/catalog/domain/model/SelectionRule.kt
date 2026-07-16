package ao.consuma.aqui.feature.catalog.domain.model

data class SelectionRule(
    val minimumSelections: Int,
    val maximumSelections: Int
) {
    init {
        require(minimumSelections >= 0) { "Minimum selections cannot be negative" }
        require(maximumSelections >= 1) { "Maximum selections must be positive" }
        require(minimumSelections <= maximumSelections) {
            "Minimum selections cannot exceed maximum selections"
        }
    }

    val required: Boolean get() = minimumSelections > 0
    val singleChoice: Boolean get() = maximumSelections == 1
}
