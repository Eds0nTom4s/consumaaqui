package ao.consuma.aqui.feature.checkout

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.data.InMemoryCartRepository
import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.cart.domain.model.CartMerchant
import ao.consuma.aqui.feature.cart.domain.model.CartTotals
import ao.consuma.aqui.feature.cart.domain.service.CartItemFactory
import ao.consuma.aqui.feature.cart.domain.service.CartItemIdentityFactory
import ao.consuma.aqui.feature.cart.domain.service.CartMerchantPolicy
import ao.consuma.aqui.feature.cart.domain.service.CartTotalsCalculator
import ao.consuma.aqui.feature.cart.cartItem
import ao.consuma.aqui.feature.checkout.data.CheckoutCapabilitiesProvider
import ao.consuma.aqui.feature.checkout.data.InMemoryCheckoutRepository
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutCapabilities
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutCustomer
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutContact
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutItem
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutItemSelection
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutQuote
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.model.PickupDetails
import ao.consuma.aqui.feature.checkout.domain.model.PickupTimePreference
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutAddressValidator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutCustomerValidator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutDraftFactory
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutEntryValidator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutFulfillmentValidator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutIdGenerator
import ao.consuma.aqui.feature.checkout.domain.service.CheckoutQuoteCalculator
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

internal val fixedInstant: Instant = Instant.parse("2026-07-19T10:00:00Z")
internal val fixedClock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

internal class SequentialCheckoutIdGenerator : CheckoutIdGenerator {
    private var value = 0
    override fun createId(): String = "checkout-id-${++value}"
}

internal fun validCart(version: Long = 4): Cart {
    val item = cartItem(merchantId = "sabor-maianga")
    return Cart(
        id = "cart-one",
        merchant = CartMerchant("sabor-maianga", "Sabor da Maianga"),
        items = listOf(item),
        totals = CartTotals(1, 1, item.totalPrice),
        version = version
    )
}

internal fun checkoutItem(
    amountMinor: Long = 1_200,
    quantity: Int = 1,
    currency: String = "AOA"
) = CheckoutItem(
    cartItemId = "item-one",
    merchantId = "sabor-maianga",
    productId = "product-one",
    productName = "Produto Um",
    quantity = quantity,
    selections = listOf(
        CheckoutItemSelection("size", "Tamanho", "large", "Grande", MoneyAmount(200, currency))
    ),
    note = null,
    unitPrice = MoneyAmount(amountMinor, currency),
    totalPrice = MoneyAmount(amountMinor, currency).multiply(quantity),
    configurationFingerprint = "fingerprint-one"
)

internal fun readySession(
    quote: CheckoutQuote? = null,
    status: CheckoutSessionStatus = if (quote == null) CheckoutSessionStatus.QuotePending
        else CheckoutSessionStatus.ReadyForReview,
    subtotalMinor: Long = 1_200
): CheckoutSession = CheckoutSession(
    id = "session-one",
    cartId = "cart-one",
    cartVersion = 4,
    cartItemCount = 1,
    cartSubtotal = MoneyAmount(subtotalMinor, "AOA"),
    currencyCode = "AOA",
    merchantId = "sabor-maianga",
    merchantName = "Sabor da Maianga",
    items = listOf(checkoutItem(amountMinor = subtotalMinor)),
    capabilities = CheckoutCapabilities(true, true),
    selectedFulfillmentMethod = FulfillmentMethod.PICKUP,
    customer = CheckoutCustomer("Ana Silva", CheckoutContact("+244923456789", "ana@example.com")),
    fulfillment = CheckoutFulfillment.Pickup(
        PickupDetails("Ana Silva", "+244923456789", PickupTimePreference.AsSoonAsPossible)
    ),
    quote = quote,
    status = status,
    version = 3
)

internal fun calculator(ids: CheckoutIdGenerator = SequentialCheckoutIdGenerator()) =
    CheckoutQuoteCalculator(fixedClock, ids)

internal fun repositoryFixture(): Pair<InMemoryCheckoutRepository, InMemoryCartRepository> {
    val cartIdentity = CartItemIdentityFactory()
    val cart = InMemoryCartRepository(
        CartItemFactory(cartIdentity),
        cartIdentity,
        CartTotalsCalculator(),
        CartMerchantPolicy()
    )
    val customer = CheckoutCustomerValidator()
    val address = CheckoutAddressValidator()
    val fulfillment = CheckoutFulfillmentValidator(customer, address)
    val ids = SequentialCheckoutIdGenerator()
    val checkout = InMemoryCheckoutRepository(
        cart,
        CheckoutEntryValidator(),
        customer,
        fulfillment,
        CheckoutQuoteCalculator(fixedClock, ids),
        CheckoutDraftFactory(fixedClock, ids, customer),
        CheckoutCapabilitiesProvider(),
        ids
    )
    return checkout to cart
}
