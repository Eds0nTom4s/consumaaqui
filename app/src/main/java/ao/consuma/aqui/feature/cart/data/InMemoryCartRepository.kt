package ao.consuma.aqui.feature.cart.data

import ao.consuma.aqui.feature.cart.domain.command.AddCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.ClearCartCommand
import ao.consuma.aqui.feature.cart.domain.command.RemoveCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.ReplaceCartItemCommand
import ao.consuma.aqui.feature.cart.domain.command.UpdateCartItemQuantityCommand
import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.cart.domain.model.CartItem
import ao.consuma.aqui.feature.cart.domain.model.CartMerchant
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.cart.domain.result.AddCartItemResult
import ao.consuma.aqui.feature.cart.domain.result.CartError
import ao.consuma.aqui.feature.cart.domain.result.CartResult
import ao.consuma.aqui.feature.cart.domain.service.CartItemFactory
import ao.consuma.aqui.feature.cart.domain.service.CartItemFactoryResult
import ao.consuma.aqui.feature.cart.domain.service.CartItemIdentityFactory
import ao.consuma.aqui.feature.cart.domain.service.CartMerchantPolicy
import ao.consuma.aqui.feature.cart.domain.service.CartTotalsCalculationResult
import ao.consuma.aqui.feature.cart.domain.service.CartTotalsCalculator
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-local source of truth for the active cart.
 *
 * Nothing is persisted: terminating the process may discard the complete cart. A future
 * persistent implementation can replace this class through [CartRepository].
 */
@Singleton
class InMemoryCartRepository @Inject constructor(
    private val itemFactory: CartItemFactory,
    private val identityFactory: CartItemIdentityFactory,
    private val totalsCalculator: CartTotalsCalculator,
    private val merchantPolicy: CartMerchantPolicy
) : CartRepository {
    private val mutex = Mutex()
    private val mutableCart = MutableStateFlow(Cart.empty(identityFactory.createCartId()))

    override val cart: StateFlow<Cart> = mutableCart.asStateFlow()

    override suspend fun addItem(command: AddCartItemCommand): AddCartItemResult = mutex.withLock {
        val current = mutableCart.value
        merchantPolicy.conflict(current, command.merchant)?.let {
            return@withLock AddCartItemResult.Conflict(it)
        }
        val incoming = when (val result = itemFactory.create(command)) {
            is CartItemFactoryResult.Success -> result.item
            is CartItemFactoryResult.Failure -> return@withLock AddCartItemResult.Failure(result.error)
        }
        current.items.firstOrNull()?.let { existing ->
            if (existing.totalPrice.currencyCode != incoming.totalPrice.currencyCode) {
                return@withLock AddCartItemResult.Failure(CartError.CurrencyMismatch)
            }
        }

        val existingIndex = current.items.indexOfFirst {
            it.configurationFingerprint == incoming.configurationFingerprint
        }
        val merged = existingIndex >= 0
        val updatedItems = current.items.toMutableList()
        val resultingItem = if (merged) {
            val existing = updatedItems[existingIndex]
            val quantity = existing.quantity + incoming.quantity
            if (quantity > CartItem.MAX_QUANTITY) {
                return@withLock AddCartItemResult.Failure(CartError.QuantityLimitExceeded)
            }
            try {
                existing.copy(
                    quantity = quantity,
                    totalPrice = existing.totalUnitPrice.multiply(quantity)
                )
            } catch (_: ArithmeticException) {
                return@withLock AddCartItemResult.Failure(CartError.PriceOverflow)
            }.also { updatedItems[existingIndex] = it }
        } else {
            incoming.also(updatedItems::add)
        }

        when (val result = createUpdatedCart(
            current = current,
            merchant = current.merchant ?: command.merchant,
            items = updatedItems
        )) {
            is CartResult.Success -> {
                mutableCart.value = result.data
                AddCartItemResult.Added(result.data, resultingItem, merged)
            }
            is CartResult.Failure -> AddCartItemResult.Failure(result.error)
        }
    }

    override suspend fun replaceCartWithItem(
        command: AddCartItemCommand
    ): CartResult<Cart> = mutex.withLock {
        val incoming = when (val result = itemFactory.create(command)) {
            is CartItemFactoryResult.Success -> result.item
            is CartItemFactoryResult.Failure -> return@withLock CartResult.Failure(result.error)
        }
        val current = mutableCart.value
        createUpdatedCart(
            current = current,
            merchant = command.merchant,
            items = listOf(incoming),
            cartId = identityFactory.createCartId()
        ).alsoSuccess { mutableCart.value = it }
    }

    override suspend fun updateQuantity(
        command: UpdateCartItemQuantityCommand
    ): CartResult<Cart> = mutex.withLock {
        if (command.quantity !in CartItem.MIN_QUANTITY..CartItem.MAX_QUANTITY) {
            return@withLock CartResult.Failure(CartError.InvalidQuantity)
        }
        val current = mutableCart.value
        val index = current.items.indexOfFirst { it.id == command.cartItemId }
        if (index < 0) return@withLock CartResult.Failure(CartError.ItemNotFound)
        if (current.items[index].quantity == command.quantity) {
            return@withLock CartResult.Success(current)
        }
        val items = current.items.toMutableList()
        items[index] = try {
            items[index].copy(
                quantity = command.quantity,
                totalPrice = items[index].totalUnitPrice.multiply(command.quantity)
            )
        } catch (_: ArithmeticException) {
            return@withLock CartResult.Failure(CartError.PriceOverflow)
        }
        createUpdatedCart(current, current.merchant, items)
            .alsoSuccess { mutableCart.value = it }
    }

    override suspend fun removeItem(command: RemoveCartItemCommand): CartResult<Cart> = mutex.withLock {
        val current = mutableCart.value
        if (current.items.none { it.id == command.cartItemId }) {
            return@withLock CartResult.Failure(CartError.ItemNotFound)
        }
        val items = current.items.filterNot { it.id == command.cartItemId }
        createUpdatedCart(current, current.merchant.takeIf { items.isNotEmpty() }, items)
            .alsoSuccess { mutableCart.value = it }
    }

    override suspend fun replaceItem(command: ReplaceCartItemCommand): CartResult<Cart> = mutex.withLock {
        val current = mutableCart.value
        val replacedIndex = current.items.indexOfFirst { it.id == command.cartItemId }
        if (replacedIndex < 0) return@withLock CartResult.Failure(CartError.ItemNotFound)
        if (current.merchant?.id != command.merchant.id) {
            return@withLock CartResult.Failure(CartError.MerchantMismatch)
        }
        val incoming = when (val result = itemFactory.create(
            AddCartItemCommand(command.merchant, command.configuredProduct, command.snapshot)
        )) {
            is CartItemFactoryResult.Success -> result.item
            is CartItemFactoryResult.Failure -> return@withLock CartResult.Failure(result.error)
        }
        if (current.items.any { it.totalPrice.currencyCode != incoming.totalPrice.currencyCode }) {
            return@withLock CartResult.Failure(CartError.CurrencyMismatch)
        }

        val mergeIndex = current.items.indexOfFirst {
            it.id != command.cartItemId &&
                it.configurationFingerprint == incoming.configurationFingerprint
        }
        val items = current.items.toMutableList()
        if (mergeIndex >= 0) {
            val target = items[mergeIndex]
            val quantity = target.quantity + incoming.quantity
            if (quantity > CartItem.MAX_QUANTITY) {
                return@withLock CartResult.Failure(CartError.QuantityLimitExceeded)
            }
            items[mergeIndex] = try {
                target.copy(
                    quantity = quantity,
                    totalPrice = target.totalUnitPrice.multiply(quantity)
                )
            } catch (_: ArithmeticException) {
                return@withLock CartResult.Failure(CartError.PriceOverflow)
            }
            items.removeAt(replacedIndex)
        } else {
            val replacement = incoming.copy(id = command.cartItemId)
            if (replacement == current.items[replacedIndex]) {
                return@withLock CartResult.Success(current)
            }
            items[replacedIndex] = replacement
        }
        createUpdatedCart(current, current.merchant, items)
            .alsoSuccess { mutableCart.value = it }
    }

    override suspend fun clearCart(command: ClearCartCommand): CartResult<Cart> = mutex.withLock {
        val current = mutableCart.value
        val version = nextVersion(current) ?: return@withLock CartResult.Failure(CartError.Unknown)
        val cleared = Cart.empty(
            id = identityFactory.createCartId(),
            version = version,
            currencyCode = current.totals.subtotal.currencyCode
        )
        mutableCart.value = cleared
        CartResult.Success(cleared)
    }

    private fun createUpdatedCart(
        current: Cart,
        merchant: CartMerchant?,
        items: List<CartItem>,
        cartId: String = current.id
    ): CartResult<Cart> {
        val version = nextVersion(current) ?: return CartResult.Failure(CartError.Unknown)
        val totals = when (val result = totalsCalculator.calculate(
            items,
            current.totals.subtotal.currencyCode
        )) {
            is CartTotalsCalculationResult.Success -> result.totals
            is CartTotalsCalculationResult.Failure -> return CartResult.Failure(result.error)
        }
        return try {
            CartResult.Success(
                Cart(
                    cartId,
                    merchant,
                    Collections.unmodifiableList(items.toList()),
                    totals,
                    version
                )
            )
        } catch (_: ArithmeticException) {
            CartResult.Failure(CartError.PriceOverflow)
        } catch (_: IllegalArgumentException) {
            CartResult.Failure(CartError.InvalidItem)
        }
    }

    private fun nextVersion(cart: Cart): Long? = try {
        Math.addExact(cart.version, 1L)
    } catch (_: ArithmeticException) {
        null
    }

    private inline fun CartResult<Cart>.alsoSuccess(block: (Cart) -> Unit): CartResult<Cart> =
        also { if (it is CartResult.Success) block(it.data) }
}
