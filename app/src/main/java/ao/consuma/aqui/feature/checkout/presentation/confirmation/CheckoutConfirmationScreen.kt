package ao.consuma.aqui.feature.checkout.presentation.confirmation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaCard
import ao.consuma.aqui.core.designsystem.components.ConsumaErrorState
import ao.consuma.aqui.core.designsystem.components.ConsumaInlineMessage
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingState
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutConfirmationUiModel

@Composable
fun CheckoutConfirmationScreen(
    uiState: CheckoutConfirmationUiState,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier.fillMaxSize().testTag(NavigationTestTags.CHECKOUT_CONFIRMATION),
        topBar = {
            ConsumaTopAppBar(
                stringResource(R.string.checkout_confirmation_title),
                onBackClick = onNavigateToCart
            )
        }
    ) { padding ->
        when (uiState) {
            CheckoutConfirmationUiState.Loading -> ConsumaLoadingState(Modifier.padding(padding))
            CheckoutConfirmationUiState.Missing -> ConsumaErrorState(
                stringResource(R.string.checkout_error_session),
                modifier = Modifier.padding(padding)
            )
            is CheckoutConfirmationUiState.Content -> ConfirmationContent(
                uiState.confirmation,
                onNavigateToCart,
                Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ConfirmationContent(
    value: CheckoutConfirmationUiModel,
    onNavigateToCart: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ConsumaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.lg, Alignment.CenterVertically)
    ) {
        Text(
            stringResource(R.string.checkout_confirmation_prepared),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(stringResource(R.string.checkout_confirmation_validated))
        ConsumaInlineMessage(
            stringResource(R.string.checkout_confirmation_warning),
            ConsumaStatusSemantic.WARNING
        )
        ConsumaCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(ConsumaSpacing.md),
                verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)
            ) {
                Text(stringResource(R.string.checkout_simulation_reference), style = MaterialTheme.typography.labelLarge)
                Text(value.clientReference, style = MaterialTheme.typography.titleMedium)
                Text(value.merchantName)
                Text(
                    stringResource(
                        if (value.method == FulfillmentMethod.PICKUP) {
                            R.string.checkout_pickup_review
                        } else {
                            R.string.checkout_delivery_review
                        }
                    )
                )
                Text(
                    stringResource(R.string.checkout_total_estimated),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(value.total, style = MaterialTheme.typography.titleLarge)
            }
        }
        ConsumaPrimaryButton(
            stringResource(R.string.checkout_back_to_cart),
            onNavigateToCart,
            Modifier.fillMaxWidth(),
            fullWidth = true
        )
    }
}

private fun previewConfirmation(method: FulfillmentMethod) = CheckoutConfirmationUiState.Content(
    CheckoutConfirmationUiModel("AB12CD34", "Sabor da Maianga", "6.000 Kz", method)
)

@Preview(showBackground = true) @Composable private fun CheckoutConfirmationPickupPreview() {
    ConsumaAquiTheme {
        CheckoutConfirmationScreen(previewConfirmation(FulfillmentMethod.PICKUP), {})
    }
}
@Preview(showBackground = true) @Composable private fun CheckoutConfirmationDeliveryPreview() {
    ConsumaAquiTheme {
        CheckoutConfirmationScreen(previewConfirmation(FulfillmentMethod.DELIVERY_MOCK), {})
    }
}
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun CheckoutConfirmationDarkPreview() {
    ConsumaAquiTheme {
        CheckoutConfirmationScreen(previewConfirmation(FulfillmentMethod.DELIVERY_MOCK), {})
    }
}
