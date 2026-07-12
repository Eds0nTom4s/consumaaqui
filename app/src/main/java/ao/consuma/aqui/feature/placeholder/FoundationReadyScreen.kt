package ao.consuma.aqui.feature.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.environment.EnvironmentResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FoundationReadyViewModel @Inject constructor(
    private val environmentResolver: EnvironmentResolver
) : ViewModel() {
    val environment: String = environmentResolver.currentEnvironment
    val version: String = environmentResolver.versionName
}

@Composable
fun FoundationReadyScreen(
    viewModel: FoundationReadyViewModel = hiltViewModel()
) {
    var isValidated by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CONSUMA AQUI",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Fundação Android configurada.")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Ambiente: ${viewModel.environment}")
        Text(text = "Versão: ${viewModel.version}")
        Spacer(modifier = Modifier.height(32.dp))
        ConsumaPrimaryButton(
            text = "Validar navegação",
            onClick = { isValidated = true }
        )
        if (isValidated) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Navegação validada com sucesso!",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
