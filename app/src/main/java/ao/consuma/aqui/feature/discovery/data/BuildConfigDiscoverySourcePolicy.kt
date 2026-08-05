package ao.consuma.aqui.feature.discovery.data

import ao.consuma.aqui.BuildConfig
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySource
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySourcePolicy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class BuildConfigDiscoverySourcePolicy @Inject constructor() : DiscoverySourcePolicy {
    private val mutableSource = MutableStateFlow(
        runCatching { DiscoverySource.valueOf(BuildConfig.DISCOVERY_SOURCE) }
            .getOrDefault(DiscoverySource.REMOTE)
    )

    override val source: StateFlow<DiscoverySource> = mutableSource
    override val selectable: Boolean = BuildConfig.DISCOVERY_SOURCE_SELECTABLE

    override fun select(source: DiscoverySource): Boolean {
        if (!selectable) return false
        mutableSource.value = source
        return true
    }
}
