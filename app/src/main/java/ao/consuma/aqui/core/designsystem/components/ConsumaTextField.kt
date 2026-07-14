package ao.consuma.aqui.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.semantics
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme

@Composable
fun ConsumaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    error: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val fieldModifier = if (error && supportingText != null) {
        modifier.semantics { semanticsError(supportingText) }
    } else {
        modifier
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = fieldModifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        isError = error,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(imageVector = it, contentDescription = null) } },
        trailingIcon = trailingIcon?.let { { Icon(imageVector = it, contentDescription = null) } },
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun ConsumaSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true
) {
    ConsumaTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder ?: stringResource(R.string.search_placeholder),
        leadingIcon = Icons.Default.Search,
        enabled = enabled,
        singleLine = true
    )
}

@Preview(showBackground = true)
@Composable
private fun ConsumaTextFieldPreview() {
    ConsumaAquiTheme {
        ConsumaTextField(value = "", onValueChange = {}, label = "Nome")
    }
}
