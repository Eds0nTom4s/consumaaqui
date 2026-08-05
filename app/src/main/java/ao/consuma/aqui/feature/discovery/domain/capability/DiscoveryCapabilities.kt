package ao.consuma.aqui.feature.discovery.domain.capability

import ao.consuma.aqui.feature.discovery.domain.model.DiscoveryOrderBy
import kotlinx.coroutines.flow.StateFlow

enum class DiscoverySource { MOCK, REMOTE }

data class DiscoveryCapabilities(
    val supportedSorts: Set<DiscoveryOrderBy>,
    val supportsOnlyOpen: Boolean,
    val supportsFulfillmentFilter: Boolean
) {
    companion object {
        val Mock = DiscoveryCapabilities(DiscoveryOrderBy.entries.toSet(), true, true)
        val Remote = DiscoveryCapabilities(setOf(DiscoveryOrderBy.NAME), false, false)
    }
}

interface DiscoverySourcePolicy {
    val source: StateFlow<DiscoverySource>
    val selectable: Boolean
    val capabilities: DiscoveryCapabilities
        get() = if (source.value == DiscoverySource.REMOTE) DiscoveryCapabilities.Remote else DiscoveryCapabilities.Mock

    fun select(source: DiscoverySource): Boolean
}

object MockDiscoverySourcePolicy : DiscoverySourcePolicy {
    private val selected = kotlinx.coroutines.flow.MutableStateFlow(DiscoverySource.MOCK)
    override val source: StateFlow<DiscoverySource> = selected
    override val selectable: Boolean = true
    override fun select(source: DiscoverySource): Boolean {
        selected.value = source
        return true
    }
}
