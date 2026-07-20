package ao.consuma.aqui.feature.checkout.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutStepUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.resolve
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiText
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme

@Composable
fun CheckoutStepIndicator(
    steps: List<CheckoutStepUiModel>,
    modifier: Modifier = Modifier
) {
    val currentIndex = steps.indexOfFirst { it.current }.coerceAtLeast(0)
    val title = steps.getOrNull(currentIndex)?.title?.resolve().orEmpty()
    val description = stringResource(
        R.string.checkout_step_progress,
        currentIndex + 1,
        steps.size,
        title
    )
    val progress = if (steps.isEmpty()) 0f else (currentIndex + 1f) / steps.size
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(NavigationTestTags.CHECKOUT_STEP)
            .semantics {
                stateDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f, steps.size)
            },
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)
    ) {
        Text(description, style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckoutStepIndicatorPreview() {
    ConsumaAquiTheme {
        CheckoutStepIndicator(
            listOf(
                CheckoutStepUiModel(
                    CheckoutStep.FULFILLMENT,
                    CheckoutUiText.Resource(R.string.checkout_step_fulfillment),
                    completed = true,
                    current = false
                ),
                CheckoutStepUiModel(
                    CheckoutStep.CUSTOMER,
                    CheckoutUiText.Resource(R.string.checkout_step_customer),
                    completed = false,
                    current = true
                ),
                CheckoutStepUiModel(
                    CheckoutStep.DETAILS,
                    CheckoutUiText.Resource(R.string.checkout_step_details),
                    completed = false,
                    current = false
                ),
                CheckoutStepUiModel(
                    CheckoutStep.REVIEW,
                    CheckoutUiText.Resource(R.string.checkout_step_review),
                    completed = false,
                    current = false
                )
            )
        )
    }
}
