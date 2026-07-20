package ao.consuma.aqui

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.cart.domain.command.AddCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.ProductCartSnapshot
import ao.consuma.aqui.feature.cart.domain.command.UpdateCartItemQuantityCommand
import ao.consuma.aqui.feature.cart.domain.model.CartMerchant
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.catalog.domain.model.ConfiguredProduct
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.repository.CheckoutRepository
import ao.consuma.aqui.feature.launch.di.LaunchStateModule
import ao.consuma.aqui.feature.launch.domain.AppLaunchStateRepository
import ao.consuma.aqui.feature.launch.domain.InMemoryAppLaunchStateRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(LaunchStateModule::class)
class CheckoutEndToEndTest {
    @BindValue @JvmField
    var appLaunchStateRepository: AppLaunchStateRepository = InMemoryAppLaunchStateRepository()

    @get:Rule(order = 0)
    val launchStateRule = LaunchStateRule(
        setup = { completeOnboarding() },
        assign = { appLaunchStateRepository = it }
    )
    @get:Rule(order = 1) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 2) val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var cartRepository: CartRepository
    @Inject lateinit var checkoutRepository: CheckoutRepository

    @Before fun seedCart() = runBlocking {
        hiltRule.inject()
        checkoutRepository.resetCheckout()
        cartRepository.clearCart()
        assertTrue(cartRepository.addItem(cartCommand()) is AddCartItemResult.Added)
        waitForTag(NavigationTestTags.CART_BADGE)
    }

    @Test fun pickup_flow_rotation_confirmation_and_back_stack_preserve_cart() {
        val cartBefore = cartRepository.cart.value
        openCheckout()
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertDoesNotExist()
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_BADGE).assertDoesNotExist()

        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_PICKUP).performClick()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_PICKUP).assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                "Seleccionado"
            )
        )
        continueCheckout()

        input(NavigationTestTags.CHECKOUT_CUSTOMER_NAME, "Ana Teste")
        input(NavigationTestTags.CHECKOUT_CUSTOMER_PHONE, "+244 923 000 111")
        input(NavigationTestTags.CHECKOUT_CUSTOMER_EMAIL, "ana.teste@example.com")
        closeSoftKeyboard()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CUSTOMER_NAME)
            .assertTextContains("Ana Teste")
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CUSTOMER_PHONE)
            .assertTextContains("+244 923 000 111")
        continueCheckout()

        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_PICKUP_NAME)
            .assertTextContains("Ana Teste")
        composeTestRule.onNodeWithText("Assim que possível").assertIsDisplayed()
        continueCheckout()
        waitForTag(NavigationTestTags.CHECKOUT_REVIEW)
        assertEquals(CheckoutSessionStatus.ReadyForReview, checkoutRepository.session.value?.status)
        assertTrue(checkoutRepository.session.value?.quote?.charges.orEmpty().isEmpty())
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CHARGES).assertDoesNotExist()

        composeTestRule.activityRule.scenario.recreate()
        waitForTag(NavigationTestTags.CHECKOUT_REVIEW)
        scrollCheckoutTo(NavigationTestTags.CHECKOUT_CONFIRM)
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CONFIRM).performClick()
        waitForTag(NavigationTestTags.CHECKOUT_CONFIRMATION)
        assertNotNull(checkoutRepository.draft.value)
        assertEquals(cartBefore, cartRepository.cart.value)

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithText("Intenção preparada").assertIsDisplayed()
        composeTestRule.onNodeWithText("Voltar ao carrinho").performClick()
        waitForTag(NavigationTestTags.CART)
        assertEquals(cartBefore, cartRepository.cart.value)
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CART_CONTINUE)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_CONTINUE).performClick()
        waitForTag(NavigationTestTags.CHECKOUT_FULFILLMENT)
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_PICKUP).performClick()
        continueCheckout()
        val restoredName = composeTestRule.onNodeWithTag(
            NavigationTestTags.CHECKOUT_CUSTOMER_NAME
        ).fetchSemanticsNode().config[
            androidx.compose.ui.semantics.SemanticsProperties.EditableText
        ].text
        assertEquals("", restoredName)
        pressActivityBack()
        waitForTag(NavigationTestTags.CART)
        pressActivityBack()
        waitForTag(NavigationTestTags.HOME)
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_BADGE)
            .assertContentDescriptionContains("Carrinho, 1 item")
    }

    @Test fun delivery_mock_flow_collects_address_quotes_and_preserves_cart() {
        val cartBefore = cartRepository.cart.value
        openCheckout()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_DELIVERY).performClick()
        composeTestRule.onNodeWithText(
            "Entrega simulada. Nenhum operador logístico será accionado."
        ).assertIsDisplayed()
        continueCheckout()
        input(NavigationTestTags.CHECKOUT_CUSTOMER_NAME, "Berta Teste")
        input(NavigationTestTags.CHECKOUT_CUSTOMER_PHONE, "+244 924 000 222")
        continueCheckout()

        input(NavigationTestTags.CHECKOUT_DELIVERY_PROVINCE, "Luanda")
        input(NavigationTestTags.CHECKOUT_DELIVERY_MUNICIPALITY, "Luanda")
        input(NavigationTestTags.CHECKOUT_DELIVERY_AREA, "Maianga")
        input(NavigationTestTags.CHECKOUT_DELIVERY_STREET, "Rua de Teste")
        input(NavigationTestTags.CHECKOUT_DELIVERY_BUILDING, "Casa azul")
        input(NavigationTestTags.CHECKOUT_DELIVERY_REFERENCE, "Próximo ao banco de teste")
        input(NavigationTestTags.CHECKOUT_DELIVERY_INSTRUCTIONS, "Ligar ao chegar")
        closeSoftKeyboard()
        scrollCheckoutTo(NavigationTestTags.CHECKOUT_DELIVERY_INSTRUCTIONS)
        composeTestRule.onNodeWithText("15 de 300 caracteres").assertIsDisplayed()
        continueCheckout()

        waitForTag(NavigationTestTags.CHECKOUT_REVIEW)
        val quote = checkoutRepository.session.value?.quote
        assertNotNull(quote)
        assertEquals(1, quote?.charges?.size)
        assertEquals(600_000L, quote?.total?.amountMinor)
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CHARGES).assertExists()
        scrollCheckoutTo(NavigationTestTags.CHECKOUT_CONFIRM)
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CONFIRM).performClick()
        waitForTag(NavigationTestTags.CHECKOUT_CONFIRMATION)
        composeTestRule.onNodeWithText("Entrega simulada").assertIsDisplayed()
        composeTestRule.onNodeWithText("tracking", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("mapa", substring = true).assertDoesNotExist()
        assertEquals(FulfillmentMethod.DELIVERY_MOCK, checkoutRepository.draft.value?.fulfillment?.method)
        assertEquals(cartBefore, cartRepository.cart.value)
    }

    @Test fun invalid_customer_and_cart_change_are_recoverable_without_silent_confirmation() {
        openCheckout()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_PICKUP).performClick()
        continueCheckout()
        continueCheckout()
        composeTestRule.onNodeWithText("Informe o nome.").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Informe um telefone válido com 9 a 15 dígitos."
        ).assertIsDisplayed()
        input(NavigationTestTags.CHECKOUT_CUSTOMER_NAME, "Ca")
        input(NavigationTestTags.CHECKOUT_CUSTOMER_PHONE, "923000333")
        continueCheckout()
        val item = cartRepository.cart.value.items.single()
        runBlocking {
            cartRepository.updateQuantity(UpdateCartItemQuantityCommand(item.id, 2))
        }
        continueCheckout()
        waitForTag(NavigationTestTags.CHECKOUT_CONFLICT)
        composeTestRule.onNodeWithText(
            "O carrinho foi alterado desde o início do Checkout."
        ).assertIsDisplayed()
        assertEquals(null, checkoutRepository.draft.value)
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_RESTART).performClick()
        waitForTag(NavigationTestTags.CHECKOUT_FULFILLMENT)
        assertEquals(2, checkoutRepository.session.value?.cartItemCount)
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_PICKUP).performClick()
        continueCheckout()
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CUSTOMER_NAME)
            .assertTextContains("Ca")
    }

    private fun openCheckout() {
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_BADGE).performClick()
        waitForTag(NavigationTestTags.CART)
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_LIST).performScrollToNode(
            hasTestTag(NavigationTestTags.CART_CONTINUE)
        )
        composeTestRule.onNodeWithTag(NavigationTestTags.CART_CONTINUE).performClick()
        waitForTag(NavigationTestTags.CHECKOUT_FULFILLMENT)
    }

    private fun continueCheckout() {
        scrollCheckoutTo(NavigationTestTags.CHECKOUT_CONTINUE)
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_CONTINUE).performClick()
        composeTestRule.waitForIdle()
    }

    private fun input(tag: String, value: String) {
        scrollCheckoutTo(tag)
        composeTestRule.onNodeWithTag(tag).performTextInput(value)
    }

    private fun scrollCheckoutTo(tag: String) {
        composeTestRule.onNodeWithTag(NavigationTestTags.CHECKOUT_LIST).performScrollToNode(hasTestTag(tag))
    }

    private fun waitForTag(tag: String) {
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun pressActivityBack() {
        composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()
    }

    private fun cartCommand(): AddCartItemCommand {
        val price = MoneyAmount(450_000, "AOA")
        return AddCartItemCommand(
            merchant = CartMerchant("sabor-maianga", "Sabor da Maianga"),
            configuredProduct = ConfiguredProduct(
                merchantId = "sabor-maianga",
                productId = "checkout-test-product",
                quantity = 1,
                selectedOptionIds = emptyMap(),
                note = null,
                unitPrice = price,
                optionsPrice = MoneyAmount(0, "AOA"),
                totalUnitPrice = price,
                totalPrice = price
            ),
            snapshot = ProductCartSnapshot("Produto de teste", null, emptyList())
        )
    }
}
