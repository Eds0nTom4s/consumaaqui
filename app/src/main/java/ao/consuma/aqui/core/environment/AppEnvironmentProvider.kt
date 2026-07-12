package ao.consuma.aqui.core.environment

interface AppEnvironmentProvider {
    val currentEnvironment: String
    val versionName: String
}
