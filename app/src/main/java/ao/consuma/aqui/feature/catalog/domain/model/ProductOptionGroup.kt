package ao.consuma.aqui.feature.catalog.domain.model

data class ProductOptionGroup(
    val id: String,
    val name: String,
    val description: String?,
    val selectionRule: SelectionRule,
    val options: List<ProductOption>,
    val sortOrder: Int
) {
    init {
        require(id.isNotBlank()) { "Option group id is required" }
        require(name.isNotBlank()) { "Option group name is required" }
        require(sortOrder >= 0) { "Option group sort order cannot be negative" }
        require(options.distinctBy(ProductOption::id).size == options.size) {
            "Option ids must be unique inside a group"
        }
        require(selectionRule.maximumSelections <= options.size) {
            "Maximum selections cannot exceed the number of defined options"
        }
        val defaults = options.count(ProductOption::defaultSelected)
        require(defaults <= selectionRule.maximumSelections) {
            "Default selections exceed the group maximum"
        }
        require(defaults == 0 || defaults >= selectionRule.minimumSelections) {
            "Default selections do not satisfy the group minimum"
        }
    }

    val required: Boolean get() = selectionRule.required
    val minimumSelections: Int get() = selectionRule.minimumSelections
    val maximumSelections: Int get() = selectionRule.maximumSelections
}
