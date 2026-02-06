package com.walletconnect.pay

object Pay {
    data class SdkConfig(
        val apiKey: String? = null,
        val appId: String? = null,
        val packageName: String,
        val baseUrl: String = "https://api.pay.walletconnect.com",
        val clientId: String? = null
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
        val networkName: String?,
        val networkIconUrl: String?
    )

    data class Amount(
        val value: String,
        val unit: String,
        val display: AmountDisplay?
    )

    data class PaymentOption(
        val id: String,
        val amount: Amount,
        val account: String,
        val estimatedTxs: Int?
    )

    data class MerchantInfo(
        val name: String,
        val iconUrl: String?
    )

    data class PaymentInfo(
        val status: PaymentStatus,
        val amount: Amount,
        val expiresAt: Long,
        val merchant: MerchantInfo
    )

    data class PaymentOptionsResponse(
        val info: PaymentInfo?,
        val options: List<PaymentOption>,
        val paymentId: String,
        val collectDataAction: CollectDataAction?
    )

    data class WalletRpcAction(
        val chainId: String,
        val method: String,
        val params: String
    )

    enum class CollectDataFieldType {
        TEXT,
        DATE,
        CHECKBOX
    }

    data class CollectDataField(
        val id: String,
        val name: String,
        val fieldType: CollectDataFieldType,
        val required: Boolean
    )

    data class CollectDataAction(
        @Deprecated("Use url for WebView-based data collection or schema to parse field requirements")
        val fields: List<CollectDataField>,
        val url: String?,
        val schema: String?
    )

    sealed class RequiredAction {
        data class WalletRpc(val action: WalletRpcAction) : RequiredAction()
    }

    data class CollectDataFieldResult(
        val id: String,
        val value: String
    )

    data class PaymentResultInfo(
        val txId: String,
        val optionAmount: Amount
    )

    data class ConfirmPaymentResponse(
        val status: PaymentStatus,
        val isFinal: Boolean,
        val pollInMs: Long?,
        val info: PaymentResultInfo?
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
        data class InvalidPaymentLink(override val message: String) : GetPaymentOptionsError()
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
        data class UnsupportedMethod(override val message: String) : ConfirmPaymentError()
    }
}

