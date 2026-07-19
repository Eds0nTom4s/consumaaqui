package ao.consuma.aqui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.cart.presentation.cart.CartScreen
import ao.consuma.aqui.feature.cart.presentation.cart.CartUiEvent
import ao.consuma.aqui.feature.cart.presentation.cart.CartUiState
import ao.consuma.aqui.feature.cart.presentation.components.CartActionButton
import ao.consuma.aqui.feature.cart.presentation.components.CartConflictDialog
import ao.consuma.aqui.feature.cart.presentation.mapper.CartBadgeUiState
import ao.consuma.aqui.feature.cart.presentation.mapper.CartItemUiModel
import ao.consuma.aqui.feature.cart.presentation.mapper.CartMerchantUiModel
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CartPresentationTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun empty_cart_explores_merchants() {
        val events = mutableListOf<CartUiEvent>()
        composeTestRule.setContent {
            ConsumaAquiTheme {
                CartScreen(
                    CartUiState.Empty(CartUiText.Resource(R.string.cart_empty_description)),
                    {},
                    events::add
                )
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText("Explorar comerciantes").performClick()
        assertTrue(events.contains(CartUiEvent.ExploreMerchants))
    }

    @Test fun content_exposes_quantity_remove_clear_and_estimated_subtotal() {
        val events = mutableListOf<CartUiEvent>()
        val item = item()
        composeTestRule.setContent {
            ConsumaAquiTheme {
                CartScreen(content(item), {}, events::add)
            }
        }
        composeTestRule.onNodeWithTag("${NavigationTestTags.CART_ITEM}_${item.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_ITEM_INCREASE).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_ITEM_REMOVE).performClick()
        assertTrue(events.contains(CartUiEvent.IncreaseQuantity(item.id)))
        assertTrue(events.contains(CartUiEvent.RequestRemoveItem(item.id)))
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CART_SUBTOTAL)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_SUBTOTAL).assertIsDisplayed()
        composeTestRule.onNodeWithText("A finalização será implementada na próxima fase.").assertIsDisplayed()
    }

    @Test fun destructive_confirmations_are_contextual() {
        val item = item()
        var state by mutableStateOf(content(item).copy(pendingRemovalItemId = item.id))
        composeTestRule.setContent {
            ConsumaAquiTheme {
                CartScreen(state, {}, {})
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_REMOVE_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Remover Muamba da Casa do carrinho?").assertIsDisplayed()
        composeTestRule.runOnIdle { state = content(item).copy(showClearConfirmation = true) }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_CLEAR_DIALOG).assertIsDisplayed()
    }

    @Test fun quantity_boundaries_and_actions_have_accessible_semantics() {
        composeTestRule.setContent {
            ConsumaAquiTheme { CartScreen(content(item()), {}, {}) }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_ITEM_DECREASE)
            .assertIsNotEnabled()
            .assertContentDescriptionContains("Diminuir quantidade")
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_ITEM_INCREASE)
            .assertIsEnabled()
            .assertContentDescriptionContains("Aumentar quantidade")
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_ITEM_EDIT).assertIsEnabled()
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_ITEM_REMOVE).assertIsEnabled()
        composeTestRule.onNodeWithText("Observação: Sem cebola").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tamanho: Grande").assertIsDisplayed()
    }

    @Test fun dark_theme_content_and_destructive_dialogs_compose() {
        composeTestRule.setContent {
            ConsumaAquiTheme(darkTheme = true) {
                CartScreen(content(item()).copy(showClearConfirmation = true), {}, {})
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_LIST).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_CLEAR_DIALOG).assertIsDisplayed()
    }

    @Test fun font_scale_one_point_three_and_one_point_five_remains_scrollable() {
        var fontScale by mutableStateOf(1.3f)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                ConsumaAquiTheme { CartScreen(content(item()), {}, {}) }
            }
        }
        listOf(1.3f, 1.5f).forEach { scale ->
            composeTestRule.runOnIdle { fontScale = scale }
            composeTestRule.onNodeWithTag(NavigationTestTags.CART_LIST).performScrollToNode(
                hasTestTag(NavigationTestTags.CART_SUBTOTAL)
            )
            composeTestRule.onNodeWithTag(NavigationTestTags.CART_SUBTOTAL).assertIsDisplayed()
            composeTestRule.onNodeWithTag(NavigationTestTags.CART_LIST).performScrollToNode(
                hasTestTag(NavigationTestTags.CART_CONTINUE)
            )
            composeTestRule.onNodeWithTag(NavigationTestTags.CART_CONTINUE).assertIsDisplayed()
            composeTestRule.onNodeWithTag(NavigationTestTags.CART_LIST).performScrollToNode(
                hasTestTag(NavigationTestTags.CART_CLEAR)
            )
            composeTestRule.onNodeWithTag(NavigationTestTags.CART_CLEAR).assertIsDisplayed()
        }
    }

    @Test fun badge_has_accessible_label_for_empty_singular_plural_and_overflow() {
        var state by mutableStateOf(
            CartBadgeUiState(
                accessibilityDescription = CartUiText.Resource(R.string.cart_badge_empty_accessibility)
            )
        )
        var clicks = 0
        composeTestRule.setContent {
            ConsumaAquiTheme { CartActionButton(state, { clicks++ }) }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_BADGE)
            .assertContentDescriptionContains("Carrinho vazio")
            .performClick()
        composeTestRule.runOnIdle {
            state = CartBadgeUiState(
                1, "1", true, CartUiText.Plural(R.plurals.cart_badge_accessibility, 1)
            )
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_BADGE)
            .assertContentDescriptionContains("Carrinho, 1 item")
        composeTestRule.runOnIdle {
            state = CartBadgeUiState(
                2, "2", true, CartUiText.Plural(R.plurals.cart_badge_accessibility, 2)
            )
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_BADGE)
            .assertContentDescriptionContains("Carrinho, 2 itens")
        composeTestRule.runOnIdle {
            state = CartBadgeUiState(
                100, "99+", true,
                CartUiText.Resource(R.string.cart_badge_overflow_accessibility)
            )
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_BADGE)
            .assertContentDescriptionContains("Carrinho, mais de 99 itens")
        assertTrue(clicks == 1)
    }

    @Test fun conflict_dialog_names_merchants_and_exposes_contextual_actions() {
        composeTestRule.setContent {
            ConsumaAquiTheme(darkTheme = true) {
                CartConflictDialog("Sabor da Maianga", "Café Horizonte", false, {}, {})
            }
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_CONFLICT).assertIsDisplayed()
        composeTestRule.onNodeWithText("Sabor da Maianga", substring = true).assertExists()
        composeTestRule.onNodeWithText("Café Horizonte", substring = true).assertExists()
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_CONFLICT_KEEP).assertIsEnabled()
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_CONFLICT_REPLACE).assertIsEnabled()
    }

    private fun content(item: CartItemUiModel) = CartUiState.Content(
        "cart", CartMerchantUiModel("merchant", "Sabor da Maianga"), listOf(item),
        1, 1, "4.500 Kz", "4.500 Kz", 1
    )

    private fun item() = CartItemUiModel(
        "item", "merchant", "product", "Muamba da Casa", false, null,
        listOf("Tamanho: Grande"), "Sem cebola", 1, "4.500 Kz", "4.500 Kz",
        false, true, CartUiText.Dynamic("Muamba da Casa")
    )
}
