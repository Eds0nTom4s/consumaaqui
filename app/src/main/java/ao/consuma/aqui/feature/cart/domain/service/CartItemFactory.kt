package ao.consuma.aqui.feature.cart.domain.service

import ao.consuma.aqui.core.domain.model.MoneyAmount
import ao.consuma.aqui.feature.cart.domain.command.AddCartItemCommand
import ao.consuma.aqui.feature.cart.domain.model.CartItem
import ao.consuma.aqui.feature.cart.domain.model.CartItemSelection
import ao.consuma.aqui.feature.cart.domain.result.CartError
import java.util.Collections
import javax.inject.Inject

sealed interface CartItemFactoryResult {
    data class Success(val item: CartItem) : CartItemFactoryResult
    data class Failure(val error: CartError) : CartItemFactoryResult
}

class CartItemFactory @Inject constructor(
    private val identityFactory: CartItemIdentityFactory
) {
    fun create(command: AddCartItemCommand): CartItemFactoryResult {
        val configured = command.configuredProduct
        if (configured.merchantId != command.merchant.id) {
            return CartItemFactoryResult.Failure(CartError.MerchantMismatch)
        }
        if (command.snapshot.productName.isBlank()) {
            return CartItemFactoryResult.Failure(CartError.InvalidItem)
        }
        val selections = command.snapshot.selectedOptions.map {
            runCatching {
                CartItemSelection(
                    groupId = it.groupId,
                    groupName = it.groupName,
                    optionId = it.optionId,
                    optionName = it.optionName,
                    additionalPrice = it.additionalPrice
                )
            }.getOrElse { return CartItemFactoryResult.Failure(CartError.InvalidItem) }
        }.sortedWith(compareBy(CartItemSelection::groupId, CartItemSelection::optionId))

        val configuredSelections = configured.selectedOptionIds
            .flatMap { (groupId, options) -> options.map { groupId to it } }
            .toSet()
        val snapshotSelections = selections.map { it.groupId to it.optionId }.toSet()
        if (configuredSelections != snapshotSelections || snapshotSelections.size != selections.size) {
            return CartItemFactoryResult.Failure(CartError.InvalidItem)
        }
        val currency = configured.unitPrice.currencyCode
        if (selections.any {
                it.additionalPrice != null && it.additionalPrice.currencyCode != currency
            }) {
            return CartItemFactoryResult.Failure(CartError.CurrencyMismatch)
        }
        val snapshotOptionsPrice = try {
            selections.mapNotNull(CartItemSelection::additionalPrice)
                .fold(MoneyAmount(0, currency), MoneyAmount::add)
        } catch (_: ArithmeticException) {
            return CartItemFactoryResult.Failure(CartError.PriceOverflow)
        }
        if (snapshotOptionsPrice != configured.optionsPrice) {
            return CartItemFactoryResult.Failure(CartError.InvalidItem)
        }
        val note = configured.note.normalizedNote()
        return try {
            CartItemFactoryResult.Success(
                CartItem(
                    id = identityFactory.createItemId(),
                    merchantId = configured.merchantId,
                    productId = configured.productId,
                    productName = command.snapshot.productName.trim(),
                    productImageUrl = command.snapshot.imageUrl,
                    quantity = configured.quantity,
                    note = note,
                    selections = Collections.unmodifiableList(selections.toList()),
                    unitPrice = configured.unitPrice,
                    optionsPrice = configured.optionsPrice,
                    totalUnitPrice = configured.totalUnitPrice,
                    totalPrice = configured.totalPrice,
                    configurationFingerprint = identityFactory.configurationFingerprint(
                        configured.merchantId,
                        configured.productId,
                        selections,
                        note
                    )
                )
            )
        } catch (_: ArithmeticException) {
            CartItemFactoryResult.Failure(CartError.PriceOverflow)
        } catch (_: IllegalArgumentException) {
            CartItemFactoryResult.Failure(CartError.InvalidItem)
        }
    }
}
