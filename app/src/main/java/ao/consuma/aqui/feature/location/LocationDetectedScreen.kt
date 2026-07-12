package ao.consuma.aqui.feature.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTextButton
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.launch.domain.MockLocation

@Composable
fun LocationDetectedScreen(
    location: MockLocation,
    onConfirm: () -> Unit,
    onChooseAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ConsumaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(ConsumaSize.iconXLarge),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(ConsumaSpacing.xl))
            Text(
                text = stringResource(R.string.location_found_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(ConsumaSpacing.md))
            Text(
                text = location.displayName,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
        ) {
            ConsumaPrimaryButton(
                text = stringResource(R.string.location_confirm),
                onClick = onConfirm,
                fullWidth = true,
                modifier = Modifier.testTag(NavigationTestTags.LOCATION_CONFIRM)
            )
            ConsumaTextButton(
                text = stringResource(R.string.location_choose_another),
                onClick = onChooseAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NavigationTestTags.LOCATION_CHOOSE_ANOTHER)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetectedScreenPreview() {
    ConsumaAquiTheme {
        LocationDetectedScreen(
            location = MockLocation(
                id = "maianga",
                city = "Luanda",
                area = "Maianga",
                displayName = "Luanda — Maianga"
            ),
            onConfirm = {},
            onChooseAnother = {}
        )
    }
}
