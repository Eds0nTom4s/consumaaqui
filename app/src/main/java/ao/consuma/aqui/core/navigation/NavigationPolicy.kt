package ao.consuma.aqui.core.navigation

object NavigationPolicy {

    fun destinationAfterInitialization(environment: String): AppDestination =
        AppDestination.AppShell

    fun isDesignSystemCatalogEnabled(environment: String): Boolean =
        environment != Environment.RELEASE

    fun isDevelopmentToolsVisible(environment: String): Boolean =
        environment != Environment.RELEASE

    object Environment {
        const val DEBUG = "DEBUG"
        const val STAGING = "STAGING"
        const val RELEASE = "RELEASE"
    }
}
