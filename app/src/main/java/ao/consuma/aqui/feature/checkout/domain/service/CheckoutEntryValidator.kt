package ao.consuma.aqui.feature.checkout.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.domain.model.Cart
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutItem
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutItemSelection
import java.util.Collections
import javax.inject.Inject

data class CheckoutEntrySnapshot(
    val cartId: String,
    val cartVersion: Long,
    val itemCount: Int,
    val merchantId: String,
    val merchantName: String,
    val items: List<CheckoutItem>,
    val subtotal: MoneyAmount,
    val currencyCode: String
)

sealed interface CheckoutEntryValidationResult {
    data class Valid(val snapshot: CheckoutEntrySnapshot) : CheckoutEntryValidationResult
    data object EmptyCart : CheckoutEntryValidationResult
    data object CurrencyInconsistent : CheckoutEntryValidationResult
    data object InvalidItem : CheckoutEntryValidationResult
    data object InvalidSubtotal : CheckoutEntryValidationResult
    data object InvalidMerchant : CheckoutEntryValidationResult
    data object InvalidVersion : CheckoutEntryValidationResult
}

class CheckoutEntryValidator @Inject constructor() {
    fun validate(cart: Cart): CheckoutEntryValidationResult {
        if (cart.version < 0) return CheckoutEntryValidationResult.InvalidVersion
        if (cart.items.isEmpty()) return CheckoutEntryValidationResult.EmptyCart
        val merchant = cart.merchant ?: return CheckoutEntryValidationResult.InvalidMerchant
        if (merchant.id.isBlank() || merchant.name.isBlank()) return CheckoutEntryValidationResult.InvalidMerchant
        if (cart.items.any { it.merchantId != merchant.id }) return CheckoutEntryValidationResult.InvalidMerchant

        val currency = cart.items.first().totalPrice.currencyCode
        if (cart.items.any { item ->
                item.totalPrice.currencyCode != currency ||
                    item.totalUnitPrice.currencyCode != currency ||
                    item.selections.any { it.additionalPrice?.currencyCode !in setOf(null, currency) }
            }
        ) return CheckoutEntryValidationResult.CurrencyInconsistent

        val checkoutItems = try {
            cart.items.map { item ->
                if (item.id.isBlank() || item.productId.isBlank() || item.quantity <= 0 ||
                    item.configurationFingerprint.isBlank()
                ) return CheckoutEntryValidationResult.InvalidItem
                CheckoutItem(
                    cartItemId = item.id,
                    merchantId = item.merchantId,
                    productId = item.productId,
                    productName = item.productName,
                    quantity = item.quantity,
                    selections = Collections.unmodifiableList(item.selections.map { selection ->
                        CheckoutItemSelection(
                            selection.groupId,
                            selection.groupName,
                            selection.optionId,
                            selection.optionName,
                            selection.additionalPrice
                        )
                    }),
                    note = item.note,
                    unitPrice = item.totalUnitPrice,
                    totalPrice = item.totalPrice,
                    configurationFingerprint = item.configurationFingerprint
                )
            }
        } catch (_: IllegalArgumentException) {
            return CheckoutEntryValidationResult.InvalidItem
        } catch (_: ArithmeticException) {
            return CheckoutEntryValidationResult.InvalidSubtotal
        }
        val subtotal = try {
            checkoutItems.fold(MoneyAmount(0, currency)) { sum, item -> sum.add(item.totalPrice) }
        } catch (_: ArithmeticException) {
            return CheckoutEntryValidationResult.InvalidSubtotal
        }
        if (subtotal != cart.totals.subtotal || cart.totals.itemCount != checkoutItems.sumOf { it.quantity }) {
            return CheckoutEntryValidationResult.InvalidSubtotal
        }
        return CheckoutEntryValidationResult.Valid(
            CheckoutEntrySnapshot(
                cart.id,
                cart.version,
                cart.totals.itemCount,
                merchant.id,
                merchant.name,
                Collections.unmodifiableList(checkoutItems.toList()),
                subtotal,
                currency
            )
        )
    }
}
