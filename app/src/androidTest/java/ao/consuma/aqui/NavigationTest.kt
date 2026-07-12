package ao.consuma.aqui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_navigates_from_splash_to_foundation_ready_and_validates() {
        // Wait for navigation to FoundationReady since Splash is fast
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodes(hasText("Fundação Android configurada."))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("CONSUMA AQUI").assertExists()
        composeTestRule.onNodeWithText("Validar navegação").assertExists()
        
        composeTestRule.onNodeWithText("Validar navegação").performClick()
        
        composeTestRule.onNodeWithText("Navegação validada com sucesso!").assertExists()
    }
}
