package ao.consuma.aqui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.presentation.checkout.CheckoutScreen
import ao.consuma.aqui.feature.checkout.presentation.checkout.CheckoutUiEvent
import ao.consuma.aqui.feature.checkout.presentation.checkout.CheckoutUiState
import ao.consuma.aqui.feature.checkout.presentation.checkout.CustomerFormUiState
import ao.consuma.aqui.feature.checkout.presentation.checkout.DeliveryFormUiState
import ao.consuma.aqui.feature.checkout.presentation.checkout.PickupFormUiState
import ao.consuma.aqui.feature.checkout.presentation.confirmation.CheckoutConfirmationScreen
import ao.consuma.aqui.feature.checkout.presentation.confirmation.CheckoutConfirmationUiState
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCapabilitiesUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCartSummaryUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutChargeUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutConfirmationUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCustomerReviewUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutFulfillmentReviewUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutItemUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutQuoteUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutStepUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CheckoutPresentationTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun fulfillment_exposes_available_selected_and_mock_delivery_semantics() {
        val events = mutableListOf<CheckoutUiEvent>()
        composeTestRule.setContent {
            ConsumaAquiTheme {
                CheckoutScreen(content(CheckoutStep.FULFILLMENT), {}, events::add)
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_PICKUP).assertIsEnabled()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_DELIVERY).assertIsEnabled().performClick()
        composeTestRule.onNodeWithText(
            "Entrega simulada. Nenhum operador logístico será accionado."
        ).assertIsDisplayed()
        assertTrue(events.contains(CheckoutUiEvent.FulfillmentSelected(FulfillmentMethod.DELIVERY_MOCK)))
    }

    @Test fun pickup_only_disables_delivery() {
        val pickupOnly = content(CheckoutStep.FULFILLMENT).copy(
            capabilities = CheckoutCapabilitiesUiModel(true, false)
        )
        composeTestRule.setContent {
            ConsumaAquiTheme { CheckoutScreen(pickupOnly, {}, {}) }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_DELIVERY).assertIsNotEnabled()
    }

    @Test fun customer_fields_emit_typed_events() {
        val events = mutableListOf<CheckoutUiEvent>()
        composeTestRule.setContent {
            ConsumaAquiTheme {
                CheckoutScreen(content(CheckoutStep.CUSTOMER, FulfillmentMethod.PICKUP), {}, events::add)
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CUSTOMER_NAME)
            .performTextInput("Ana")
        assertTrue(events.any { it is CheckoutUiEvent.CustomerNameChanged })
    }

    @Test fun delivery_form_prioritizes_angolan_address_reference_and_instruction_counter() {
        composeTestRule.setContent {
            ConsumaAquiTheme {
                CheckoutScreen(
                    content(CheckoutStep.DETAILS, FulfillmentMethod.DELIVERY_MOCK),
                    {}, {}
                )
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_DELIVERY_PROVINCE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_DELIVERY_MUNICIPALITY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CHECKOUT_DELIVERY_REFERENCE)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_DELIVERY_REFERENCE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ex.: próximo ao Banco BIC, edifício azul.").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 de 300 caracteres").assertIsDisplayed()
    }

    @Test fun review_shows_estimates_financial_notice_and_confirm_intention() {
        val events = mutableListOf<CheckoutUiEvent>()
        composeTestRule.setContent {
            ConsumaAquiTheme {
                CheckoutScreen(
                    content(CheckoutStep.REVIEW, FulfillmentMethod.DELIVERY_MOCK),
                    {}, events::add
                )
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_REVIEW).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CHECKOUT_QUOTE)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_QUOTE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CHECKOUT_TOTAL)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_TOTAL).assertExists()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_LIST).performScrollToNode(
            hasText("Os valores, produtos e disponibilidade serão validados pelo servidor", substring = true)
        )
        composeTestRule.onNodeWithText(
            "Os valores, produtos e disponibilidade serão validados pelo servidor",
            substring = true
        ).assertExists()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CHECKOUT_CONFIRM)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CONFIRM).performClick()
        assertTrue(events.contains(CheckoutUiEvent.Confirm))
    }

    @Test fun expired_quote_disables_confirmation_and_offers_refresh() {
        val events = mutableListOf<CheckoutUiEvent>()
        val expired = content(CheckoutStep.REVIEW, FulfillmentMethod.PICKUP).let {
            it.copy(quote = it.quote?.copy(expired = true), canConfirm = false)
        }
        composeTestRule.setContent {
            ConsumaAquiTheme { CheckoutScreen(expired, {}, events::add) }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CHECKOUT_REFRESH_QUOTE)
        )
        composeTestRule.onNodeWithText("A estimativa expirou.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_REFRESH_QUOTE).performClick()
        composeTestRule.waitForIdle()
        assertTrue(events.contains(CheckoutUiEvent.RefreshQuote))
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CONFIRM).assertDoesNotExist()
    }

    @Test fun conflict_exposes_review_cart_and_restart_without_reconciliation() {
        val events = mutableListOf<CheckoutUiEvent>()
        composeTestRule.setContent {
            ConsumaAquiTheme {
                CheckoutScreen(
                    CheckoutUiState.CartConflict(
                        CheckoutUiText.Resource(R.string.checkout_cart_changed),
                        true
                    ),
                    {}, events::add
                )
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CONFLICT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_REVIEW_CART).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_RESTART).performClick()
        assertTrue(events.contains(CheckoutUiEvent.ReviewCart))
        assertTrue(events.contains(CheckoutUiEvent.RestartCheckout))
    }

    @Test fun confirmation_uses_simulation_language_and_has_no_order_or_payment_claims() {
        composeTestRule.setContent {
            ConsumaAquiTheme(darkTheme = true) {
                CheckoutConfirmationScreen(
                    CheckoutConfirmationUiState.Content(
                        CheckoutConfirmationUiModel(
                            "AB12CD34", "Sabor da Maianga", "6.000 Kz",
                            FulfillmentMethod.DELIVERY_MOCK
                        )
                    ),
                    {}
                )
            }
        }
        composeTestRule.onNodeWithText("Intenção preparada").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Esta versão ainda não cria um pedido real nem realiza pagamento."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Pedido enviado").assertDoesNotExist()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CONFIRMATION).assertIsDisplayed()
    }

    @Test fun large_font_checkout_remains_scrollable_to_actions() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                ConsumaAquiTheme {
                    CheckoutScreen(
                        content(CheckoutStep.DETAILS, FulfillmentMethod.DELIVERY_MOCK),
                        {}, {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CHECKOUT_CONTINUE)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CONTINUE).assertIsDisplayed()
    }

    @Test fun dark_theme_checkout_states_compose_without_losing_primary_semantics() {
        val state = mutableStateOf<CheckoutUiState>(
            content(CheckoutStep.FULFILLMENT, FulfillmentMethod.DELIVERY_MOCK)
        )
        composeTestRule.setContent {
            ConsumaAquiTheme(darkTheme = true) { CheckoutScreen(state.value, {}, {}) }
        }
        val states = listOf(
            content(CheckoutStep.CUSTOMER, FulfillmentMethod.PICKUP),
            content(CheckoutStep.DETAILS, FulfillmentMethod.PICKUP),
            content(CheckoutStep.DETAILS, FulfillmentMethod.DELIVERY_MOCK),
            content(CheckoutStep.REVIEW, FulfillmentMethod.PICKUP),
            content(CheckoutStep.REVIEW, FulfillmentMethod.DELIVERY_MOCK),
            CheckoutUiState.CartConflict(CheckoutUiText.Resource(R.string.checkout_cart_changed), true),
            CheckoutUiState.Error(CheckoutUiText.Resource(R.string.checkout_error_generic), true)
        )
        states.forEach { next ->
            composeTestRule.runOnIdle { state.value = next }
            composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT).assertIsDisplayed()
        }
    }

    @Test fun font_scale_one_point_three_step_semantics_and_confirmation_actions_are_accessible() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.3f)) {
                ConsumaAquiTheme {
                    CheckoutScreen(content(CheckoutStep.CUSTOMER, FulfillmentMethod.PICKUP), {}, {})
                }
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_STEP).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Etapa 2 de 4: Os seus dados"
            )
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CHECKOUT_CONTINUE)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CONTINUE).assertIsDisplayed()
    }

    @Test fun confirmation_at_one_point_five_font_scale_scrolls_to_cart_action() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                ConsumaAquiTheme(darkTheme = true) {
                    CheckoutConfirmationScreen(
                        CheckoutConfirmationUiState.Content(
                            CheckoutConfirmationUiModel(
                                "AB12CD34", "Sabor da Maianga", "6.000 Kz",
                                FulfillmentMethod.DELIVERY_MOCK
                            )
                        ),
                        {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Voltar ao carrinho").performScrollTo().assertIsDisplayed()
    }

    private fun content(
        step: CheckoutStep,
        method: FulfillmentMethod? = null
    ) = CheckoutUiState.Content(
        "session",
        step,
        listOf(
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
        method,
        CheckoutCapabilitiesUiModel(true, true),
        CustomerFormUiState("Ana Silva", "+244923456789", "ana@example.com"),
        PickupFormUiState("Ana Silva", "+244923456789"),
        DeliveryFormUiState(
            "Luanda", "Talatona", "Benfica", "Rua 10", "Casa azul", "Banco BIC",
            "Ana Silva", "+244923456789", "Ligar ao chegar"
        ),
        CheckoutCartSummaryUiModel(
            "Sabor da Maianga",
            listOf(CheckoutItemUiModel("item", "Muamba", 1, emptyList(), null, "4.500 Kz")),
            1,
            "4.500 Kz"
        ),
        if (step == CheckoutStep.REVIEW) CheckoutQuoteUiModel(
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
            false
        ) else null,
        CheckoutCustomerReviewUiModel("Ana Silva", "+244923456789", "ana@example.com"),
        when (method) {
            FulfillmentMethod.PICKUP -> CheckoutFulfillmentReviewUiModel.Pickup(
                "Ana Silva", "+244923456789"
            )
            FulfillmentMethod.DELIVERY_MOCK -> CheckoutFulfillmentReviewUiModel.Delivery(
                "Ana Silva", "+244923456789", listOf("Luanda", "Talatona", "Rua 10"), null
            )
            null -> null
        },
        null,
        null,
        true,
        true,
        step == CheckoutStep.REVIEW
    )
}
