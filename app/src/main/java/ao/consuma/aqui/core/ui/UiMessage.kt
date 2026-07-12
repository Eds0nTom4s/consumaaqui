package ao.consuma.aqui.core.ui

import androidx.annotation.StringRes

sealed interface UiMessage {
    data class Resource(@StringRes val resId: Int) : UiMessage
    data class Dynamic(val value: String) : UiMessage
}
