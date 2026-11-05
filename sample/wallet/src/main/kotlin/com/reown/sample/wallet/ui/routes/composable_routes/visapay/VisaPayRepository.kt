package com.reown.sample.wallet.ui.routes.composable_routes.visapay

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VisaPay demo backend, backed by the hackathon Visa Pay services (see [VisaPayServiceClient]):
 * KYC on the bin-sponsor mock, then card issuing, limits, payments and activity on vpay-server.
 *
 * Still mocked, because the services don't cover them yet: the merchant behind a QR code, and the
 * Permit2 spending allowance the wallet signs.
 */
object VisaPayRepository {
    private const val KYC_POLL_INTERVAL_MS = 1_000L
    private const val KYC_POLL_ATTEMPTS = 30
    private const val PERMIT2_PREPARE_DELAY_MS = 1_200L
    private const val PERMIT2_SIGN_DELAY_MS = 1_800L
    private const val ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    private val MOCK_PERMIT2_SIGNATURE = "0x" + "ab".repeat(65)

    private val _card = MutableStateFlow<VisaPayCard?>(null)
    val card: StateFlow<VisaPayCard?> = _card.asStateFlow()

    private val _payments = MutableStateFlow<List<VisaPayPayment>>(emptyList())
    val payments: StateFlow<List<VisaPayPayment>> = _payments.asStateFlow()

    /** Categories are not stored by the server, so the wallet remembers them per Pismo transaction. */
    private val categories = mutableMapOf<String, String>()

    /** Picks the merchant behind a scanned QR code; creating merchant payments is not in the services yet. */
    fun createPaymentRequest(): VisaPayPaymentRequest = VisaPayMocks.merchants.random()

    /**
     * Sends the cardholder to the BIN sponsor's KYC and waits for its decision, then issues the card on
     * vpay-server (Visa Pay enrollment) when approved.
     */
    suspend fun submitKycApplication(application: VisaPayApplication): KycDecision {
        val externalId = "wc" + randomId(12)
        var record = VisaPayServiceClient.submitKyc(
            KycRequest(
                externalId = externalId,
                fullName = application.fullName,
                dateOfBirth = application.dateOfBirthIso,
                country = application.countryCode,
                documentNumber = application.documentNumber,
                email = application.email,
            )
        )
        repeat(KYC_POLL_ATTEMPTS) {
            if (record.status == KycStatus.APPROVED || record.status == KycStatus.REJECTED) return@repeat
            delay(KYC_POLL_INTERVAL_MS)
            record = VisaPayServiceClient.getKyc(externalId)
        }
        return when (record.status) {
            KycStatus.APPROVED -> {
                val card = VisaPayServiceClient.createCard(userRef = externalId, deviceId = "android-" + randomId(16))
                KycDecision.Approved(cardholderName = application.fullName, issuedCard = IssuedCard(card.id, card.last4))
            }
            KycStatus.REJECTED -> KycDecision.Rejected(record.reason ?: "The bank could not verify your identity.")
            KycStatus.PENDING, KycStatus.REVIEW -> KycDecision.InReview
        }
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

    /** Sets the daily limit on vpay-server and fetches the tap-to-pay keys, then makes the card active. */
    suspend fun activateCard(issuedCard: IssuedCard, cardholderName: String, setup: VisaPayCardSetup): VisaPayCard {
        val serverCard = VisaPayServiceClient.setDailyLimit(issuedCard.id, setup.dailyLimit)
        VisaPayServiceClient.replenishTapKeys(issuedCard.id)
        val card = VisaPayCard(
            id = serverCard.id,
            cardholderName = cardholderName,
            lastDigits = serverCard.last4.ifBlank { issuedCard.last4 },
            dailyLimit = setup.dailyLimit,
            fundingWallet = setup.fundingWallet,
            network = setup.network,
            leftToSpend = serverCard.dailyLimit,
        )
        _card.value = card
        _payments.value = emptyList()
        categories.clear()
        refreshActivity()
        return card
    }

    /** Pays with the card: vpay-server has the visa-pay mock send the payment to its authorization webhook. */
    suspend fun pay(request: VisaPayPaymentRequest): PaymentResult {
        val card = _card.value ?: throw VisaPayServiceException("No active Visa Pay card")
        val result = VisaPayServiceClient.demoPay(card.id, request.amount, request.merchant)
        result.transactionId?.let { categories[it] = if (request.isContactless) "${request.category} · Contactless" else request.category }
        runCatching { refreshActivity() }
        return result
    }

    /** Reloads what is left today and recent authorizations (declines included) from vpay-server. */
    suspend fun refreshActivity() {
        val card = _card.value ?: return
        val activity = VisaPayServiceClient.activity(card.id)
        _card.value = _card.value?.copy(leftToSpend = activity.leftToday)
        _payments.value = activity.authorizations.map { authorization ->
            VisaPayPayment(
                merchant = authorization.merchant,
                category = categories[authorization.transactionId] ?: "Card payment",
                amount = authorization.amount,
                responseCode = authorization.responseCode,
            )
        }
    }

    /** Card number, expiry and a new one-time code for an online checkout. Each call invalidates the last code. */
    suspend fun cardDetails(): VisaPayCardDetails {
        val card = _card.value ?: throw VisaPayServiceException("No active Visa Pay card")
        return VisaPayServiceClient.cardDetails(card.id)
    }

    private var lastTapReference: String? = null

    /**
     * Returns false when this POS order was already handled. Android can deliver the same NFC
     * tag more than once during a single tap, and each delivery must not open a new payment.
     */
    fun markTapHandled(reference: String?): Boolean {
        if (reference != null && reference == lastTapReference) return false
        lastTapReference = reference
        return true
    }

    /** Forgets the card locally. The next onboarding issues a new card with a new device id, as vpay-server requires. */
    fun reset() {
        _card.value = null
        _payments.value = emptyList()
        categories.clear()
    }

    private fun randomId(length: Int): String = (1..length).map { ID_ALPHABET.random() }.joinToString("")
}

object VisaPayMocks {
    const val FULL_NAME = "Alex Morgan"
    const val EMAIL = "alex.morgan@example.com"
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
    const val DEMO_DOCUMENT_PREFIX = "P"
    const val KYC_PROVIDER = "our verification partner"

    /** Country names shown in the forms, with the ISO 3166 code the bank's KYC expects. */
    val countryCodes = linkedMapOf(
        "Greece" to "GR",
        "Germany" to "DE",
        "France" to "FR",
        "Netherlands" to "NL",
        "Portugal" to "PT",
        "Spain" to "ES",
        "United Kingdom" to "GB",
        "United States" to "US",
    )
    val countries: List<String> = countryCodes.keys.toList()
    val dailyLimitPresets = listOf(100, 500, 1_000, 2_500)

    val merchants = listOf(
        VisaPayPaymentRequest("Blue Bottle Coffee", "Coffee shop", 4.75),
        VisaPayPaymentRequest("Athens Corner Market", "Groceries", 18.40),
        VisaPayPaymentRequest("Metro Line 3", "Transport", 2.10),
        VisaPayPaymentRequest("Plaka Bistro", "Restaurant", 36.90),
    )
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
    val email: String,
    /** As typed in the form: DD/MM/YYYY. */
    val dateOfBirth: String,
    val nationality: String,
    val addressLine: String,
    val city: String,
    val postcode: String,
    val country: String,
    val documentType: VisaPayDocumentType,
    /** The ID capture is mocked, so the document number is generated. */
    val documentNumber: String,
) {
    /** The bank's KYC expects YYYY-MM-DD. */
    val dateOfBirthIso: String
        get() = dateOfBirth.split("/").takeIf { it.size == 3 }?.let { (day, month, year) ->
            "${year.trim()}-${month.trim().padStart(2, '0')}-${day.trim().padStart(2, '0')}"
        } ?: dateOfBirth

    val countryCode: String get() = VisaPayMocks.countryCodes[nationality] ?: nationality.take(2).uppercase()
}

/** A card vpay-server issued after KYC, not yet activated with a daily limit. */
data class IssuedCard(val id: String, val last4: String)

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
    /** vpay-server card id, also the Visa Pay account id. */
    val id: String,
    val cardholderName: String,
    val lastDigits: String,
    val dailyLimit: Int,
    val fundingWallet: String,
    val network: VisaPayNetwork,
    /** What is left of today's limit, as vpay-server reports it. */
    val leftToSpend: Double,
)

data class VisaPayPaymentRequest(
    val merchant: String,
    val category: String,
    val amount: Double,
    /** Amount as the merchant shows it (e.g. "$12.50"); USDC is charged 1:1 in this mock. */
    val fiatLabel: String? = null,
    val reference: String? = null,
    /** True when the request came from a tap on the sample POS over NFC. */
    val isContactless: Boolean = false,
)

data class VisaPayPayment(
    val merchant: String,
    val category: String,
    val amount: Double,
    /** Visa response code from the authorization: 00 approved, 51 over the daily limit, others declined. */
    val responseCode: String = RESPONSE_APPROVED,
) {
    val isApproved: Boolean get() = responseCode == RESPONSE_APPROVED
}

const val RESPONSE_APPROVED = "00"

/** Short, user-facing reason for a Visa response code. */
fun declineReason(responseCode: String): String = when (responseCode) {
    "51" -> "Over your daily limit"
    "05" -> "Declined by the issuer"
    "78" -> "Card is blocked"
    "91" -> "Visa Pay didn't answer in time"
    "N0" -> "Decided by Visa on our behalf"
    else -> "Declined (code $responseCode)"
}

/** Formats a USDC amount, e.g. 1,000 USDC or 4.75 USDC. */
fun formatUsdc(amount: Double): String {
    val number = if (amount % 1.0 == 0.0) "%,.0f".format(amount) else "%,.2f".format(amount)
    return "$number ${VisaPayMocks.CARD_CURRENCY}"
}

sealed class KycDecision {
    data class Approved(val cardholderName: String, val issuedCard: IssuedCard) : KycDecision()
    data class Rejected(val reason: String) : KycDecision()
    data object InReview : KycDecision()
}
