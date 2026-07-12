package ao.consuma.aqui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import ao.consuma.aqui.core.appstate.rememberConsumaAppState
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import org.junit.Rule
import org.junit.Test

class SnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun snackbar_can_be_displayed_through_app_state() {
        val message = context.getString(R.string.snackbar_demo_message)

        composeTestRule.setContent {
            ConsumaAquiTheme {
                val appState = rememberConsumaAppState()
                Scaffold(
                    snackbarHost = { SnackbarHost(appState.snackbarHostState) }
                ) { paddingValues ->
                    Text(text = "Content", modifier = Modifier.padding(paddingValues))
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    appState.showSnackbar(message)
                }
            }
        }

        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }
}
