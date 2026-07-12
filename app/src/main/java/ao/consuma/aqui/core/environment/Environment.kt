package ao.consuma.aqui.core.environment

import ao.consuma.aqui.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvironmentResolver @Inject constructor() {
    val currentEnvironment: String
        get() = BuildConfig.ENVIRONMENT
        
    val versionName: String
        get() = BuildConfig.VERSION_NAME
}
