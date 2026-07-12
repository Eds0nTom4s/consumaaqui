package ao.consuma.aqui.core.navigation

sealed class AppDestination(val route: String) {
    data object Splash : AppDestination("splash")
    data object DesignSystemCatalog : AppDestination("design_system_catalog")
}
