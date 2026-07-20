package ao.consuma.aqui.feature.checkout.domain.repository

import ao.consuma.aqui.feature.checkout.domain.command.ConfirmCheckoutCommand
import ao.consuma.aqui.feature.checkout.domain.command.RequestQuoteCommand
import ao.consuma.aqui.feature.checkout.domain.command.SelectFulfillmentCommand
import ao.consuma.aqui.feature.checkout.domain.command.StartCheckoutCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdateCustomerCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdateDeliveryAddressCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdatePickupDetailsCommand
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutDraft
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutResult
import kotlinx.coroutines.flow.StateFlow

interface CheckoutRepository {
    val session: StateFlow<CheckoutSession?>
    val draft: StateFlow<CheckoutDraft?>

    suspend fun startCheckout(command: StartCheckoutCommand = StartCheckoutCommand()): CheckoutResult<CheckoutSession>
    suspend fun updateCustomer(command: UpdateCustomerCommand): CheckoutResult<CheckoutSession>
    suspend fun selectFulfillment(command: SelectFulfillmentCommand): CheckoutResult<CheckoutSession>
    suspend fun updateDeliveryAddress(command: UpdateDeliveryAddressCommand): CheckoutResult<CheckoutSession>
    suspend fun updatePickupDetails(command: UpdatePickupDetailsCommand): CheckoutResult<CheckoutSession>
    suspend fun requestQuote(command: RequestQuoteCommand): CheckoutResult<CheckoutSession>
    suspend fun confirmCheckout(command: ConfirmCheckoutCommand): CheckoutResult<CheckoutDraft>
    suspend fun resetCheckout(): CheckoutResult<Unit>
}
