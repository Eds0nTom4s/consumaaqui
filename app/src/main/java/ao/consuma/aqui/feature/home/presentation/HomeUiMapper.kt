package ao.consuma.aqui.feature.home.presentation

import ao.consuma.aqui.feature.discovery.domain.model.HomeDiscoveryContent
import ao.consuma.aqui.feature.discovery.presentation.mapper.DiscoveryUiMapper
import javax.inject.Inject

/** Compatibility facade while Home presentation remains in its established package. */
class HomeUiMapper @Inject constructor() : DiscoveryUiMapper() {
    fun categories(content: HomeDiscoveryContent) = categories(content.categories)
    fun merchants(content: HomeDiscoveryContent, hasLocation: Boolean) = home(content, hasLocation)
}
