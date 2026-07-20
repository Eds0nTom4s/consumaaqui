package ao.consuma.aqui

import androidx.test.ext.junit.runners.AndroidJUnit4
import ao.consuma.aqui.feature.checkout.domain.repository.CheckoutRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CheckoutHiltSingletonTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var first: CheckoutRepository
    @Inject lateinit var second: CheckoutRepository

    @Test fun checkout_repository_binding_is_process_singleton_and_reset_is_shared() = runBlocking {
        hiltRule.inject()
        assertSame(first, second)
        first.resetCheckout()
        assertNull(second.session.value)
        assertNull(second.draft.value)
    }
}
