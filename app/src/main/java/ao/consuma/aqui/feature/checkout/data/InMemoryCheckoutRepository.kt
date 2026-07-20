package ao.consuma.aqui.feature.checkout.data

import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.checkout.domain.command.ConfirmCheckoutCommand
import ao.consuma.aqui.feature.checkout.domain.command.MockCheckoutScenario
import ao.consuma.aqui.feature.checkout.domain.command.RequestQuoteCommand
import ao.consuma.aqui.feature.checkout.domain.command.SelectFulfillmentCommand
import ao.consuma.aqui.feature.checkout.domain.command.StartCheckoutCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdateCustomerCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdateDeliveryAddressCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdatePickupDetailsCommand
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutDraft
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.repository.CheckoutRepository
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutConflict
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutResult
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutCustomerValidator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutDraftFactory
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutEntryValidationResult
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutEntryValidator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutFulfillmentValidator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutIdGenerator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutQuoteCalculator
import ao.consuma.aqui.feature.checkout.domain.service.CustomerValidationResult
import ao.consuma.aqui.feature.checkout.domain.service.FulfillmentValidationResult
import ao.consuma.aqui.feature.checkout.domain.service.QuoteCalculationResult
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-local checkout source of truth. Customer, address, session and draft data are never
 * persisted or sent. [resetCheckout] atomically discards all checkout-owned personal data.
 */
@Singleton
class InMemoryCheckoutRepository @Inject constructor(
    private val cartRepository: CartRepository,
    private val entryValidator: CheckoutEntryValidator,
    private val customerValidator: CheckoutCustomerValidator,
    private val fulfillmentValidator: CheckoutFulfillmentValidator,
    private val quoteCalculator: CheckoutQuoteCalculator,
    private val draftFactory: CheckoutDraftFactory,
    private val capabilitiesProvider: CheckoutCapabilitiesProvider,
    private val idGenerator: CheckoutIdGenerator
) : CheckoutRepository {
    private val mutex = Mutex()
    private val mutableSession = MutableStateFlow<CheckoutSession?>(null)
    private val mutableDraft = MutableStateFlow<CheckoutDraft?>(null)
    private var scenario: MockCheckoutScenario = MockCheckoutScenario.SUCCESS

    override val session: StateFlow<CheckoutSession?> = mutableSession.asStateFlow()
    override val draft: StateFlow<CheckoutDraft?> = mutableDraft.asStateFlow()

    override suspend fun startCheckout(command: StartCheckoutCommand): CheckoutResult<CheckoutSession> =
        mutex.withLock {
            val cart = cartRepository.cart.value
            command.expectedCartId?.takeIf { it != cart.id }?.let {
                return@withLock changedConflict(it, command.expectedCartVersion ?: cart.version, cart)
            }
            command.expectedCartVersion?.takeIf { it != cart.version }?.let {
                return@withLock changedConflict(command.expectedCartId ?: cart.id, it, cart)
            }
            val snapshot = when (val result = entryValidator.validate(cart)) {
                is CheckoutEntryValidationResult.Valid -> result.snapshot
                CheckoutEntryValidationResult.EmptyCart -> return@withLock CheckoutResult.Failure(CheckoutError.EmptyCart)
                CheckoutEntryValidationResult.CurrencyInconsistent ->
                    return@withLock CheckoutResult.Failure(CheckoutError.CurrencyMismatch)
                CheckoutEntryValidationResult.InvalidSubtotal ->
                    return@withLock CheckoutResult.Failure(CheckoutError.InvalidCart)
                CheckoutEntryValidationResult.InvalidItem,
                CheckoutEntryValidationResult.InvalidMerchant,
                CheckoutEntryValidationResult.InvalidVersion ->
                    return@withLock CheckoutResult.Failure(CheckoutError.InvalidCart)
            }
            if (command.scenario == MockCheckoutScenario.UNKNOWN_ERROR) {
                return@withLock CheckoutResult.Failure(CheckoutError.Unknown)
            }
            val capabilities = if (command.scenario == MockCheckoutScenario.FULFILLMENT_UNAVAILABLE) {
                ao.consuma.aqui.feature.checkout.domain.model.CheckoutCapabilities(false, false)
            } else {
                capabilitiesProvider.forMerchant(snapshot.merchantId)
            }
            val created = CheckoutSession(
                id = idGenerator.createId(),
                cartId = snapshot.cartId,
                cartVersion = snapshot.cartVersion,
                cartItemCount = snapshot.itemCount,
                cartSubtotal = snapshot.subtotal,
                currencyCode = snapshot.currencyCode,
                merchantId = snapshot.merchantId,
                merchantName = snapshot.merchantName,
                items = Collections.unmodifiableList(snapshot.items.toList()),
                capabilities = capabilities,
                selectedFulfillmentMethod = null,
                customer = null,
                fulfillment = null,
                quote = null,
                status = CheckoutSessionStatus.Started,
                version = 0
            )
            scenario = command.scenario
            mutableDraft.value = null
            mutableSession.value = created
            CheckoutResult.Success(created)
        }

    override suspend fun updateCustomer(command: UpdateCustomerCommand): CheckoutResult<CheckoutSession> =
        mutex.withLock {
            val current = active(command.sessionId) ?: return@withLock missingOrConfirmed(command.sessionId)
            val customer = when (val result = customerValidator.validate(
                command.fullName,
                command.phone,
                command.email
            )) {
                is CustomerValidationResult.Valid -> result.customer
                is CustomerValidationResult.Invalid -> return@withLock CheckoutResult.Failure(result.error)
            }
            if (customer == current.customer) return@withLock CheckoutResult.Success(current)
            update(current.copy(customer = customer, version = nextVersion(current)))
        }

    override suspend fun selectFulfillment(
        command: SelectFulfillmentCommand
    ): CheckoutResult<CheckoutSession> = mutex.withLock {
        val current = active(command.sessionId) ?: return@withLock missingOrConfirmed(command.sessionId)
        if (!fulfillmentValidator.isAvailable(command.method, current.capabilities)) {
            return@withLock CheckoutResult.Failure(CheckoutError.FulfillmentUnavailable)
        }
        if (current.selectedFulfillmentMethod == command.method) {
            return@withLock CheckoutResult.Success(current)
        }
        update(
            current.copy(
                selectedFulfillmentMethod = command.method,
                fulfillment = null,
                quote = null,
                status = if (current.customer == null) {
                    CheckoutSessionStatus.CustomerPending
                } else {
                    CheckoutSessionStatus.FulfillmentPending
                },
                version = nextVersion(current)
            )
        )
    }

    override suspend fun updateDeliveryAddress(
        command: UpdateDeliveryAddressCommand
    ): CheckoutResult<CheckoutSession> = mutex.withLock {
        val current = active(command.sessionId) ?: return@withLock missingOrConfirmed(command.sessionId)
        if (current.selectedFulfillmentMethod != FulfillmentMethod.DELIVERY_MOCK) {
            return@withLock CheckoutResult.Failure(CheckoutError.InvalidState)
        }
        val fulfillment = when (val result = fulfillmentValidator.validateDelivery(
            current.capabilities,
            command.address,
            command.recipientName,
            command.recipientPhone,
            command.instructions
        )) {
            is FulfillmentValidationResult.Valid -> result.fulfillment
            is FulfillmentValidationResult.Invalid -> return@withLock CheckoutResult.Failure(result.error)
        }
        if (current.fulfillment == fulfillment) return@withLock CheckoutResult.Success(current)
        update(
            current.copy(
                fulfillment = fulfillment,
                quote = null,
                status = if (current.customer == null) {
                    CheckoutSessionStatus.CustomerPending
                } else {
                    CheckoutSessionStatus.QuotePending
                },
                version = nextVersion(current)
            )
        )
    }

    override suspend fun updatePickupDetails(
        command: UpdatePickupDetailsCommand
    ): CheckoutResult<CheckoutSession> = mutex.withLock {
        val current = active(command.sessionId) ?: return@withLock missingOrConfirmed(command.sessionId)
        if (current.selectedFulfillmentMethod != FulfillmentMethod.PICKUP) {
            return@withLock CheckoutResult.Failure(CheckoutError.InvalidState)
        }
        val fulfillment = when (val result = fulfillmentValidator.validatePickup(
            current.capabilities,
            command.contactName,
            command.contactPhone,
            command.desiredTime
        )) {
            is FulfillmentValidationResult.Valid -> result.fulfillment
            is FulfillmentValidationResult.Invalid -> return@withLock CheckoutResult.Failure(result.error)
        }
        if (current.fulfillment == fulfillment) return@withLock CheckoutResult.Success(current)
        update(
            current.copy(
                fulfillment = fulfillment,
                quote = null,
                status = if (current.customer == null) {
                    CheckoutSessionStatus.CustomerPending
                } else {
                    CheckoutSessionStatus.QuotePending
                },
                version = nextVersion(current)
            )
        )
    }

    override suspend fun requestQuote(command: RequestQuoteCommand): CheckoutResult<CheckoutSession> =
        mutex.withLock {
            val current = active(command.sessionId) ?: return@withLock missingOrConfirmed(command.sessionId)
            expectedConflict(current, command.expectedCartId, command.expectedCartVersion)?.let {
                return@withLock CheckoutResult.Conflict(it)
            }
            currentCartConflict(current)?.let { return@withLock CheckoutResult.Conflict(it) }
            if (current.customer == null || current.fulfillment == null) {
                return@withLock CheckoutResult.Failure(CheckoutError.InvalidState)
            }
            if (scenario == MockCheckoutScenario.QUOTE_ERROR) {
                return@withLock CheckoutResult.Failure(CheckoutError.QuoteUnavailable)
            }
            if (scenario == MockCheckoutScenario.CART_CHANGED) {
                return@withLock fixtureCartChanged(current)
            }
            val quote = when (val result = quoteCalculator.calculate(
                current,
                expiredFixture = scenario == MockCheckoutScenario.QUOTE_EXPIRED
            )) {
                is QuoteCalculationResult.Success -> result.quote
                is QuoteCalculationResult.Failure -> return@withLock CheckoutResult.Failure(result.error)
            }
            update(current.copy(quote = quote, version = nextVersion(current)))
        }

    override suspend fun confirmCheckout(command: ConfirmCheckoutCommand): CheckoutResult<CheckoutDraft> =
        mutex.withLock {
            val current = mutableSession.value
                ?: return@withLock CheckoutResult.Failure(CheckoutError.SessionNotFound)
            if (current.id != command.sessionId) {
                return@withLock CheckoutResult.Failure(CheckoutError.SessionNotFound)
            }
            if (current.status == CheckoutSessionStatus.ConfirmedMock) {
                val draft = mutableDraft.value
                    ?: return@withLock CheckoutResult.Failure(CheckoutError.SessionAlreadyConfirmed)
                return@withLock if (current.cartId == command.expectedCartId &&
                    current.cartVersion == command.expectedCartVersion
                ) CheckoutResult.Success(draft) else CheckoutResult.Conflict(
                    CheckoutConflict.CartChanged(
                        current.cartId,
                        command.expectedCartId,
                        current.cartVersion,
                        command.expectedCartVersion
                    )
                )
            }
            expectedConflict(current, command.expectedCartId, command.expectedCartVersion)?.let {
                return@withLock CheckoutResult.Conflict(it)
            }
            currentCartConflict(current)?.let { return@withLock CheckoutResult.Conflict(it) }
            if (scenario == MockCheckoutScenario.CART_CHANGED) {
                return@withLock fixtureCartChanged(current)
            }
            val draft = when (val result = draftFactory.create(
                current,
                command.expectedCartId,
                command.expectedCartVersion
            )) {
                is CheckoutResult.Success -> result.data
                is CheckoutResult.Failure -> return@withLock result
                is CheckoutResult.Conflict -> return@withLock result
            }
            // A second read defines the linearization point against Cart changes during creation.
            currentCartConflict(current)?.let { return@withLock CheckoutResult.Conflict(it) }
            mutableDraft.value = draft
            mutableSession.value = current.copy(
                status = CheckoutSessionStatus.ConfirmedMock,
                version = nextVersion(current)
            )
            CheckoutResult.Success(draft)
        }

    override suspend fun resetCheckout(): CheckoutResult<Unit> = mutex.withLock {
        mutableSession.value = null
        mutableDraft.value = null
        scenario = MockCheckoutScenario.SUCCESS
        CheckoutResult.Success(Unit)
    }

    private fun active(sessionId: String): CheckoutSession? = mutableSession.value?.takeIf {
        it.id == sessionId && it.status != CheckoutSessionStatus.ConfirmedMock
    }

    private fun missingOrConfirmed(sessionId: String): CheckoutResult.Failure =
        if (mutableSession.value?.id == sessionId &&
            mutableSession.value?.status == CheckoutSessionStatus.ConfirmedMock
        ) CheckoutResult.Failure(CheckoutError.SessionAlreadyConfirmed)
        else CheckoutResult.Failure(CheckoutError.SessionNotFound)

    private fun update(candidate: CheckoutSession): CheckoutResult<CheckoutSession> {
        val resolved = candidate.copy(status = statusFor(candidate))
        mutableSession.value = resolved
        return CheckoutResult.Success(resolved)
    }

    private fun statusFor(session: CheckoutSession): CheckoutSessionStatus = when {
        session.selectedFulfillmentMethod == null && session.customer == null -> CheckoutSessionStatus.Started
        session.selectedFulfillmentMethod == null -> CheckoutSessionStatus.FulfillmentPending
        session.customer == null -> CheckoutSessionStatus.CustomerPending
        session.fulfillment == null -> CheckoutSessionStatus.FulfillmentPending
        session.quote == null -> CheckoutSessionStatus.QuotePending
        else -> CheckoutSessionStatus.ReadyForReview
    }

    private fun nextVersion(session: CheckoutSession): Long = try {
        Math.addExact(session.version, 1L)
    } catch (_: ArithmeticException) {
        throw IllegalStateException("Checkout session version exhausted")
    }

    private fun expectedConflict(
        session: CheckoutSession,
        expectedCartId: String,
        expectedVersion: Long
    ): CheckoutConflict? = if (session.cartId != expectedCartId || session.cartVersion != expectedVersion) {
        CheckoutConflict.CartChanged(
            session.cartId,
            expectedCartId,
            session.cartVersion,
            expectedVersion
        )
    } else null

    private fun currentCartConflict(session: CheckoutSession): CheckoutConflict? {
        val cart = cartRepository.cart.value
        if (cart.items.isEmpty()) return CheckoutConflict.CartCleared(session.cartId, session.cartVersion)
        if (cart.merchant?.id != session.merchantId) {
            return CheckoutConflict.MerchantChanged(session.merchantId, cart.merchant?.id)
        }
        if (cart.totals.subtotal.currencyCode != session.currencyCode) {
            return CheckoutConflict.CurrencyChanged(session.currencyCode, cart.totals.subtotal.currencyCode)
        }
        if (cart.id != session.cartId) {
            return CheckoutConflict.CartChanged(session.cartId, cart.id, session.cartVersion, cart.version)
        }
        if (cart.version != session.cartVersion || cart.totals.itemCount != session.cartItemCount ||
            cart.totals.subtotal != session.cartSubtotal
        ) {
            return CheckoutConflict.CartChanged(session.cartId, cart.id, session.cartVersion, cart.version)
        }
        return null
    }

    private fun changedConflict(
        initialId: String,
        initialVersion: Long,
        current: Cart
    ) = CheckoutResult.Conflict(
        CheckoutConflict.CartChanged(initialId, current.id, initialVersion, current.version)
    )

    private fun fixtureCartChanged(session: CheckoutSession): CheckoutResult.Conflict =
        CheckoutResult.Conflict(
            CheckoutConflict.CartChanged(
                session.cartId,
                session.cartId,
                session.cartVersion,
                if (session.cartVersion == Long.MAX_VALUE) session.cartVersion else session.cartVersion + 1
            )
        )
}
