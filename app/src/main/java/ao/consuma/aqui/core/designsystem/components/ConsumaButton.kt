package ao.consuma.aqui.core.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSize
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing

@Composable
fun ConsumaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    fullWidth: Boolean = false
) {
    val loadingDescription = stringResource(R.string.loading)
    val finalModifier = modifier.height(ConsumaSize.buttonHeight).let {
        if (fullWidth) it.fillMaxWidth() else it
    }.semantics { if (loading) contentDescription = loadingDescription }
    
    Button(
        onClick = { if (!loading) onClick() },
        modifier = finalModifier,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ConsumaSize.iconMedium),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = ConsumaSpacing.xxs
            )
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ConsumaSize.iconMedium))
                Spacer(modifier = Modifier.width(ConsumaSpacing.sm))
            }
            Text(text = text)
        }
    }
}

@Composable
fun ConsumaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    fullWidth: Boolean = false
) {
    val loadingDescription = stringResource(R.string.loading)
    val finalModifier = modifier.height(ConsumaSize.buttonHeight).let {
        if (fullWidth) it.fillMaxWidth() else it
    }.semantics { if (loading) contentDescription = loadingDescription }

    OutlinedButton(
        onClick = { if (!loading) onClick() },
        modifier = finalModifier,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ConsumaSize.iconMedium),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = ConsumaSpacing.xxs
            )
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ConsumaSize.iconMedium))
                Spacer(modifier = Modifier.width(ConsumaSpacing.sm))
            }
            Text(text = text)
        }
    }
}

@Composable
fun ConsumaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(ConsumaSize.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = text)
    }
}

@Composable
fun ConsumaIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(ConsumaSize.touchTarget),
        enabled = enabled
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsumaButtonPreview() {
    ConsumaAquiTheme {
        Row(modifier = Modifier.fillMaxWidth()) {
            ConsumaPrimaryButton(text = "Primary", onClick = {})
        }
    }
}
