package com.walletconnect.pay

object Pay {
    data class SdkConfig(
        val baseUrl: String,
        val apiKey: String,
        val sdkName: String,
        val sdkVersion: String,
        val sdkPlatform: String
    )

    enum class PaymentStatus {
        REQUIRES_ACTION,
        PROCESSING,
        SUCCEEDED,
        FAILED,
        EXPIRED
    }

    data class AmountDisplay(
        val assetSymbol: String,
        val assetName: String,
        val decimals: Int,
        val iconUrl: String?,
        val networkName: String?
    )

    data class Amount(
        val value: String,
        val unit: String,
        val display: AmountDisplay?
    )

    data class Fee(
        val amount: Amount,
        val type: String
    )

    data class PaymentOption(
        val id: String,
        val amount: Amount,
        val estimatedTxs: Int?
    )

    data class PaymentOptionsResponse(
        val options: List<PaymentOption>
    )

    data class WalletRpcAction(
        val chainId: String,
        val method: String,
        val params: String
    )

    sealed class RequiredAction {
        data class WalletRpc(val action: WalletRpcAction) : RequiredAction()
    }

    data class SignatureValue(
        val value: String
    )

    data class SignatureResult(
        val signature: SignatureValue
    )

    data class ConfirmPaymentResponse(
        val status: PaymentStatus,
        val isFinal: Boolean,
        val pollInMs: Long?
    )

    sealed class PayError : Exception() {
        data class Http(override val message: String) : PayError()
        data class Api(override val message: String) : PayError()
        data object Timeout : PayError() {
            private fun readResolve(): Any = Timeout
            override val message: String = "Timeout: polling exceeded maximum duration"
        }
    }

    sealed class GetPaymentOptionsError : Exception() {
        data class PaymentExpired(override val message: String) : GetPaymentOptionsError()
        data class PaymentNotFound(override val message: String) : GetPaymentOptionsError()
        data class InvalidRequest(override val message: String) : GetPaymentOptionsError()
        data class OptionNotFound(override val message: String) : GetPaymentOptionsError()
        data class PaymentNotReady(override val message: String) : GetPaymentOptionsError()
        data class InvalidAccount(override val message: String) : GetPaymentOptionsError()
        data class ComplianceFailed(override val message: String) : GetPaymentOptionsError()
        data class Http(override val message: String) : GetPaymentOptionsError()
        data class InternalError(override val message: String) : GetPaymentOptionsError()
    }

    sealed class GetPaymentRequestError : Exception() {
        data class OptionNotFound(override val message: String) : GetPaymentRequestError()
        data class PaymentNotFound(override val message: String) : GetPaymentRequestError()
        data class InvalidAccount(override val message: String) : GetPaymentRequestError()
        data class Http(override val message: String) : GetPaymentRequestError()
        data class FetchError(override val message: String) : GetPaymentRequestError()
        data class InternalError(override val message: String) : GetPaymentRequestError()
    }

    sealed class ConfirmPaymentError : Exception() {
        data class PaymentNotFound(override val message: String) : ConfirmPaymentError()
        data class PaymentExpired(override val message: String) : ConfirmPaymentError()
        data class InvalidOption(override val message: String) : ConfirmPaymentError()
        data class InvalidSignature(override val message: String) : ConfirmPaymentError()
        data class RouteExpired(override val message: String) : ConfirmPaymentError()
        data class Http(override val message: String) : ConfirmPaymentError()
        data class InternalError(override val message: String) : ConfirmPaymentError()
    }
}

