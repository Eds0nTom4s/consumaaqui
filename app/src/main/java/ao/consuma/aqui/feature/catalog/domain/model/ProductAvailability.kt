package ao.consuma.aqui.feature.catalog.domain.model

import java.time.LocalTime

sealed interface ProductAvailability {
    data object Available : ProductAvailability
    data object Unavailable : ProductAvailability
    data object TemporarilyUnavailable : ProductAvailability
    data class AvailableFrom(val time: LocalTime) : ProductAvailability
    data object OutOfStock : ProductAvailability
    data object Unknown : ProductAvailability
}
