package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VisaPayStep(val number: Int?) {
    DETAILS(1),
    ADDRESS(2),
    IDENTITY(3),
    REVIEW(4),
    IN_REVIEW(null),
    APPROVED(null),
    SETUP(null),
    ACTIVATING(null),
}

/** Background stages shown on the activation loading screen, in order. */
enum class VisaPayActivationStage { PREPARING_PERMIT2, SIGNING_PERMIT2, ACTIVATING_CARD, DONE }

enum class VisaPayCapture { FRONT, BACK, SELFIE }

data class VisaPayOnboardingState(
    val step: VisaPayStep = VisaPayStep.DETAILS,
    // Personal details and address are prefilled with mock data so the demo is quick to click through.
    val fullName: String = VisaPayMocks.FULL_NAME,
    val dateOfBirth: String = VisaPayMocks.DATE_OF_BIRTH,
    val nationality: String = VisaPayMocks.NATIONALITY,
    val street: String = VisaPayMocks.STREET,
    val street2: String = VisaPayMocks.STREET_2,
    val city: String = VisaPayMocks.CITY,
    val postcode: String = VisaPayMocks.POSTCODE,
    val country: String = VisaPayMocks.COUNTRY,
    val documentType: VisaPayDocumentType = VisaPayDocumentType.PASSPORT,
    val captures: Set<VisaPayCapture> = emptySet(),
    val consent: Boolean = false,
    val dailyLimit: String = "",
    val fundingWallet: String = "",
    val network: VisaPayNetwork = VisaPayNetwork.BASE,
    val activationStage: VisaPayActivationStage = VisaPayActivationStage.PREPARING_PERMIT2,
) {
    val requiredCaptures: List<VisaPayCapture>
        get() = buildList {
            add(VisaPayCapture.FRONT)
            if (documentType.needsBack) add(VisaPayCapture.BACK)
            add(VisaPayCapture.SELFIE)
        }

    val isDetailsValid: Boolean get() = fullName.isNotBlank() && dateOfBirth.isNotBlank() && nationality.isNotBlank()
    val isAddressValid: Boolean get() = street.isNotBlank() && city.isNotBlank() && postcode.isNotBlank() && country.isNotBlank()
    val isIdentityValid: Boolean get() = captures.containsAll(requiredCaptures)
    val dailyLimitValue: Int get() = dailyLimit.toIntOrNull() ?: 0
    val isWalletValid: Boolean get() = WALLET_REGEX.matches(fundingWallet.trim())
    val isSetupValid: Boolean get() = dailyLimitValue > 0 && isWalletValid

    val addressLine: String get() = listOf(street, street2).filter { it.isNotBlank() }.joinToString(", ")
    val cityLine: String get() = listOf(postcode, city, country).filter { it.isNotBlank() }.joinToString(" ")

    private companion object {
        val WALLET_REGEX = Regex("^0x[a-fA-F0-9]{40}$")
    }
}

class VisaPayOnboardingViewModel : ViewModel() {
    private val _state = MutableStateFlow(VisaPayOnboardingState())
    val state: StateFlow<VisaPayOnboardingState> = _state.asStateFlow()

    val sampleWalletAddress: String
        get() = runCatching { EthAccountDelegate.address }.getOrNull()?.takeIf { it.startsWith("0x") }
            ?: VisaPayMocks.FALLBACK_WALLET_ADDRESS

    fun goTo(step: VisaPayStep) = _state.update { it.copy(step = step) }

    /** Returns false when there is no previous step inside the flow and the caller should leave it. */
    fun back(): Boolean {
        val previous = when (_state.value.step) {
            VisaPayStep.DETAILS -> return false
            VisaPayStep.ADDRESS -> VisaPayStep.DETAILS
            VisaPayStep.IDENTITY -> VisaPayStep.ADDRESS
            VisaPayStep.REVIEW -> VisaPayStep.IDENTITY
            VisaPayStep.IN_REVIEW, VisaPayStep.APPROVED, VisaPayStep.ACTIVATING -> return true
            VisaPayStep.SETUP -> VisaPayStep.APPROVED
        }
        goTo(previous)
        return true
    }

    fun onFullNameChange(value: String) = _state.update { it.copy(fullName = value) }
    fun onDateOfBirthChange(value: String) = _state.update { it.copy(dateOfBirth = value) }
    fun onNationalityChange(value: String) = _state.update { it.copy(nationality = value) }
    fun onStreetChange(value: String) = _state.update { it.copy(street = value) }
    fun onStreet2Change(value: String) = _state.update { it.copy(street2 = value) }
    fun onCityChange(value: String) = _state.update { it.copy(city = value) }
    fun onPostcodeChange(value: String) = _state.update { it.copy(postcode = value) }
    fun onCountryChange(value: String) = _state.update { it.copy(country = value) }

    fun onDocumentTypeChange(type: VisaPayDocumentType) =
        _state.update { it.copy(documentType = type, captures = it.captures - VisaPayCapture.FRONT - VisaPayCapture.BACK) }

    /** Mocked camera: tapping a capture slot marks the photo as taken. */
    fun capture(capture: VisaPayCapture) = _state.update { it.copy(captures = it.captures + capture) }
    fun retake(capture: VisaPayCapture) = _state.update { it.copy(captures = it.captures - capture) }

    fun onConsentChange(value: Boolean) = _state.update { it.copy(consent = value) }

    fun submitForReview() {
        val current = _state.value
        _state.update { it.copy(step = VisaPayStep.IN_REVIEW) }
        viewModelScope.launch {
            when (VisaPayMockRepository.submitKycApplication(current.toApplication())) {
                is KycDecision.Approved -> goTo(VisaPayStep.APPROVED)
            }
        }
    }

    fun onDailyLimitChange(value: String) =
        _state.update { it.copy(dailyLimit = value.filter(Char::isDigit).take(MAX_LIMIT_DIGITS)) }

    fun onDailyLimitPreset(value: Int) = _state.update { it.copy(dailyLimit = value.toString()) }
    fun onFundingWalletChange(value: String) = _state.update { it.copy(fundingWallet = value) }
    fun useSampleWalletAddress() = _state.update { it.copy(fundingWallet = sampleWalletAddress) }
    fun onNetworkChange(network: VisaPayNetwork) = _state.update { it.copy(network = network) }

    /**
     * Mocks what happens under the hood after the user taps "Activate card": the wallet signs a
     * Permit2 approval so the card issuer can pull USDC up to the daily limit, then the card is activated.
     */
    fun activateCard() {
        val current = _state.value
        if (!current.isSetupValid || current.step == VisaPayStep.ACTIVATING) return
        val setup = VisaPayCardSetup(
            dailyLimit = current.dailyLimitValue,
            fundingWallet = current.fundingWallet.trim(),
            network = current.network,
        )
        _state.update { it.copy(step = VisaPayStep.ACTIVATING, activationStage = VisaPayActivationStage.PREPARING_PERMIT2) }
        viewModelScope.launch {
            val approval = VisaPayMockRepository.preparePermit2Approval(setup)
            setActivationStage(VisaPayActivationStage.SIGNING_PERMIT2)
            VisaPayMockRepository.signPermit2Approval(approval)
            setActivationStage(VisaPayActivationStage.ACTIVATING_CARD)
            VisaPayMockRepository.activateCard(cardholderName = current.fullName, setup = setup)
            setActivationStage(VisaPayActivationStage.DONE)
        }
    }

    private fun setActivationStage(stage: VisaPayActivationStage) = _state.update { it.copy(activationStage = stage) }

    private fun VisaPayOnboardingState.toApplication() = VisaPayApplication(
        fullName = fullName,
        dateOfBirth = dateOfBirth,
        nationality = nationality,
        addressLine = addressLine,
        city = city,
        postcode = postcode,
        country = country,
        documentType = documentType,
    )

    private companion object {
        const val MAX_LIMIT_DIGITS = 6
    }
}
