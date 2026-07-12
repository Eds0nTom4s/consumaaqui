package ao.consuma.aqui

import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Regra JUnit que prepara o estado de lançamento em memória antes do Hilt
 * injectar a dependência e antes da Activity ser criada.
 */
class LaunchStateRule(
    private val setup: InMemoryAppLaunchStateRepository.() -> Unit,
    private val assign: (AppLaunchStateRepository) -> Unit
) : TestWatcher() {

    override fun starting(description: Description?) {
        val repository = InMemoryAppLaunchStateRepository().apply(setup)
        assign(repository)
    }
}
