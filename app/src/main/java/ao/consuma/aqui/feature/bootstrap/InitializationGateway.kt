package ao.consuma.aqui.feature.bootstrap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingIndicator
import ao.consuma.aqui.core.navigation.NavigationTestTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface InitializationResult {
    data object Ready : InitializationResult
    data object Failure : InitializationResult
}

interface InitializationGatewayViewModel {
    val result: StateFlow<InitializationResult>
}

internal class FakeInitializationGatewayViewModel : InitializationGatewayViewModel {
    override val result = MutableStateFlow<InitializationResult>(InitializationResult.Ready)
}

@Composable
fun InitializationGateway(
    onInitializationComplete: () -> Unit,
    viewModel: InitializationGatewayViewModel = FakeInitializationGatewayViewModel()
) {
    val currentResult = viewModel.result

    LaunchedEffect(currentResult) {
        currentResult.collect { state ->
            when (state) {
                is InitializationResult.Ready -> onInitializationComplete()
                is InitializationResult.Failure -> { /* Future: error handling */ }
            }
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
