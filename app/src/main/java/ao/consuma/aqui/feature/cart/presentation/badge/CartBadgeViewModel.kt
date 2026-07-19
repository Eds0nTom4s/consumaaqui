package ao.consuma.aqui.feature.cart.presentation.badge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.cart.domain.repository.CartRepository
import ao.consuma.aqui.feature.cart.presentation.mapper.CartBadgeUiState
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiMapper
import ao.consuma.aqui.feature.cart.presentation.mapper.CartUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CartBadgeViewModel @Inject constructor(
    repository: CartRepository,
    mapper: CartUiMapper
) : ViewModel() {
    val uiState: StateFlow<CartBadgeUiState> = repository.cart
        .map { mapper.badge(it.totals.itemCount) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CartBadgeUiState(accessibilityDescription = CartUiText.Resource(R.string.cart_badge_empty_accessibility))
        )
}
