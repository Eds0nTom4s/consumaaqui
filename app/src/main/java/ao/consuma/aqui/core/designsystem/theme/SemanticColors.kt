package ao.consuma.aqui.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ConsumaSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
)

val LocalConsumaSemanticColors = staticCompositionLocalOf {
    ConsumaSemanticColors(
        success = Color.Unspecified,
        onSuccess = Color.Unspecified,
        warning = Color.Unspecified,
        onWarning = Color.Unspecified,
        info = Color.Unspecified,
        onInfo = Color.Unspecified,
    )
}

val lightSemanticColors = ConsumaSemanticColors(
    success = ConsumaSuccess,
    onSuccess = ConsumaOnSuccess,
    warning = ConsumaWarning,
    onWarning = ConsumaOnWarning,
    info = ConsumaInfo,
    onInfo = ConsumaOnInfo,
)

val darkSemanticColors = ConsumaSemanticColors(
    success = ConsumaSuccess, // could adjust for dark mode later
    onSuccess = ConsumaOnSuccess,
    warning = ConsumaWarning,
    onWarning = ConsumaOnWarning,
    info = ConsumaInfo,
    onInfo = ConsumaOnInfo,
)
