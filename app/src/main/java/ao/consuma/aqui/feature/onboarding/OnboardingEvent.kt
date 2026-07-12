package ao.consuma.aqui.feature.onboarding

sealed interface OnboardingEvent {
    data object Continue : OnboardingEvent
    data object Skip : OnboardingEvent
    data object Finish : OnboardingEvent
    data class PageChanged(val page: Int) : OnboardingEvent
}
