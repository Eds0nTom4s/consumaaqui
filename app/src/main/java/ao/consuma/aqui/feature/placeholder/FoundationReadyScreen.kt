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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.environment.AppEnvironmentProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FoundationReadyViewModel @Inject constructor(
    private val environmentProvider: AppEnvironmentProvider
) : ViewModel() {
    val environment: String = environmentProvider.currentEnvironment
    val version: String = environmentProvider.versionName
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
            text = stringResource(id = R.string.foundation_ready_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.foundation_ready_subtitle))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.environment_label, viewModel.environment))
        Text(text = stringResource(id = R.string.version_label, viewModel.version))
        Spacer(modifier = Modifier.height(32.dp))
        ConsumaPrimaryButton(
            text = stringResource(id = R.string.validate_navigation),
            onClick = { isValidated = true }
        )
        if (isValidated) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.navigation_success),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
