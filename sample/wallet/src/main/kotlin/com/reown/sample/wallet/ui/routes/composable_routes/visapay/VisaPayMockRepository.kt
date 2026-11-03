package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mocked VisaPay backend for the demo. Every call succeeds after a short delay so the
 * happy-path onboarding flow can be shown without a real KYC provider or card issuer.
 */
object VisaPayMockRepository {
    private const val KYC_REVIEW_DELAY_MS = 4_000L
    private const val CARD_ACTIVATION_DELAY_MS = 1_200L
    private const val PERMIT2_PREPARE_DELAY_MS = 1_200L
    private const val PERMIT2_SIGN_DELAY_MS = 1_800L
    private val MOCK_PERMIT2_SIGNATURE = "0x" + "ab".repeat(65)

    private val _card = MutableStateFlow<VisaPayCard?>(null)
    val card: StateFlow<VisaPayCard?> = _card.asStateFlow()

    suspend fun submitKycApplication(application: VisaPayApplication): KycDecision {
        delay(KYC_REVIEW_DELAY_MS)
        return KycDecision.Approved(cardholderName = application.fullName)
    }

    /** Mocks fetching the Permit2 typed data that lets the card issuer pull USDC from the funding wallet. */
    suspend fun preparePermit2Approval(setup: VisaPayCardSetup): Permit2Approval {
        delay(PERMIT2_PREPARE_DELAY_MS)
        return Permit2Approval(
            token = VisaPayMocks.CARD_CURRENCY,
            owner = setup.fundingWallet,
            network = setup.network,
            dailyAllowance = setup.dailyLimit,
        )
    }

    /** Mocks the wallet signing the Permit2 approval (eth_signTypedData_v4) under the hood. */
    suspend fun signPermit2Approval(approval: Permit2Approval): String {
        delay(PERMIT2_SIGN_DELAY_MS)
        return MOCK_PERMIT2_SIGNATURE
    }

    suspend fun activateCard(cardholderName: String, setup: VisaPayCardSetup): VisaPayCard {
        delay(CARD_ACTIVATION_DELAY_MS)
        return VisaPayCard(
            cardholderName = cardholderName,
            lastDigits = VisaPayMocks.CARD_LAST_DIGITS,
            dailyLimit = setup.dailyLimit,
            fundingWallet = setup.fundingWallet,
            network = setup.network,
        ).also { _card.value = it }
    }

    fun reset() {
        _card.value = null
    }
}

object VisaPayMocks {
    const val FULL_NAME = "Alex Morgan"
    const val DATE_OF_BIRTH = "18/04/1992"
    const val NATIONALITY = "Greece"
    const val STREET = "24 Ermou Street"
    const val STREET_2 = "Floor 3"
    const val CITY = "Athens"
    const val POSTCODE = "105 63"
    const val COUNTRY = "Greece"
    const val FALLBACK_WALLET_ADDRESS = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F"
    const val CARD_CURRENCY = "USDC"
    const val CARD_LAST_DIGITS = "4821"
    const val KYC_PROVIDER = "our verification partner"

    val countries = listOf(
        "Greece", "Germany", "France", "Netherlands", "Portugal", "Spain", "United Kingdom", "United States"
    )
    val dailyLimitPresets = listOf(100, 500, 1_000, 2_500)
}

enum class VisaPayDocumentType(val label: String, val needsBack: Boolean) {
    PASSPORT("Passport", false),
    ID_CARD("ID card", true),
    DRIVING_LICENCE("Driving licence", true),
}

enum class VisaPayNetwork(val label: String) {
    BASE("Base"),
    ETHEREUM("Ethereum"),
    ARBITRUM("Arbitrum"),
}

data class VisaPayApplication(
    val fullName: String,
    val dateOfBirth: String,
    val nationality: String,
    val addressLine: String,
    val city: String,
    val postcode: String,
    val country: String,
    val documentType: VisaPayDocumentType,
)

data class VisaPayCardSetup(
    val dailyLimit: Int,
    val fundingWallet: String,
    val network: VisaPayNetwork,
)

data class Permit2Approval(
    val token: String,
    val owner: String,
    val network: VisaPayNetwork,
    val dailyAllowance: Int,
)

data class VisaPayCard(
    val cardholderName: String,
    val lastDigits: String,
    val dailyLimit: Int,
    val fundingWallet: String,
    val network: VisaPayNetwork,
)

sealed class KycDecision {
    data class Approved(val cardholderName: String) : KycDecision()
}
