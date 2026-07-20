package ao.consuma.aqui.feature.checkout.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import ao.consuma.aqui.R
import ao.consuma.aqui.core.designsystem.components.ConsumaCard
import ao.consuma.aqui.core.designsystem.components.ConsumaInlineMessage
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusChip
import ao.consuma.aqui.core.designsystem.components.ConsumaStatusSemantic
import ao.consuma.aqui.core.designsystem.components.ConsumaTextField
import ao.consuma.aqui.core.designsystem.tokens.ConsumaSpacing
import ao.consuma.aqui.core.navigation.NavigationTestTags
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.presentation.checkout.CheckoutUiEvent
import ao.consuma.aqui.feature.checkout.presentation.checkout.CustomerFormUiState
import ao.consuma.aqui.feature.checkout.presentation.checkout.DeliveryFormUiState
import ao.consuma.aqui.feature.checkout.presentation.checkout.PickupFormUiState
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutCapabilitiesUiModel
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiText
import ao.consuma.aqui.feature.checkout.presentation.mapper.resolve
import ao.consuma.aqui.core.designsystem.theme.ConsumaAquiTheme

@Composable
fun FulfillmentSelector(
    capabilities: CheckoutCapabilitiesUiModel,
    selected: FulfillmentMethod?,
    error: CheckoutUiText?,
    enabled: Boolean,
    onSelected: (FulfillmentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().selectableGroup().testTag(NavigationTestTags.CHECKOUT_FULFILLMENT),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        Text(stringResource(R.string.checkout_fulfillment_title), style = MaterialTheme.typography.titleLarge)
        FulfillmentOption(
            title = stringResource(R.string.checkout_pickup_title),
            description = stringResource(R.string.checkout_pickup_description),
            selected = selected == FulfillmentMethod.PICKUP,
            available = capabilities.pickupAvailable,
            enabled = enabled,
            simulated = false,
            testTag = NavigationTestTags.CHECKOUT_PICKUP,
            onClick = { onSelected(FulfillmentMethod.PICKUP) }
        )
        FulfillmentOption(
            title = stringResource(R.string.checkout_delivery_title),
            description = stringResource(R.string.checkout_delivery_description),
            selected = selected == FulfillmentMethod.DELIVERY_MOCK,
            available = capabilities.deliveryAvailable,
            enabled = enabled,
            simulated = true,
            testTag = NavigationTestTags.CHECKOUT_DELIVERY,
            onClick = { onSelected(FulfillmentMethod.DELIVERY_MOCK) }
        )
        error?.let {
            ConsumaInlineMessage(it.resolve(), ConsumaStatusSemantic.ERROR)
        }
    }
}

@Composable
private fun FulfillmentOption(
    title: String,
    description: String,
    selected: Boolean,
    available: Boolean,
    enabled: Boolean,
    simulated: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val selectedDescription = stringResource(
        if (selected) R.string.checkout_selected else R.string.checkout_not_selected
    )
    ConsumaCard(
        Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .selectable(
                selected = selected,
                enabled = enabled && available,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { stateDescription = selectedDescription }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(ConsumaSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled && available
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(ConsumaSpacing.sm)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (simulated) ConsumaStatusChip(
                        stringResource(R.string.checkout_delivery_mock_badge),
                        ConsumaStatusSemantic.INFO
                    )
                }
                Text(description, style = MaterialTheme.typography.bodyMedium)
                if (!available) Text(
                    stringResource(R.string.checkout_fulfillment_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                if (simulated) Text(
                    stringResource(R.string.checkout_delivery_mock_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CustomerForm(
    state: CustomerFormUiState,
    enabled: Boolean,
    onEvent: (CheckoutUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().testTag(NavigationTestTags.CHECKOUT_CUSTOMER),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        Text(stringResource(R.string.checkout_customer_title), style = MaterialTheme.typography.titleLarge)
        ConsumaTextField(
            value = state.fullName,
            onValueChange = { onEvent(CheckoutUiEvent.CustomerNameChanged(it)) },
            modifier = Modifier.testTag(NavigationTestTags.CHECKOUT_CUSTOMER_NAME),
            label = stringResource(R.string.checkout_name_label),
            error = state.fullNameError != null,
            supportingText = state.fullNameError?.resolve(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        ConsumaTextField(
            value = state.phone,
            onValueChange = { onEvent(CheckoutUiEvent.CustomerPhoneChanged(it)) },
            modifier = Modifier.testTag(NavigationTestTags.CHECKOUT_CUSTOMER_PHONE),
            label = stringResource(R.string.checkout_phone_label),
            placeholder = stringResource(R.string.checkout_phone_placeholder),
            error = state.phoneError != null,
            supportingText = state.phoneError?.resolve(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )
        )
        ConsumaTextField(
            value = state.email,
            onValueChange = { onEvent(CheckoutUiEvent.CustomerEmailChanged(it)) },
            modifier = Modifier.testTag(NavigationTestTags.CHECKOUT_CUSTOMER_EMAIL),
            label = stringResource(R.string.checkout_email_label),
            error = state.emailError != null,
            supportingText = state.emailError?.resolve(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            )
        )
    }
}

@Composable
fun PickupForm(
    state: PickupFormUiState,
    enabled: Boolean,
    onEvent: (CheckoutUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().testTag(NavigationTestTags.CHECKOUT_DETAILS),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        Text(stringResource(R.string.checkout_pickup_details_title), style = MaterialTheme.typography.titleLarge)
        ConsumaTextField(
            state.contactName,
            { onEvent(CheckoutUiEvent.PickupNameChanged(it)) },
            Modifier.testTag(NavigationTestTags.CHECKOUT_PICKUP_NAME),
            label = stringResource(R.string.checkout_pickup_name_label),
            error = state.contactNameError != null,
            supportingText = state.contactNameError?.resolve(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        ConsumaTextField(
            state.contactPhone,
            { onEvent(CheckoutUiEvent.PickupPhoneChanged(it)) },
            Modifier.testTag(NavigationTestTags.CHECKOUT_PICKUP_PHONE),
            label = stringResource(R.string.checkout_pickup_phone_label),
            placeholder = stringResource(R.string.checkout_phone_placeholder),
            error = state.contactPhoneError != null,
            supportingText = state.contactPhoneError?.resolve(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            )
        )
        ConsumaStatusChip(
            stringResource(R.string.checkout_pickup_asap),
            ConsumaStatusSemantic.INFO
        )
        ConsumaInlineMessage(
            stringResource(R.string.checkout_pickup_future_notice),
            ConsumaStatusSemantic.NEUTRAL
        )
    }
}

@Composable
fun DeliveryForm(
    state: DeliveryFormUiState,
    enabled: Boolean,
    onEvent: (CheckoutUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().testTag(NavigationTestTags.CHECKOUT_DETAILS),
        verticalArrangement = Arrangement.spacedBy(ConsumaSpacing.md)
    ) {
        Text(stringResource(R.string.checkout_delivery_details_title), style = MaterialTheme.typography.titleLarge)
        ConsumaInlineMessage(
            stringResource(R.string.checkout_delivery_mock_notice),
            ConsumaStatusSemantic.INFO
        )
        DeliveryField(
            state.province, CheckoutUiEvent::DeliveryProvinceChanged,
            R.string.checkout_province_label, NavigationTestTags.CHECKOUT_DELIVERY_PROVINCE,
            state.provinceError, enabled, onEvent
        )
        DeliveryField(
            state.municipality, CheckoutUiEvent::DeliveryMunicipalityChanged,
            R.string.checkout_municipality_label, NavigationTestTags.CHECKOUT_DELIVERY_MUNICIPALITY,
            state.municipalityError, enabled, onEvent
        )
        Text(
            stringResource(R.string.checkout_location_mock_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DeliveryField(
            state.districtOrArea, CheckoutUiEvent::DeliveryAreaChanged,
            R.string.checkout_area_label, NavigationTestTags.CHECKOUT_DELIVERY_AREA,
            null, enabled, onEvent
        )
        DeliveryField(
            state.streetOrReference, CheckoutUiEvent::DeliveryStreetChanged,
            R.string.checkout_street_label, NavigationTestTags.CHECKOUT_DELIVERY_STREET,
            state.streetError, enabled, onEvent
        )
        DeliveryField(
            state.buildingOrHouse, CheckoutUiEvent::DeliveryBuildingChanged,
            R.string.checkout_building_label, NavigationTestTags.CHECKOUT_DELIVERY_BUILDING,
            null, enabled, onEvent
        )
        ConsumaTextField(
            state.referencePoint,
            { onEvent(CheckoutUiEvent.DeliveryReferenceChanged(it)) },
            Modifier.testTag(NavigationTestTags.CHECKOUT_DELIVERY_REFERENCE),
            label = stringResource(R.string.checkout_reference_label),
            supportingText = stringResource(R.string.checkout_reference_helper),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        DeliveryField(
            state.recipientName, CheckoutUiEvent::DeliveryRecipientNameChanged,
            R.string.checkout_recipient_label, NavigationTestTags.CHECKOUT_DELIVERY_RECIPIENT,
            state.recipientNameError, enabled, onEvent
        )
        ConsumaTextField(
            state.recipientPhone,
            { onEvent(CheckoutUiEvent.DeliveryRecipientPhoneChanged(it)) },
            Modifier.testTag(NavigationTestTags.CHECKOUT_DELIVERY_PHONE),
            label = stringResource(R.string.checkout_recipient_phone_label),
            placeholder = stringResource(R.string.checkout_phone_placeholder),
            error = state.recipientPhoneError != null,
            supportingText = state.recipientPhoneError?.resolve(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )
        )
        ConsumaTextField(
            state.instructions,
            { onEvent(CheckoutUiEvent.DeliveryInstructionsChanged(it)) },
            Modifier.testTag(NavigationTestTags.CHECKOUT_DELIVERY_INSTRUCTIONS),
            label = stringResource(R.string.checkout_instructions_label),
            placeholder = stringResource(R.string.checkout_instructions_placeholder),
            error = state.instructionsError != null,
            supportingText = state.instructionsError?.resolve() ?: pluralStringResource(
                R.plurals.checkout_instructions_counter,
                state.instructionsCount,
                state.instructionsCount,
                300
            ),
            enabled = enabled,
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Text(
            stringResource(R.string.checkout_sensitive_data_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeliveryField(
    value: String,
    event: (String) -> CheckoutUiEvent,
    label: Int,
    tag: String,
    error: CheckoutUiText?,
    enabled: Boolean,
    onEvent: (CheckoutUiEvent) -> Unit
) {
    ConsumaTextField(
        value,
        { onEvent(event(it)) },
        Modifier.testTag(tag),
        label = stringResource(label),
        error = error != null,
        supportingText = error?.resolve(),
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Preview(showBackground = true) @Composable private fun FulfillmentSelectorPreview() {
    ConsumaAquiTheme {
        FulfillmentSelector(CheckoutCapabilitiesUiModel(true, true), FulfillmentMethod.PICKUP, null, true, {})
    }
}
@Preview(showBackground = true) @Composable private fun CustomerFormPreview() {
    ConsumaAquiTheme { CustomerForm(CustomerFormUiState("Ana Silva", "+244 923 456 789", ""), true, {}) }
}
@Preview(showBackground = true) @Composable private fun PickupFormPreview() {
    ConsumaAquiTheme { PickupForm(PickupFormUiState("Ana Silva", "+244 923 456 789"), true, {}) }
}
@Preview(showBackground = true) @Composable private fun DeliveryFormPreview() {
    ConsumaAquiTheme {
        DeliveryForm(
            DeliveryFormUiState(
                "Luanda", "Talatona", "Benfica", "Rua 10", "Casa azul",
                "Próximo ao Banco BIC", "Ana Silva", "+244 923 456 789", "Ligar ao chegar"
            ),
            true,
            {}
        )
    }
}
