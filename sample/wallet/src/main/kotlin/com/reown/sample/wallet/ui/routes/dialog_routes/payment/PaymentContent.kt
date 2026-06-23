package com.reown.sample.wallet.ui.routes.dialog_routes.payment

/**
 * Centralized presentation copy for the WalletConnect Pay flow result and
 * loading screens.
 *
 * The view-model carries only semantic state (success/error, the classified
 * [PaymentErrorType], the raw error message, the token being set up); the view
 * derives every user-facing string from the two pure helpers below. Keeping the
 * copy here — rather than scattered across [PaymentRoute] and [PaymentViewModel]
 * — means the wording for an outcome lives in exactly one place.
 */

/** What the result screen's primary action does. */
enum class ResultActionKind { CLOSE, SCAN_QR }

/** Full presentation for a result (success or error) screen. */
data class PaymentResultContent(
    val title: String,
    // Errors show a secondary description line; success shows the title only.
    val description: String? = null,
    val iconTestId: String,
    val actionLabel: String,
    val actionKind: ResultActionKind,
)

private const val SUCCESS_DEFAULT_TITLE = "Payment confirmed"
private const val GENERIC_ERROR_DESCRIPTION =
    "No funds were moved. Try again, or pay with a different asset from checkout."

/**
 * Resolves the full presentation for a result screen.
 *
 * @param isSuccess       Whether the payment succeeded.
 * @param errorType       The classified error (ignored when [isSuccess]).
 * @param successSummary  Dynamic success summary, e.g. "You've paid 5.00 USDT to Acme".
 * @param rawErrorMessage Raw error message used as the generic-error fallback.
 */
fun getResultContent(
    isSuccess: Boolean,
    errorType: PaymentErrorType?,
    successSummary: String? = null,
    rawErrorMessage: String? = null,
): PaymentResultContent {
    if (isSuccess) {
        return PaymentResultContent(
            title = successSummary?.takeIf { it.isNotBlank() } ?: SUCCESS_DEFAULT_TITLE,
            iconTestId = "pay-result-success-icon",
            actionLabel = "Done",
            actionKind = ResultActionKind.CLOSE,
        )
    }

    return when (errorType ?: PaymentErrorType.GENERIC) {
        PaymentErrorType.INSUFFICIENT_FUNDS -> PaymentResultContent(
            title = "Not enough funds in your wallet",
            description = "This wallet doesn't have enough funds on the supported networks. Add funds, or pay with a different asset.",
            iconTestId = "pay-result-insufficient-funds-icon",
            actionLabel = "Got it",
            actionKind = ResultActionKind.CLOSE,
        )

        PaymentErrorType.EXPIRED -> PaymentResultContent(
            title = "Payment request expired",
            description = "Ask the merchant to create a new payment, then try again.",
            iconTestId = "pay-result-expired-icon",
            actionLabel = "Scan a new QR code",
            actionKind = ResultActionKind.SCAN_QR,
        )

        PaymentErrorType.CANCELLED -> PaymentResultContent(
            title = "Payment request cancelled",
            description = "Ask the merchant to create a new payment, then try again.",
            iconTestId = "pay-result-cancelled-icon",
            actionLabel = "Scan a new QR code",
            actionKind = ResultActionKind.SCAN_QR,
        )

        PaymentErrorType.NOT_FOUND -> PaymentResultContent(
            title = "Payment request not found",
            description = "This payment link isn't valid, or it's already been completed.",
            iconTestId = "pay-result-error-icon",
            actionLabel = "Close",
            actionKind = ResultActionKind.CLOSE,
        )

        PaymentErrorType.GENERIC -> PaymentResultContent(
            title = "Payment didn't go through",
            description = rawErrorMessage?.takeIf { it.isNotBlank() } ?: GENERIC_ERROR_DESCRIPTION,
            iconTestId = "pay-result-error-icon",
            actionLabel = "Close",
            actionKind = ResultActionKind.CLOSE,
        )
    }
}

/** Steps that render the loading screen. Token setup happens during [CONFIRMING]. */
enum class LoadingStep { PREPARING, CONFIRMING }

/** Full presentation for a loading screen. */
data class PaymentLoadingContent(
    val message: String,
    // Secondary line shown only while setting up a token for the first time.
    val note: String? = null,
)

/**
 * Resolves the loading copy for a [step]. When [setupTokenSymbol] is non-null
 * the wallet is performing a one-time token setup (e.g. the USDT Permit2
 * approve), which takes precedence over the step default.
 */
fun getLoadingContent(
    step: LoadingStep,
    setupTokenSymbol: String? = null,
): PaymentLoadingContent {
    if (setupTokenSymbol != null) {
        return PaymentLoadingContent(
            message = "Setting up $setupTokenSymbol",
            note = "This usually takes a few seconds. Future $setupTokenSymbol payments will skip this step.",
        )
    }

    return PaymentLoadingContent(
        message = when (step) {
            LoadingStep.CONFIRMING -> "Confirming your payment…"
            LoadingStep.PREPARING -> "Preparing your payment…"
        },
    )
}
