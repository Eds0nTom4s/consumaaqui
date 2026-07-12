package ao.consuma.aqui.feature.bootstrap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingIndicator
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed interface InitializationResult {
    data object Onboarding : InitializationResult
    data object AppShell : InitializationResult
    data object Failure : InitializationResult
}

interface InitializationGatewayViewModel {
    val result: StateFlow<InitializationResult>
}

/**
 * ViewModel responsável pela decisão inicial de navegação.
 *
 * Nesta fase, a decisão é determinística e baseada apenas no estado de
 * lançamento em memória. Futuramente poderá incluir onboarding versionado,
 * localização, manutenção, actualização obrigatória e estado de sessão.
 */
@HiltViewModel
class ConsumaInitializationGatewayViewModel @Inject constructor(
    private val appLaunchStateRepository: AppLaunchStateRepository
) : ViewModel(), InitializationGatewayViewModel {

    override val result: StateFlow<InitializationResult> = MutableStateFlow(
        if (appLaunchStateRepository.hasCompletedOnboarding()) {
            InitializationResult.AppShell
        } else {
            InitializationResult.Onboarding
        }
    )
}

@Composable
fun InitializationGateway(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToAppShell: () -> Unit,
    viewModel: InitializationGatewayViewModel = androidx.hilt.navigation.compose.hiltViewModel<ConsumaInitializationGatewayViewModel>()
) {
    val currentResult by viewModel.result.collectAsState()
    val currentOnNavigateToOnboarding by rememberUpdatedState(onNavigateToOnboarding)
    val currentOnNavigateToAppShell by rememberUpdatedState(onNavigateToAppShell)

    LaunchedEffect(currentResult) {
        when (currentResult) {
            is InitializationResult.Onboarding -> currentOnNavigateToOnboarding()
            is InitializationResult.AppShell -> currentOnNavigateToAppShell()
            is InitializationResult.Failure -> { /* Future: error handling */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(NavigationTestTags.INITIALIZATION),
        contentAlignment = Alignment.Center
    ) {
        ConsumaLoadingIndicator()
    }
}
