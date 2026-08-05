package ao.consuma.aqui.feature.discovery.remote

object DiscoveryPagingMapper {
    fun toBackend(androidPage: Int): Int {
        require(androidPage >= 1) { "Android page must be at least 1." }
        return androidPage - 1
    }

    fun toAndroid(backendPage: Int): Int {
        require(backendPage >= 0) { "Backend page must not be negative." }
        return backendPage + 1
    }
}
