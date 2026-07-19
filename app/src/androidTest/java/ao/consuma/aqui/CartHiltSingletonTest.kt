package ao.consuma.aqui

import androidx.test.ext.junit.runners.AndroidJUnit4
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CartHiltSingletonTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var first: CartRepository
    @Inject lateinit var second: CartRepository

    @Test fun cart_repository_binding_is_process_singleton_and_shared() = runBlocking {
        hiltRule.inject()
        assertSame(first, second)
        first.clearCart()
        assertEquals(first.cart.value, second.cart.value)
    }
}
