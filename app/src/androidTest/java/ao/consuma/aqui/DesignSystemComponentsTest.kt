package ao.consuma.aqui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaErrorState
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaPriceText
import ao.consuma.aqui.core.designsystem.components.ConsumaSearchField
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusChip
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.components.ConsumaTextField
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DesignSystemComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun primaryButton_executesCallback() {
        var clicked = false
        composeTestRule.setContent {
            ConsumaAquiTheme {
                ConsumaPrimaryButton(text = "Click me", onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("Click me").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun primaryButton_loading_doesNotExecuteCallback() {
        var clickCount = 0
        composeTestRule.setContent {
            ConsumaAquiTheme {
                ConsumaPrimaryButton(
                    text = "Loading",
                    loading = true,
                    onClick = { clickCount++ },
                    modifier = Modifier.testTag("loading_btn")
                )
            }
        }
        composeTestRule.onNodeWithText("Loading").assertDoesNotExist()
        composeTestRule.onNodeWithTag("loading_btn").performClick()
        composeTestRule.onNodeWithTag("loading_btn").performClick()
        assertEquals(0, clickCount)
        composeTestRule.onNodeWithContentDescription("A carregar").assertExists()
    }

    @Test
    fun discountedPrice_exposesMeaningfulAccessibilityDescription() {
        composeTestRule.setContent {
            ConsumaAquiTheme {
                ConsumaPriceText(price = "5.500 Kz", oldPrice = "6.000 Kz")
            }
        }
        composeTestRule
            .onNodeWithContentDescription("Preço promocional: 5.500 Kz; preço anterior: 6.000 Kz")
            .assertExists()
    }

    @Test
    fun primaryButton_disabled_doesNotExecuteCallback() {
        var clicked = false
        composeTestRule.setContent {
            ConsumaAquiTheme {
                ConsumaPrimaryButton(text = "Disabled", enabled = false, onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("Disabled").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Disabled").performClick()
        assertEquals(false, clicked)
    }

    @Test
    fun textField_displaysErrorAndSupportingText() {
        composeTestRule.setContent {
            ConsumaAquiTheme {
                ConsumaTextField(
                    value = "",
                    onValueChange = {},
                    error = true,
                    supportingText = "Error message"
                )
            }
        }
        composeTestRule.onNodeWithText("Error message").assertIsDisplayed()
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "Error message")
        ).assertExists()
    }

    @Test
    fun searchField_acceptsInput() {
        var query = ""
        composeTestRule.setContent {
            ConsumaAquiTheme {
                ConsumaSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.testTag("search")
                )
            }
        }
        composeTestRule.onNodeWithTag("search").performTextInput("Hamburguer")
        assertEquals("Hamburguer", query)
    }

    @Test
    fun statusChip_displaysText() {
        composeTestRule.setContent {
            ConsumaAquiTheme {
                ConsumaStatusChip(text = "INFO_TEST", semantic = ConsumaStatusSemantic.INFO)
            }
        }
        composeTestRule.onNodeWithText("INFO_TEST").assertIsDisplayed()
    }

    @Test
    fun emptyState_displaysTitleAndAction() {
        var clicked = false
        composeTestRule.setContent {
            ConsumaAquiTheme {
                ConsumaEmptyState(
                    title = "No data",
                    actionText = "Refresh",
                    onActionClick = { clicked = true }
                )
            }
        }
        composeTestRule.onNodeWithText("No data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Refresh").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun errorState_executesRetry() {
        var retried = false
        composeTestRule.setContent {
            ConsumaAquiTheme {
                ConsumaErrorState(
                    message = "Failed",
                    onRetry = { retried = true }
                )
            }
        }
        composeTestRule.onNodeWithText("Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tentar Novamente").performClick() // Assuming stringResource translates to this
        assertEquals(true, retried)
    }
}
