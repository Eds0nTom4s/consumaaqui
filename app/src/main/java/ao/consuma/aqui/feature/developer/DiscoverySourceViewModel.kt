package ao.consuma.aqui.feature.developer

import androidx.lifecycle.ViewModel
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySource
import ao.consuma.aqui.feature.discovery.domain.capability.DiscoverySourcePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class DiscoverySourceViewModel @Inject constructor(
    private val policy: DiscoverySourcePolicy
) : ViewModel() {
    val source: StateFlow<DiscoverySource> = policy.source
    val selectable: Boolean = policy.selectable
    fun select(source: DiscoverySource) { policy.select(source) }
}
