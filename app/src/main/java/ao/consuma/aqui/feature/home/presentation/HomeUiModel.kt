package ao.consuma.aqui.feature.home.presentation

import androidx.annotation.StringRes

sealed interface UiText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Dynamic(val value: String) : UiText
}

typealias LocationUiModel = ao.consuma.aqui.feature.discovery.presentation.mapper.LocationUiModel
typealias CategoryUiModel = ao.consuma.aqui.feature.discovery.presentation.mapper.CategoryUiModel
typealias MerchantCompactUiModel = ao.consuma.aqui.feature.discovery.presentation.mapper.MerchantCompactUiModel
