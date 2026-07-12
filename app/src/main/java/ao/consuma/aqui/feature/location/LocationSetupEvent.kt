package ao.consuma.aqui.feature.location

sealed interface LocationSetupEvent {
    data object UseCurrentLocation : LocationSetupEvent
    data object ChooseManually : LocationSetupEvent
    data object Skip : LocationSetupEvent
    data class SearchChanged(val value: String) : LocationSetupEvent
    data class LocationSelected(val id: String) : LocationSetupEvent
    data object Confirm : LocationSetupEvent
    data object ChooseAnother : LocationSetupEvent
    data object BackToExplanation : LocationSetupEvent
}
