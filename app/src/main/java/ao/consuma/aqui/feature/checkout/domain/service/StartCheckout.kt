package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.checkout.domain.command.MockCheckoutScenario
import ao.consuma.aqui.feature.checkout.domain.command.StartCheckoutCommand
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.repository.CheckoutRepository
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutResult
import javax.inject.Inject

/** UI-facing entry use case; checkout snapshot construction remains outside presentation. */
class StartCheckout @Inject constructor(
    private val repository: CheckoutRepository,
    private val cartRepository: CartRepository
) {
    /**
     * Reuses an unconfirmed session only while its cart identity and version still match. Confirmed
     * or stale sessions are discarded before a fresh snapshot is created; form text remains a
     * presentation concern and may be restored independently.
     */
    suspend operator fun invoke(
        scenario: MockCheckoutScenario = MockCheckoutScenario.SUCCESS
    ): CheckoutResult<ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession> {
        val cart = cartRepository.cart.value
        val active = repository.session.value
        if (active != null && active.status != CheckoutSessionStatus.ConfirmedMock &&
            active.cartId == cart.id && active.cartVersion == cart.version
        ) return CheckoutResult.Success(active)

        if (active != null) repository.resetCheckout()
        return repository.startCheckout(
            StartCheckoutCommand(
                expectedCartId = cart.id,
                expectedCartVersion = cart.version,
                scenario = scenario
            )
        )
    }
}
