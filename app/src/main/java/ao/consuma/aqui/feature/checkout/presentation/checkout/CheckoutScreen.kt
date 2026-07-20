package ao.consuma.aqui.feature.checkout.presentation.checkout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import ao.consuma.aqui.core.designsystem.components.ConsumaErrorState
import ao.consuma.aqui.core.designsystem.components.ConsumaInlineMessage
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingState
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaSecondaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.presentation.components.CheckoutReviewSection
import ao.consuma.aqui.feature.checkout.presentation.components.CheckoutStepIndicator
import ao.consuma.aqui.feature.checkout.presentation.components.CustomerForm
import ao.consuma.aqui.feature.checkout.presentation.components.DeliveryForm
import ao.consuma.aqui.feature.checkout.presentation.components.FulfillmentSelector
import ao.consuma.aqui.feature.checkout.presentation.components.PickupForm
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCapabilitiesUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCartSummaryUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutChargeUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCustomerReviewUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutFulfillmentReviewUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutItemUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutQuoteUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutStepUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiText
import ao.consuma.aqui.feature.checkout.presentation.mapper.resolve

@Composable
fun CheckoutScreen(
    uiState: CheckoutUiState,
    onNavigateBack: () -> Unit,
    onEvent: (CheckoutUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(NavigationTestTags.CHECKOUT),
        topBar = {
            ConsumaTopAppBar(
                stringResource(R.string.checkout_title),
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        when (uiState) {
            CheckoutUiState.Initializing, CheckoutUiState.ConfirmedMock ->
                ConsumaLoadingState(Modifier.padding(padding))
            is CheckoutUiState.Content -> CheckoutContent(uiState, onEvent, Modifier.padding(padding))
            is CheckoutUiState.CartConflict -> CheckoutConflict(uiState, onEvent, Modifier.padding(padding))
            is CheckoutUiState.Error -> ConsumaErrorState(
                uiState.message.resolve(),
                if (uiState.canRetry) ({ onEvent(CheckoutUiEvent.Initialize) }) else null,
                Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun CheckoutContent(
    state: CheckoutUiState.Content,
    onEvent: (CheckoutUiEvent) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier.fillMaxSize().testTag(NavigationTestTags.CHECKOUT_LIST),
        contentPadding = PaddingValues(ConsumaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.lg)
    ) {
        item("step") { CheckoutStepIndicator(state.availableSteps) }
        item("body") {
            when (state.currentStep) {
                CheckoutStep.FULFILLMENT -> FulfillmentSelector(
                    state.capabilities,
                    state.selectedFulfillment,
                    state.fulfillmentError,
                    !state.isMutating,
                    { onEvent(CheckoutUiEvent.FulfillmentSelected(it)) }
                )
                CheckoutStep.CUSTOMER -> CustomerForm(
                    state.customerForm,
                    !state.isMutating,
                    onEvent
                )
                CheckoutStep.DETAILS -> when (state.selectedFulfillment) {
                    FulfillmentMethod.PICKUP -> PickupForm(
                        state.pickupForm,
                        !state.isMutating,
                        onEvent
                    )
                    FulfillmentMethod.DELIVERY_MOCK -> DeliveryForm(
                        state.deliveryForm,
                        !state.isMutating,
                        onEvent
                    )
                    null -> Unit
                }
                CheckoutStep.REVIEW -> CheckoutReviewSection(
                    state.cartSummary,
                    state.customerReview,
                    state.fulfillmentReview,
                    state.quote,
                    onEvent
                )
                CheckoutStep.CONFIRMATION -> Unit
            }
        }
        if (state.pendingOperation == CheckoutPendingOperation.RequestingQuote) {
            item("quote_loading") {
                ConsumaInlineMessage(
                    stringResource(R.string.checkout_requesting_quote),
                    ConsumaStatusSemantic.INFO
                )
            }
        }
        item("actions") {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)
            ) {
                if (state.currentStep == CheckoutStep.REVIEW) {
                    if (state.quote?.expired == true) {
                        ConsumaPrimaryButton(
                            stringResource(R.string.checkout_refresh_quote),
                            { onEvent(CheckoutUiEvent.RefreshQuote) },
                            Modifier.fillMaxWidth()
                                .testTag(NavigationTestTags.CHECKOUT_REFRESH_QUOTE),
                            enabled = !state.isMutating,
                            loading = state.pendingOperation == CheckoutPendingOperation.RequestingQuote,
                            fullWidth = true
                        )
                    } else {
                        ConsumaPrimaryButton(
                            stringResource(R.string.checkout_confirm_intention),
                            { onEvent(CheckoutUiEvent.Confirm) },
                            Modifier.fillMaxWidth().testTag(NavigationTestTags.CHECKOUT_CONFIRM),
                            enabled = state.canConfirm,
                            loading = state.pendingOperation == CheckoutPendingOperation.Confirming,
                            fullWidth = true
                        )
                    }
                } else {
                    ConsumaPrimaryButton(
                        stringResource(R.string.checkout_continue),
                        { onEvent(CheckoutUiEvent.Continue) },
                        Modifier.fillMaxWidth().testTag(NavigationTestTags.CHECKOUT_CONTINUE),
                        enabled = state.canContinue,
                        loading = state.isMutating,
                        fullWidth = true
                    )
                }
                ConsumaSecondaryButton(
                    stringResource(R.string.checkout_back_step),
                    { onEvent(CheckoutUiEvent.BackStep) },
                    Modifier.fillMaxWidth().testTag(NavigationTestTags.CHECKOUT_BACK_STEP),
                    enabled = state.canGoBack,
                    fullWidth = true
                )
            }
        }
    }
}

@Composable
private fun CheckoutConflict(
    state: CheckoutUiState.CartConflict,
    onEvent: (CheckoutUiEvent) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier.fillMaxSize().padding(ConsumaSpacing.xl).testTag(NavigationTestTags.CHECKOUT_CONFLICT),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
        ) {
            Text(state.message.resolve(), style = MaterialTheme.typography.titleMedium)
            ConsumaPrimaryButton(
                stringResource(R.string.checkout_review_cart),
                { onEvent(CheckoutUiEvent.ReviewCart) },
                Modifier.testTag(NavigationTestTags.CHECKOUT_REVIEW_CART)
            )
            if (state.canRestart) ConsumaSecondaryButton(
                stringResource(R.string.checkout_restart),
                { onEvent(CheckoutUiEvent.RestartCheckout) },
                Modifier.testTag(NavigationTestTags.CHECKOUT_RESTART)
            )
        }
    }
}

private fun previewContent(
    step: CheckoutStep,
    method: FulfillmentMethod? = null,
    deliveryAvailable: Boolean = true,
    quoteExpired: Boolean = false
) = CheckoutUiState.Content(
    sessionId = "preview-session",
    currentStep = step,
    availableSteps = listOf(
        CheckoutStep.FULFILLMENT,
        CheckoutStep.CUSTOMER,
        CheckoutStep.DETAILS,
        CheckoutStep.REVIEW
    ).map {
        CheckoutStepUiModel(
            it,
            CheckoutUiText.Resource(when (it) {
                CheckoutStep.FULFILLMENT -> R.string.checkout_step_fulfillment
                CheckoutStep.CUSTOMER -> R.string.checkout_step_customer
                CheckoutStep.DETAILS -> R.string.checkout_step_details
                CheckoutStep.REVIEW -> R.string.checkout_step_review
                CheckoutStep.CONFIRMATION -> R.string.checkout_confirmation_title
            }),
            it.ordinal < step.ordinal,
            it == step
        )
    },
    selectedFulfillment = method,
    capabilities = CheckoutCapabilitiesUiModel(true, deliveryAvailable),
    customerForm = CustomerFormUiState("Ana Silva", "+244 923 456 789", "ana@example.com"),
    pickupForm = PickupFormUiState("Ana Silva", "+244 923 456 789"),
    deliveryForm = DeliveryFormUiState(
        "Luanda", "Talatona", "Benfica", "Rua principal", "Casa 10",
        "Próximo ao Banco BIC", "Ana Silva", "+244 923 456 789", "Ligar ao chegar"
    ),
    cartSummary = CheckoutCartSummaryUiModel(
        "Sabor da Maianga",
        listOf(CheckoutItemUiModel("item", "Muamba da Casa", 1, emptyList(), null, "4.500 Kz")),
        1,
        "4.500 Kz"
    ),
    quote = if (step == CheckoutStep.REVIEW) CheckoutQuoteUiModel(
        "4.500 Kz",
        if (method == FulfillmentMethod.DELIVERY_MOCK) listOf(
            CheckoutChargeUiModel(
                CheckoutUiText.Resource(R.string.checkout_delivery_charge),
                "1.500 Kz"
            )
        ) else emptyList(),
        if (method == FulfillmentMethod.DELIVERY_MOCK) "6.000 Kz" else "4.500 Kz",
        25,
        if (method == FulfillmentMethod.DELIVERY_MOCK) 35 else null,
        "20:30",
        quoteExpired
    ) else null,
    customerReview = CheckoutCustomerReviewUiModel("Ana Silva", "+244 923 456 789", "ana@example.com"),
    fulfillmentReview = when (method) {
        FulfillmentMethod.PICKUP -> CheckoutFulfillmentReviewUiModel.Pickup(
            "Ana Silva", "+244 923 456 789"
        )
        FulfillmentMethod.DELIVERY_MOCK -> CheckoutFulfillmentReviewUiModel.Delivery(
            "Ana Silva", "+244 923 456 789", listOf("Luanda", "Talatona", "Rua principal"), null
        )
        null -> null
    },
    fulfillmentError = null,
    pendingOperation = null,
    canGoBack = true,
    canContinue = true,
    canConfirm = step == CheckoutStep.REVIEW && !quoteExpired
)

@Preview(showBackground = true) @Composable private fun CheckoutInitializingPreview() {
    ConsumaAquiTheme { CheckoutScreen(CheckoutUiState.Initializing, {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CheckoutFulfillmentPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.FULFILLMENT), {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CheckoutPickupOnlyPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.FULFILLMENT, deliveryAvailable = false), {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CheckoutCustomerPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.CUSTOMER, FulfillmentMethod.PICKUP), {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CheckoutPickupDetailsPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.DETAILS, FulfillmentMethod.PICKUP), {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CheckoutDeliveryDetailsPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.DETAILS, FulfillmentMethod.DELIVERY_MOCK), {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CheckoutReviewPickupPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.REVIEW, FulfillmentMethod.PICKUP), {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CheckoutReviewDeliveryPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.REVIEW, FulfillmentMethod.DELIVERY_MOCK), {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CheckoutConflictPreview() {
    ConsumaAquiTheme {
        CheckoutScreen(
            CheckoutUiState.CartConflict(CheckoutUiText.Resource(R.string.checkout_cart_changed), true),
            {}, {}
        )
    }
}
@Preview(showBackground = true) @Composable private fun CheckoutExpiredPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.REVIEW, FulfillmentMethod.PICKUP, quoteExpired = true), {}, {}) }
}
@Preview(showBackground = true) @Composable private fun CheckoutErrorPreview() {
    ConsumaAquiTheme {
        CheckoutScreen(
            CheckoutUiState.Error(CheckoutUiText.Resource(R.string.checkout_error_generic), true),
            {}, {}
        )
    }
}
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun CheckoutDarkPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.REVIEW, FulfillmentMethod.DELIVERY_MOCK), {}, {}) }
}
@Preview(showBackground = true, fontScale = 1.5f)
@Composable private fun CheckoutLargeFontPreview() {
    ConsumaAquiTheme { CheckoutScreen(previewContent(CheckoutStep.DETAILS, FulfillmentMethod.DELIVERY_MOCK), {}, {}) }
}
