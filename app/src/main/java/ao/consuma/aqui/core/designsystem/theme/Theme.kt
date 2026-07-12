package ao.consuma.aqui.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ConsumaOrangePrimary,
    onPrimary = ConsumaOrangeOnPrimary,
    primaryContainer = ConsumaOrangeContainer,
    onPrimaryContainer = ConsumaOrangeOnContainer,
    secondary = ConsumaSecondary,
    onSecondary = ConsumaOnSecondary,
    secondaryContainer = ConsumaSecondaryContainer,
    onSecondaryContainer = ConsumaOnSecondaryContainer,
    background = ConsumaBackgroundDark,
    onBackground = ConsumaOnBackgroundDark,
    surface = ConsumaSurfaceDark,
    onSurface = ConsumaOnSurfaceDark,
    surfaceVariant = ConsumaSurfaceVariantDark,
    onSurfaceVariant = ConsumaOnSurfaceVariantDark,
    outline = ConsumaOutlineDark,
    error = ConsumaError,
    onError = ConsumaOnError,
)

private val LightColorScheme = lightColorScheme(
    primary = ConsumaOrangePrimary,
    onPrimary = ConsumaOrangeOnPrimary,
    primaryContainer = ConsumaOrangeContainer,
    onPrimaryContainer = ConsumaOrangeOnContainer,
    secondary = ConsumaSecondary,
    onSecondary = ConsumaOnSecondary,
    secondaryContainer = ConsumaSecondaryContainer,
    onSecondaryContainer = ConsumaOnSecondaryContainer,
    background = ConsumaBackgroundLight,
    onBackground = ConsumaOnBackgroundLight,
    surface = ConsumaSurfaceLight,
    onSurface = ConsumaOnSurfaceLight,
    surfaceVariant = ConsumaSurfaceVariantLight,
    onSurfaceVariant = ConsumaOnSurfaceVariantLight,
    outline = ConsumaOutlineLight,
    error = ConsumaError,
    onError = ConsumaOnError,
)

object ConsumaAquiTheme {
    val semanticColors: ConsumaSemanticColors
        @Composable
        get() = LocalConsumaSemanticColors.current
}

@Composable
fun ConsumaAquiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled by default for brand consistency
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val semanticColors = if (darkTheme) darkSemanticColors else lightSemanticColors

    CompositionLocalProvider(
        LocalConsumaSemanticColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}