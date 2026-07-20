package ao.consuma.aqui.feature.checkout

import ao.consuma.aqui.core.navigation.AppDestination
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutDraft
import ao.consuma.aqui.feature.checkout.presentation.checkout.CheckoutUiEffect
import java.io.Serializable
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutArchitecturePrivacyTest {
    @Test fun `checkout routes are fixed argument-free and carry no personal data`() {
        val forbidden = listOf("name", "phone", "email", "address", "instructions", "customer", "draft")
        listOf(AppDestination.Checkout.route, AppDestination.CheckoutConfirmation.route).forEach { route ->
            assertFalse(route.contains('{'))
            assertFalse(route.contains('?'))
            assertTrue(forbidden.none { route.contains(it, ignoreCase = true) })
        }
    }

    @Test fun `navigation effects carry neither customer nor checkout draft`() {
        listOf(
            CheckoutUiEffect.NavigateToCart::class.java,
            CheckoutUiEffect.NavigateToConfirmation::class.java
        ).forEach { type ->
            assertTrue(type.declaredFields.all {
                it.name == "INSTANCE" || it.name == "\$stable" || it.isSynthetic
            })
        }
    }

    @Test fun `checkout test tags are compile-time constants without personal values`() {
        val values = NavigationTestTags::class.java.declaredFields
            .filter { it.name.startsWith("CHECKOUT") }
            .mapNotNull { it.get(null) as? String }
        assertTrue(values.isNotEmpty())
        assertTrue(values.all { it.startsWith("checkout") })
        assertTrue(values.none { '@' in it || '+' in it || ' ' in it })
    }

    @Test fun `cart contract has no checkout dependency`() {
        val signatures = CartRepository::class.java.declaredMethods.flatMap { method ->
            listOf(method.returnType.name) + method.parameterTypes.map { it.name }
        }
        assertTrue(signatures.none { it.contains(".checkout.") })
    }

    @Test fun `draft is transient and not an Android or Java serialization payload`() {
        assertFalse(Serializable::class.java.isAssignableFrom(CheckoutDraft::class.java))
        assertTrue(CheckoutDraft::class.java.interfaces.none {
            it.name == "android.os.Parcelable"
        })
    }
}
