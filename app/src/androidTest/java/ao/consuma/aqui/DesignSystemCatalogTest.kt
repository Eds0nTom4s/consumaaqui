package ao.consuma.aqui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.feature.placeholder.DesignSystemCatalogScreen
import org.junit.Rule
import org.junit.Test

class DesignSystemCatalogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun catalog_displaysSectionsAndScrollsToLowerContent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val buttons = context.getString(R.string.catalog_buttons_title)
        val inputs = context.getString(R.string.catalog_inputs_title)
        val cards = context.getString(R.string.catalog_cards_title)
        val prices = context.getString(R.string.catalog_prices_title)
        val screenStates = context.getString(R.string.catalog_screen_states_title)

        composeTestRule.setContent {
            ConsumaAquiTheme {
                DesignSystemCatalogScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithTag("design_system_catalog").assertIsDisplayed()
        composeTestRule.onNodeWithText(buttons).assertExists()
        composeTestRule.onNodeWithText(inputs).assertExists()
        composeTestRule.onNodeWithText(cards).assertExists()

        composeTestRule.onNodeWithText(prices).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(screenStates).performScrollTo().assertIsDisplayed()
    }
}
