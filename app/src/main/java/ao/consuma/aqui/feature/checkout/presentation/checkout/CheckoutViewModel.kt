package ao.consuma.aqui.feature.checkout.presentation.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.consuma.aqui.R
import ao.consuma.aqui.feature.checkout.domain.command.ConfirmCheckoutCommand
import ao.consuma.aqui.feature.checkout.domain.command.RequestQuoteCommand
import ao.consuma.aqui.feature.checkout.domain.command.SelectFulfillmentCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdateCustomerCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdateDeliveryAddressCommand
import ao.consuma.aqui.feature.checkout.domain.command.UpdatePickupDetailsCommand
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutFulfillment
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSession
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutSessionStatus
import ao.consuma.aqui.feature.checkout.domain.model.CheckoutStep
import ao.consuma.aqui.feature.checkout.domain.model.DeliveryAddress
import ao.consuma.aqui.feature.checkout.domain.model.FulfillmentMethod
import ao.consuma.aqui.feature.checkout.domain.repository.CheckoutRepository
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutConflict
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutError
import ao.consuma.aqui.feature.checkout.domain.result.CheckoutResult
import ao.consuma.aqui.feature.checkout.domain.service.StartCheckout
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiMapper
import ao.consuma.aqui.feature.checkout.presentation.mapper.CheckoutUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val repository: CheckoutRepository,
    private val startCheckout: StartCheckout,
    private val mapper: CheckoutUiMapper,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Initializing)
    val uiState: StateFlow<CheckoutUiState> = mutableUiState.asStateFlow()
    private val mutableEffects = MutableSharedFlow<CheckoutUiEffect>(extraBufferCapacity = 2)
    val effects: SharedFlow<CheckoutUiEffect> = mutableEffects.asSharedFlow()

    private var session: CheckoutSession? = null
    private var currentStep = savedStateHandle.enumValue<CheckoutStep>(CheckoutSavedStateKeys.STEP)
        ?: CheckoutStep.FULFILLMENT
    private var customerForm = CustomerFormUiState(
        fullName = savedStateHandle.get<String>(CheckoutSavedStateKeys.CUSTOMER_NAME).orEmpty(),
        phone = savedStateHandle.get<String>(CheckoutSavedStateKeys.CUSTOMER_PHONE).orEmpty(),
        email = savedStateHandle.get<String>(CheckoutSavedStateKeys.CUSTOMER_EMAIL).orEmpty()
    )
    private var pickupForm = PickupFormUiState(
        contactName = savedStateHandle.get<String>(CheckoutSavedStateKeys.PICKUP_NAME).orEmpty(),
        contactPhone = savedStateHandle.get<String>(CheckoutSavedStateKeys.PICKUP_PHONE).orEmpty()
    )
    private var deliveryForm = DeliveryFormUiState(
        province = savedStateHandle.get<String>(CheckoutSavedStateKeys.DELIVERY_PROVINCE).orEmpty(),
        municipality = savedStateHandle.get<String>(CheckoutSavedStateKeys.DELIVERY_MUNICIPALITY).orEmpty(),
        districtOrArea = savedStateHandle.get<String>(CheckoutSavedStateKeys.DELIVERY_AREA).orEmpty(),
        streetOrReference = savedStateHandle.get<String>(CheckoutSavedStateKeys.DELIVERY_STREET).orEmpty(),
        buildingOrHouse = savedStateHandle.get<String>(CheckoutSavedStateKeys.DELIVERY_BUILDING).orEmpty(),
        referencePoint = savedStateHandle.get<String>(CheckoutSavedStateKeys.DELIVERY_REFERENCE).orEmpty(),
        recipientName = savedStateHandle.get<String>(CheckoutSavedStateKeys.DELIVERY_RECIPIENT).orEmpty(),
        recipientPhone = savedStateHandle.get<String>(CheckoutSavedStateKeys.DELIVERY_PHONE).orEmpty(),
        instructions = savedStateHandle.get<String>(CheckoutSavedStateKeys.DELIVERY_INSTRUCTIONS).orEmpty()
    )
    private var fulfillmentError: CheckoutUiText? = null
    private var pendingOperation: CheckoutPendingOperation? = null
    private var initialized = false
    private var forceQuoteExpired = false
    private var cartConflict: CheckoutUiState.CartConflict? = null

    init {
        viewModelScope.launch {
            repository.session.collect { value ->
                if (value != null) {
                    session = value
                    if (value.status != CheckoutSessionStatus.ConfirmedMock) {
                        hydrateFromValidatedSession(value)
                    }
                    render()
                }
            }
        }
    }

    fun onEvent(event: CheckoutUiEvent) {
        when (event) {
            CheckoutUiEvent.Initialize -> initialize()
            is CheckoutUiEvent.FulfillmentSelected -> selectFulfillment(event.method)
            is CheckoutUiEvent.CustomerNameChanged -> updateCustomerName(event.value)
            is CheckoutUiEvent.CustomerPhoneChanged -> updateCustomerPhone(event.value)
            is CheckoutUiEvent.CustomerEmailChanged -> updateCustomerEmail(event.value)
            is CheckoutUiEvent.PickupNameChanged -> updatePickupName(event.value)
            is CheckoutUiEvent.PickupPhoneChanged -> updatePickupPhone(event.value)
            is CheckoutUiEvent.DeliveryProvinceChanged -> updateDeliveryProvince(event.value)
            is CheckoutUiEvent.DeliveryMunicipalityChanged -> updateDeliveryMunicipality(event.value)
            is CheckoutUiEvent.DeliveryAreaChanged -> updateDeliveryArea(event.value)
            is CheckoutUiEvent.DeliveryStreetChanged -> updateDeliveryStreet(event.value)
            is CheckoutUiEvent.DeliveryBuildingChanged -> updateDeliveryBuilding(event.value)
            is CheckoutUiEvent.DeliveryReferenceChanged -> updateDeliveryReference(event.value)
            is CheckoutUiEvent.DeliveryRecipientNameChanged -> updateDeliveryRecipient(event.value)
            is CheckoutUiEvent.DeliveryRecipientPhoneChanged -> updateDeliveryPhone(event.value)
            is CheckoutUiEvent.DeliveryInstructionsChanged -> updateDeliveryInstructions(event.value)
            CheckoutUiEvent.Continue -> continueFlow()
            CheckoutUiEvent.BackStep -> backStep()
            CheckoutUiEvent.RequestQuote, CheckoutUiEvent.RefreshQuote -> saveDetailsAndRequestQuote()
            CheckoutUiEvent.Confirm -> confirm()
            CheckoutUiEvent.RestartCheckout -> restart()
            CheckoutUiEvent.ReviewCart -> mutableEffects.tryEmit(CheckoutUiEffect.NavigateToCart)
            is CheckoutUiEvent.EditStep -> editStep(event.step)
        }
    }

    private fun initialize() {
        if (initialized || pendingOperation != null) return
        if (repository.session.value?.status == CheckoutSessionStatus.ConfirmedMock) {
            clearFormsForNewCheckout()
        }
        initialized = true
        cartConflict = null
        pendingOperation = CheckoutPendingOperation.Starting
        mutableUiState.value = CheckoutUiState.Initializing
        viewModelScope.launch {
            when (val result = startCheckout()) {
                is CheckoutResult.Success -> {
                    session = result.data
                    hydrateFromValidatedSession(result.data)
                    pendingOperation = null
                    render()
                }
                is CheckoutResult.Conflict -> showConflict(result.conflict)
                is CheckoutResult.Failure -> showStartFailure(result.error)
            }
        }
    }

    private fun selectFulfillment(method: FulfillmentMethod) {
        val current = session ?: return
        if (pendingOperation != null || current.selectedFulfillmentMethod == method) return
        pendingOperation = CheckoutPendingOperation.SavingFulfillment
        fulfillmentError = null
        render()
        viewModelScope.launch {
            when (val result = repository.selectFulfillment(
                SelectFulfillmentCommand(current.id, method)
            )) {
                is CheckoutResult.Success -> {
                    session = result.data
                    savedStateHandle[CheckoutSavedStateKeys.FULFILLMENT] = method.name
                    forceQuoteExpired = false
                    pendingOperation = null
                    render()
                }
                is CheckoutResult.Conflict -> showConflict(result.conflict)
                is CheckoutResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun continueFlow() {
        if (pendingOperation != null) return
        when (currentStep) {
            CheckoutStep.FULFILLMENT -> continueFromFulfillment()
            CheckoutStep.CUSTOMER -> saveCustomer()
            CheckoutStep.DETAILS -> saveDetailsAndRequestQuote()
            CheckoutStep.REVIEW -> confirm()
            CheckoutStep.CONFIRMATION -> Unit
        }
    }

    private fun continueFromFulfillment() {
        val current = session ?: return
        if (current.selectedFulfillmentMethod == null) {
            fulfillmentError = CheckoutUiText.Resource(R.string.checkout_error_select_fulfillment)
            render()
            return
        }
        moveTo(CheckoutStep.CUSTOMER)
    }

    private fun saveCustomer() {
        val current = session ?: return
        customerForm = customerForm.withValidation()
        render()
        if (customerForm.hasErrors) return
        pendingOperation = CheckoutPendingOperation.SavingCustomer
        render()
        viewModelScope.launch {
            when (val result = repository.updateCustomer(
                UpdateCustomerCommand(
                    current.id,
                    customerForm.fullName,
                    customerForm.phone,
                    customerForm.email
                )
            )) {
                is CheckoutResult.Success -> {
                    session = result.data
                    prefillDetails(result.data)
                    pendingOperation = null
                    moveTo(CheckoutStep.DETAILS)
                }
                is CheckoutResult.Conflict -> showConflict(result.conflict)
                is CheckoutResult.Failure -> handleCustomerFailure(result.error)
            }
        }
    }

    private fun saveDetailsAndRequestQuote() {
        val current = session ?: return
        if (pendingOperation != null) return
        when (current.selectedFulfillmentMethod) {
            FulfillmentMethod.PICKUP -> pickupForm = pickupForm.withValidation()
            FulfillmentMethod.DELIVERY_MOCK -> deliveryForm = deliveryForm.withValidation()
            null -> {
                moveTo(CheckoutStep.FULFILLMENT)
                return
            }
        }
        render()
        if (pickupForm.hasErrors && current.selectedFulfillmentMethod == FulfillmentMethod.PICKUP ||
            deliveryForm.hasErrors && current.selectedFulfillmentMethod == FulfillmentMethod.DELIVERY_MOCK
        ) return

        pendingOperation = CheckoutPendingOperation.SavingDetails
        render()
        viewModelScope.launch {
            val detailsResult = when (current.selectedFulfillmentMethod) {
                FulfillmentMethod.PICKUP -> repository.updatePickupDetails(
                    UpdatePickupDetailsCommand(current.id, pickupForm.contactName, pickupForm.contactPhone)
                )
                FulfillmentMethod.DELIVERY_MOCK -> repository.updateDeliveryAddress(
                    UpdateDeliveryAddressCommand(
                        current.id,
                        DeliveryAddress(
                            deliveryForm.province,
                            deliveryForm.municipality,
                            deliveryForm.districtOrArea,
                            deliveryForm.streetOrReference,
                            deliveryForm.buildingOrHouse,
                            deliveryForm.referencePoint,
                            null,
                            null
                        ),
                        deliveryForm.recipientName,
                        deliveryForm.recipientPhone,
                        deliveryForm.instructions
                    )
                )
            }
            when (detailsResult) {
                is CheckoutResult.Success -> {
                    session = detailsResult.data
                    requestQuote(detailsResult.data)
                }
                is CheckoutResult.Conflict -> showConflict(detailsResult.conflict)
                is CheckoutResult.Failure -> handleDetailsFailure(detailsResult.error)
            }
        }
    }

    private suspend fun requestQuote(current: CheckoutSession) {
        pendingOperation = CheckoutPendingOperation.RequestingQuote
        render()
        when (val result = repository.requestQuote(
            RequestQuoteCommand(current.id, current.cartId, current.cartVersion)
        )) {
            is CheckoutResult.Success -> {
                session = result.data
                forceQuoteExpired = false
                pendingOperation = null
                moveTo(CheckoutStep.REVIEW)
            }
            is CheckoutResult.Conflict -> showConflict(result.conflict)
            is CheckoutResult.Failure -> handleFailure(result.error)
        }
    }

    private fun confirm() {
        val current = session ?: return
        if (pendingOperation != null || current.quote == null) return
        pendingOperation = CheckoutPendingOperation.Confirming
        render()
        viewModelScope.launch {
            when (val result = repository.confirmCheckout(
                ConfirmCheckoutCommand(current.id, current.cartId, current.cartVersion)
            )) {
                is CheckoutResult.Success -> {
                    pendingOperation = null
                    mutableUiState.value = CheckoutUiState.ConfirmedMock
                    mutableEffects.emit(CheckoutUiEffect.NavigateToConfirmation)
                }
                is CheckoutResult.Conflict -> showConflict(result.conflict)
                is CheckoutResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun restart() {
        if (pendingOperation != null) return
        pendingOperation = CheckoutPendingOperation.Restarting
        cartConflict = null
        mutableUiState.value = CheckoutUiState.Initializing
        viewModelScope.launch {
            repository.resetCheckout()
            currentStep = CheckoutStep.FULFILLMENT
            savedStateHandle[CheckoutSavedStateKeys.STEP] = currentStep.name
            savedStateHandle[CheckoutSavedStateKeys.SESSION_ID] = null
            forceQuoteExpired = false
            when (val result = startCheckout()) {
                is CheckoutResult.Success -> {
                    session = result.data
                    pendingOperation = null
                    render()
                }
                is CheckoutResult.Conflict -> showConflict(result.conflict)
                is CheckoutResult.Failure -> showStartFailure(result.error)
            }
        }
    }

    private fun backStep() {
        if (pendingOperation != null) return
        when (currentStep) {
            CheckoutStep.FULFILLMENT -> mutableEffects.tryEmit(CheckoutUiEffect.NavigateToCart)
            CheckoutStep.CUSTOMER -> moveTo(CheckoutStep.FULFILLMENT)
            CheckoutStep.DETAILS -> moveTo(CheckoutStep.CUSTOMER)
            CheckoutStep.REVIEW -> moveTo(CheckoutStep.DETAILS)
            CheckoutStep.CONFIRMATION -> mutableEffects.tryEmit(CheckoutUiEffect.NavigateToCart)
        }
    }

    private fun editStep(step: CheckoutStep) {
        if (pendingOperation != null || step !in CheckoutUiMapper.visibleSteps) return
        if (CheckoutUiMapper.visibleSteps.indexOf(step) <=
            CheckoutUiMapper.visibleSteps.indexOf(currentStep)
        ) moveTo(step)
    }

    private fun moveTo(step: CheckoutStep) {
        currentStep = step
        savedStateHandle[CheckoutSavedStateKeys.STEP] = step.name
        render()
    }

    private fun hydrateFromValidatedSession(value: CheckoutSession) {
        val sameSavedSession = savedStateHandle.get<String>(CheckoutSavedStateKeys.SESSION_ID) == value.id
        if (!sameSavedSession) {
            currentStep = CheckoutStep.FULFILLMENT
            savedStateHandle[CheckoutSavedStateKeys.STEP] = currentStep.name
            savedStateHandle[CheckoutSavedStateKeys.SESSION_ID] = value.id
        }
        value.customer?.let { customer ->
            if (customerForm.fullName.isBlank()) updateCustomerName(customer.fullName, render = false)
            if (customerForm.phone.isBlank()) updateCustomerPhone(customer.contact.phoneNumber, render = false)
            if (customerForm.email.isBlank()) updateCustomerEmail(customer.contact.email.orEmpty(), render = false)
        }
        when (val fulfillment = value.fulfillment) {
            is CheckoutFulfillment.Pickup -> {
                if (pickupForm.contactName.isBlank()) updatePickupName(
                    fulfillment.details.contactName,
                    render = false
                )
                if (pickupForm.contactPhone.isBlank()) updatePickupPhone(
                    fulfillment.details.contactPhone,
                    render = false
                )
            }
            is CheckoutFulfillment.Delivery -> hydrateDelivery(fulfillment)
            null -> Unit
        }
        val selected = value.selectedFulfillmentMethod
        savedStateHandle[CheckoutSavedStateKeys.FULFILLMENT] = selected?.name
        currentStep = currentStep.coerceFor(value)
    }

    private fun hydrateDelivery(value: CheckoutFulfillment.Delivery) {
        val address = value.details.address
        if (deliveryForm.province.isBlank()) updateDeliveryProvince(address.province, false)
        if (deliveryForm.municipality.isBlank()) updateDeliveryMunicipality(address.municipality, false)
        if (deliveryForm.districtOrArea.isBlank()) updateDeliveryArea(address.districtOrArea.orEmpty(), false)
        if (deliveryForm.streetOrReference.isBlank()) updateDeliveryStreet(address.streetOrReference, false)
        if (deliveryForm.buildingOrHouse.isBlank()) updateDeliveryBuilding(address.buildingOrHouse.orEmpty(), false)
        if (deliveryForm.referencePoint.isBlank()) updateDeliveryReference(address.referencePoint.orEmpty(), false)
        if (deliveryForm.recipientName.isBlank()) updateDeliveryRecipient(value.details.recipientName, false)
        if (deliveryForm.recipientPhone.isBlank()) updateDeliveryPhone(value.details.recipientPhone, false)
        if (deliveryForm.instructions.isBlank()) updateDeliveryInstructions(value.details.instructions.orEmpty(), false)
    }

    private fun prefillDetails(value: CheckoutSession) {
        val customer = value.customer ?: return
        if (pickupForm.contactName.isBlank()) updatePickupName(customer.fullName, false)
        if (pickupForm.contactPhone.isBlank()) updatePickupPhone(customer.contact.phoneNumber, false)
        if (deliveryForm.recipientName.isBlank()) updateDeliveryRecipient(customer.fullName, false)
        if (deliveryForm.recipientPhone.isBlank()) updateDeliveryPhone(customer.contact.phoneNumber, false)
    }

    private fun render() {
        cartConflict?.let {
            mutableUiState.value = it
            return
        }
        val value = session ?: return
        if (value.status == CheckoutSessionStatus.ConfirmedMock) {
            mutableUiState.value = CheckoutUiState.ConfirmedMock
            return
        }
        val quote = mapper.quote(value, forceQuoteExpired)
        mutableUiState.value = CheckoutUiState.Content(
            sessionId = value.id,
            currentStep = currentStep,
            availableSteps = mapper.steps(currentStep),
            selectedFulfillment = value.selectedFulfillmentMethod,
            capabilities = mapper.capabilities(value),
            customerForm = customerForm,
            pickupForm = pickupForm,
            deliveryForm = deliveryForm,
            cartSummary = mapper.cart(value),
            quote = quote,
            customerReview = mapper.customer(value),
            fulfillmentReview = mapper.fulfillment(value),
            fulfillmentError = fulfillmentError,
            pendingOperation = pendingOperation,
            canGoBack = pendingOperation == null,
            canContinue = pendingOperation == null && currentStep != CheckoutStep.REVIEW &&
                currentStep != CheckoutStep.CONFIRMATION,
            canConfirm = pendingOperation == null && currentStep == CheckoutStep.REVIEW &&
                value.status == CheckoutSessionStatus.ReadyForReview && quote?.expired == false
        )
    }

    private fun showConflict(conflict: CheckoutConflict) {
        pendingOperation = null
        val message = when (conflict) {
            is CheckoutConflict.CartChanged,
            is CheckoutConflict.CartCleared,
            is CheckoutConflict.MerchantChanged,
            is CheckoutConflict.CurrencyChanged -> CheckoutUiText.Resource(R.string.checkout_cart_changed)
        }
        cartConflict = CheckoutUiState.CartConflict(message, canRestart = true)
        mutableUiState.value = cartConflict!!
    }

    private fun showStartFailure(error: CheckoutError) {
        pendingOperation = null
        initialized = false
        mutableUiState.value = CheckoutUiState.Error(
            mapper.error(error),
            canRetry = error != CheckoutError.EmptyCart && error != CheckoutError.InvalidCart
        )
    }

    private fun handleCustomerFailure(error: CheckoutError) {
        pendingOperation = null
        customerForm = when (error) {
            CheckoutError.InvalidPhone -> customerForm.copy(phoneError = mapper.error(error))
            CheckoutError.InvalidEmail -> customerForm.copy(emailError = mapper.error(error))
            else -> customerForm.copy(fullNameError = mapper.error(error))
        }
        render()
    }

    private fun handleDetailsFailure(error: CheckoutError) {
        pendingOperation = null
        when (session?.selectedFulfillmentMethod) {
            FulfillmentMethod.PICKUP -> pickupForm = pickupForm.copy(
                contactNameError = mapper.error(error),
                contactPhoneError = mapper.error(error)
            )
            FulfillmentMethod.DELIVERY_MOCK -> deliveryForm = deliveryForm.copy(
                streetError = mapper.error(error)
            )
            null -> Unit
        }
        render()
    }

    private fun handleFailure(error: CheckoutError) {
        pendingOperation = null
        when (error) {
            CheckoutError.QuoteExpired -> {
                forceQuoteExpired = true
                currentStep = CheckoutStep.REVIEW
                render()
                mutableEffects.tryEmit(CheckoutUiEffect.Message(mapper.error(error)))
            }
            CheckoutError.FulfillmentUnavailable -> {
                currentStep = CheckoutStep.FULFILLMENT
                fulfillmentError = mapper.error(error)
                render()
            }
            CheckoutError.SessionAlreadyConfirmed -> {
                mutableUiState.value = CheckoutUiState.ConfirmedMock
                mutableEffects.tryEmit(CheckoutUiEffect.NavigateToConfirmation)
            }
            CheckoutError.SessionNotFound -> mutableUiState.value = CheckoutUiState.Error(
                mapper.error(error),
                canRetry = true
            ).also { initialized = false }
            CheckoutError.InvalidPhone,
            CheckoutError.InvalidEmail,
            CheckoutError.InvalidCustomer -> handleCustomerFailure(error)
            CheckoutError.InvalidAddress,
            CheckoutError.InvalidPickupDetails -> handleDetailsFailure(error)
            else -> {
                render()
                mutableEffects.tryEmit(CheckoutUiEffect.Message(mapper.error(error)))
            }
        }
    }

    private fun updateCustomerName(value: String, render: Boolean = true) {
        customerForm = customerForm.copy(fullName = value, fullNameError = null)
        save(CheckoutSavedStateKeys.CUSTOMER_NAME, value, render)
    }
    private fun updateCustomerPhone(value: String, render: Boolean = true) {
        customerForm = customerForm.copy(phone = value, phoneError = null)
        save(CheckoutSavedStateKeys.CUSTOMER_PHONE, value, render)
    }
    private fun updateCustomerEmail(value: String, render: Boolean = true) {
        customerForm = customerForm.copy(email = value, emailError = null)
        save(CheckoutSavedStateKeys.CUSTOMER_EMAIL, value, render)
    }
    private fun updatePickupName(value: String, render: Boolean = true) {
        pickupForm = pickupForm.copy(contactName = value, contactNameError = null)
        save(CheckoutSavedStateKeys.PICKUP_NAME, value, render)
    }
    private fun updatePickupPhone(value: String, render: Boolean = true) {
        pickupForm = pickupForm.copy(contactPhone = value, contactPhoneError = null)
        save(CheckoutSavedStateKeys.PICKUP_PHONE, value, render)
    }
    private fun updateDeliveryProvince(value: String, render: Boolean = true) {
        deliveryForm = deliveryForm.copy(province = value, provinceError = null)
        save(CheckoutSavedStateKeys.DELIVERY_PROVINCE, value, render)
    }
    private fun updateDeliveryMunicipality(value: String, render: Boolean = true) {
        deliveryForm = deliveryForm.copy(municipality = value, municipalityError = null)
        save(CheckoutSavedStateKeys.DELIVERY_MUNICIPALITY, value, render)
    }
    private fun updateDeliveryArea(value: String, render: Boolean = true) {
        deliveryForm = deliveryForm.copy(districtOrArea = value)
        save(CheckoutSavedStateKeys.DELIVERY_AREA, value, render)
    }
    private fun updateDeliveryStreet(value: String, render: Boolean = true) {
        deliveryForm = deliveryForm.copy(streetOrReference = value, streetError = null)
        save(CheckoutSavedStateKeys.DELIVERY_STREET, value, render)
    }
    private fun updateDeliveryBuilding(value: String, render: Boolean = true) {
        deliveryForm = deliveryForm.copy(buildingOrHouse = value)
        save(CheckoutSavedStateKeys.DELIVERY_BUILDING, value, render)
    }
    private fun updateDeliveryReference(value: String, render: Boolean = true) {
        deliveryForm = deliveryForm.copy(referencePoint = value)
        save(CheckoutSavedStateKeys.DELIVERY_REFERENCE, value, render)
    }
    private fun updateDeliveryRecipient(value: String, render: Boolean = true) {
        deliveryForm = deliveryForm.copy(recipientName = value, recipientNameError = null)
        save(CheckoutSavedStateKeys.DELIVERY_RECIPIENT, value, render)
    }
    private fun updateDeliveryPhone(value: String, render: Boolean = true) {
        deliveryForm = deliveryForm.copy(recipientPhone = value, recipientPhoneError = null)
        save(CheckoutSavedStateKeys.DELIVERY_PHONE, value, render)
    }
    private fun updateDeliveryInstructions(value: String, render: Boolean = true) {
        if (value.length > MAX_INSTRUCTIONS) return
        deliveryForm = deliveryForm.copy(instructions = value, instructionsError = null)
        save(CheckoutSavedStateKeys.DELIVERY_INSTRUCTIONS, value, render)
    }

    private fun save(key: String, value: String, shouldRender: Boolean) {
        savedStateHandle[key] = value
        if (shouldRender) render()
    }

    private fun clearFormsForNewCheckout() {
        customerForm = CustomerFormUiState()
        pickupForm = PickupFormUiState()
        deliveryForm = DeliveryFormUiState()
        currentStep = CheckoutStep.FULFILLMENT
        CheckoutSavedStateKeys.personalAndSessionKeys.forEach { savedStateHandle.remove<Any>(it) }
        savedStateHandle[CheckoutSavedStateKeys.STEP] = currentStep.name
    }

    private fun CustomerFormUiState.withValidation() = copy(
        fullNameError = nameError(fullName),
        phoneError = phoneError(phone),
        emailError = emailError(email)
    )

    private fun PickupFormUiState.withValidation() = copy(
        contactNameError = nameError(contactName),
        contactPhoneError = phoneError(contactPhone)
    )

    private fun DeliveryFormUiState.withValidation() = copy(
        provinceError = requiredError(province),
        municipalityError = requiredError(municipality),
        streetError = requiredError(streetOrReference),
        recipientNameError = nameError(recipientName),
        recipientPhoneError = phoneError(recipientPhone),
        instructionsError = if (instructions.length > MAX_INSTRUCTIONS) {
            CheckoutUiText.Resource(R.string.checkout_error_instructions)
        } else null
    )

    private fun nameError(value: String): CheckoutUiText? = when {
        value.trim().isEmpty() -> CheckoutUiText.Resource(R.string.checkout_error_name_required)
        value.trim().length < 2 -> CheckoutUiText.Resource(R.string.checkout_error_name_short)
        value.trim().length > 120 -> CheckoutUiText.Resource(R.string.checkout_error_name_long)
        else -> null
    }

    private fun phoneError(value: String): CheckoutUiText? {
        val normalized = value.filterNot(Char::isWhitespace)
        val digits = normalized.removePrefix("+")
        return if (digits.length !in 9..15 || !Regex("\\+?[0-9]+").matches(normalized)) {
            CheckoutUiText.Resource(R.string.checkout_error_invalid_phone)
        } else null
    }

    private fun emailError(value: String): CheckoutUiText? {
        val normalized = value.trim()
        if (normalized.isEmpty()) return null
        val at = normalized.indexOf('@')
        return if (normalized.length > 254 || normalized.any(Char::isWhitespace) ||
            at <= 0 || at != normalized.lastIndexOf('@') || at >= normalized.lastIndex ||
            !normalized.substring(at + 1).contains('.') || normalized.endsWith('.')
        ) CheckoutUiText.Resource(R.string.checkout_error_invalid_email) else null
    }

    private fun requiredError(value: String): CheckoutUiText? =
        if (value.trim().isEmpty()) CheckoutUiText.Resource(R.string.checkout_error_required_field)
        else null

    private val CustomerFormUiState.hasErrors get() =
        fullNameError != null || phoneError != null || emailError != null
    private val PickupFormUiState.hasErrors get() =
        contactNameError != null || contactPhoneError != null
    private val DeliveryFormUiState.hasErrors get() = provinceError != null ||
        municipalityError != null || streetError != null || recipientNameError != null ||
        recipientPhoneError != null || instructionsError != null

    private fun CheckoutStep.coerceFor(value: CheckoutSession): CheckoutStep = when {
        value.selectedFulfillmentMethod == null -> CheckoutStep.FULFILLMENT
        this == CheckoutStep.REVIEW && value.quote == null -> CheckoutStep.DETAILS
        this == CheckoutStep.CONFIRMATION -> CheckoutStep.FULFILLMENT
        else -> this
    }

    private inline fun <reified T : Enum<T>> SavedStateHandle.enumValue(key: String): T? =
        get<String>(key)?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

    companion object {
        const val MAX_INSTRUCTIONS = 300
    }
}

object CheckoutSavedStateKeys {
    const val SESSION_ID = "checkout_session_id"
    const val STEP = "checkout_step"
    const val FULFILLMENT = "checkout_fulfillment"
    const val CUSTOMER_NAME = "checkout_customer_name"
    const val CUSTOMER_PHONE = "checkout_customer_phone"
    const val CUSTOMER_EMAIL = "checkout_customer_email"
    const val PICKUP_NAME = "checkout_pickup_name"
    const val PICKUP_PHONE = "checkout_pickup_phone"
    const val DELIVERY_PROVINCE = "checkout_delivery_province"
    const val DELIVERY_MUNICIPALITY = "checkout_delivery_municipality"
    const val DELIVERY_AREA = "checkout_delivery_area"
    const val DELIVERY_STREET = "checkout_delivery_street"
    const val DELIVERY_BUILDING = "checkout_delivery_building"
    const val DELIVERY_REFERENCE = "checkout_delivery_reference"
    const val DELIVERY_RECIPIENT = "checkout_delivery_recipient"
    const val DELIVERY_PHONE = "checkout_delivery_phone"
    const val DELIVERY_INSTRUCTIONS = "checkout_delivery_instructions"

    val personalAndSessionKeys = listOf(
        SESSION_ID,
        FULFILLMENT,
        CUSTOMER_NAME,
        CUSTOMER_PHONE,
        CUSTOMER_EMAIL,
        PICKUP_NAME,
        PICKUP_PHONE,
        DELIVERY_PROVINCE,
        DELIVERY_MUNICIPALITY,
        DELIVERY_AREA,
        DELIVERY_STREET,
        DELIVERY_BUILDING,
        DELIVERY_REFERENCE,
        DELIVERY_RECIPIENT,
        DELIVERY_PHONE,
        DELIVERY_INSTRUCTIONS
    )
}
