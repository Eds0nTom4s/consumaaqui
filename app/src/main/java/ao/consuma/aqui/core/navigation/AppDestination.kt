package ao.consuma.aqui.core.navigation

sealed class AppDestination(val route: String) {
    data object Splash : AppDestination("splash")
    data object Institutional : AppDestination("institutional")
    data object DesignSystemCatalog : AppDestination("design_system_catalog")
}

object NavigationPolicy {
    fun destinationAfterSplash(environment: String): AppDestination =
        if (environment == "RELEASE") {
            AppDestination.Institutional
        } else {
            AppDestination.DesignSystemCatalog
        }

    fun isDesignSystemCatalogEnabled(environment: String): Boolean = environment != "RELEASE"
}
