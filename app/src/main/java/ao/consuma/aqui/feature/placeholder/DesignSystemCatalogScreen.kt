package ao.consuma.aqui.feature.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ao.consuma.aqui.core.designsystem.components.ConsumaAvatar
import ao.consuma.aqui.core.designsystem.components.ConsumaDivider
import ao.consuma.aqui.core.designsystem.components.ConsumaEmptyState
import ao.consuma.aqui.core.designsystem.components.ConsumaErrorState
import ao.consuma.aqui.core.designsystem.components.ConsumaFilterChip
import ao.consuma.aqui.core.designsystem.components.ConsumaInlineMessage
import ao.consuma.aqui.core.designsystem.components.ConsumaLoadingState
import ao.consuma.aqui.core.designsystem.components.ConsumaMerchantCardPlaceholder
import ao.consuma.aqui.core.designsystem.components.ConsumaOfflineBanner
import ao.consuma.aqui.core.designsystem.components.ConsumaPriceText
import ao.consuma.aqui.core.designsystem.components.ConsumaPrimaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaSearchField
import ao.consuma.aqui.core.designsystem.components.ConsumaSecondaryButton
import ao.consuma.aqui.core.designsystem.components.ConsumaSectionHeader
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusChip
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.components.ConsumaTextButton
import ao.consuma.aqui.core.designsystem.components.ConsumaTextField
import ao.consuma.aqui.core.designsystem.components.ConsumaTopAppBar
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing

@Composable
fun DesignSystemCatalogScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            ConsumaTopAppBar(
                title = "Design System",
                onBackClick = onNavigateBack
            )
        },
        modifier = Modifier.testTag("design_system_catalog")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(ConsumaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.lg)
        ) {
            ConsumaOfflineBanner()

            ConsumaSectionHeader(title = "Cores e Temas")
            Text(text = "O catálogo suporta dark e light theme nativamente.", style = MaterialTheme.typography.bodyMedium)

            ConsumaSectionHeader(title = "Tipografia")
            Text(text = "Headline Large", style = MaterialTheme.typography.headlineLarge)
            Text(text = "Title Medium", style = MaterialTheme.typography.titleMedium)
            Text(text = "Body Medium", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Label Small", style = MaterialTheme.typography.labelSmall)
            
            ConsumaDivider()

            ConsumaSectionHeader(title = "Botões")
            ConsumaPrimaryButton(text = "Primary Button", onClick = { }, fullWidth = true, modifier = Modifier.testTag("primary_button"))
            ConsumaSecondaryButton(text = "Secondary Button", onClick = { }, fullWidth = true)
            ConsumaTextButton(text = "Text Button", onClick = { })
            ConsumaPrimaryButton(text = "Loading Button", onClick = { }, loading = true)

            ConsumaDivider()

            ConsumaSectionHeader(title = "Inputs")
            ConsumaTextField(value = "", onValueChange = {}, label = "Text Field Padrão")
            ConsumaSearchField(query = "", onQueryChange = {}, modifier = Modifier.testTag("search_field"))

            ConsumaDivider()

            ConsumaSectionHeader(title = "Cartões")
            ConsumaMerchantCardPlaceholder(title = "Café de Angola", subtitle = "Luanda, Talatona", actionText = "Ver", onActionClick = {})

            ConsumaDivider()

            ConsumaSectionHeader(title = "Chips e Estados")
            ConsumaFilterChip(selected = true, onClick = {}, label = "Filtrar: Proximidade")
            ConsumaStatusChip(text = "SUCESSO", semantic = ConsumaStatusSemantic.SUCCESS)
            ConsumaStatusChip(text = "ALERTA", semantic = ConsumaStatusSemantic.WARNING)
            
            ConsumaDivider()

            ConsumaSectionHeader(title = "Avatares")
            ConsumaAvatar(initials = "CA")
            
            ConsumaDivider()

            ConsumaSectionHeader(title = "Preços")
            ConsumaPriceText(price = "5.500 Kz", oldPrice = "6.000 Kz")

            ConsumaDivider()

            ConsumaSectionHeader(title = "Mensagens Inline")
            ConsumaInlineMessage(message = "Aviso importante do sistema.", semantic = ConsumaStatusSemantic.INFO)

            ConsumaDivider()

            ConsumaSectionHeader(title = "Estados de Tela")
            Spacer(modifier = Modifier.height(200.dp).fillMaxWidth().padding(ConsumaSpacing.sm).let {
                // Just to give space to show
                it
            })
        }
    }
}
