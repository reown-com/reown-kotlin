package com.walletconnect.pay

import com.walletconnect.pay.Pay.RequiredAction.*
import uniffi.yttrium_wcpay.PayAmount
import uniffi.yttrium_wcpay.AmountDisplay as YttriumAmountDisplay
import uniffi.yttrium_wcpay.PaymentOption as YttriumPaymentOption
import uniffi.yttrium_wcpay.PaymentOptionsResponse as YttriumPaymentOptionsResponse
import uniffi.yttrium_wcpay.PaymentStatus as YttriumPaymentStatus
import uniffi.yttrium_wcpay.PaymentInfo as YttriumPaymentInfo
import uniffi.yttrium_wcpay.MerchantInfo as YttriumMerchantInfo
import uniffi.yttrium_wcpay.RequiredAction as YttriumRequiredAction
import uniffi.yttrium_wcpay.WalletRpcAction as YttriumWalletRpcAction
import uniffi.yttrium_wcpay.SignatureResult as YttriumSignatureResult
import uniffi.yttrium_wcpay.SignatureValue as YttriumSignatureValue
import uniffi.yttrium_wcpay.ConfirmPaymentResultResponse as YttriumConfirmPaymentResponse
import uniffi.yttrium_wcpay.GetPaymentOptionsException as YttriumGetPaymentOptionsError
import uniffi.yttrium_wcpay.GetPaymentRequestException as YttriumGetPaymentRequestError
import uniffi.yttrium_wcpay.ConfirmPaymentException as YttriumConfirmPaymentError
import uniffi.yttrium_wcpay.PayException as YttriumPayError

internal object Mappers {
    fun mapPaymentOptionsResponse(response: YttriumPaymentOptionsResponse): Pay.PaymentOptionsResponse {
        return Pay.PaymentOptionsResponse(
            info = response.info?.let { mapPaymentInfo(it) },
            options = response.options.map { mapPaymentOption(it) }
        )
    }

    private fun mapPaymentInfo(info: YttriumPaymentInfo): Pay.PaymentInfo {
        return Pay.PaymentInfo(
            status = mapPaymentStatus(info.status),
            amount = mapAmount(info.amount),
            expiresAt = info.expiresAt,
            merchant = mapMerchantInfo(info.merchant)
        )
    }

    private fun mapMerchantInfo(merchant: YttriumMerchantInfo): Pay.MerchantInfo {
        return Pay.MerchantInfo(
            name = merchant.name,
            iconUrl = merchant.iconUrl
        )
    }

    private fun mapPaymentOption(option: YttriumPaymentOption): Pay.PaymentOption {
        return Pay.PaymentOption(
            id = option.id,
            amount = mapAmount(option.amount),
            estimatedTxs = option.etaSeconds.toInt()
        )
    }

    private fun mapAmount(amount: PayAmount): Pay.Amount {
        return Pay.Amount(
            value = amount.value,
            unit = amount.unit,
            display = mapAmountDisplay(amount.display)
        )
    }

    private fun mapAmountDisplay(display: YttriumAmountDisplay): Pay.AmountDisplay {
        return Pay.AmountDisplay(
            assetSymbol = display.assetSymbol,
            assetName = display.assetName,
            decimals = display.decimals.toInt(),
            iconUrl = display.iconUrl,
            networkName = display.networkName
        )
    }

    fun mapRequiredAction(action: YttriumRequiredAction): Pay.RequiredAction {
        return when (action) {
            is YttriumRequiredAction.WalletRpc -> WalletRpc(
                mapWalletRpcAction(action.v1)
            )
        }
    }

    private fun mapWalletRpcAction(action: YttriumWalletRpcAction): Pay.WalletRpcAction {
        return Pay.WalletRpcAction(
            chainId = action.chainId,
            method = action.method,
            params = action.params
        )
    }

    // ==================== Confirm Payment Mapping ====================

    fun mapConfirmPaymentResponse(response: YttriumConfirmPaymentResponse): Pay.ConfirmPaymentResponse {
        return Pay.ConfirmPaymentResponse(
            status = mapPaymentStatus(response.status),
            isFinal = response.isFinal,
            pollInMs = response.pollInMs
        )
    }

    private fun mapPaymentStatus(status: YttriumPaymentStatus): Pay.PaymentStatus {
        return when (status) {
            YttriumPaymentStatus.REQUIRES_ACTION -> Pay.PaymentStatus.REQUIRES_ACTION
            YttriumPaymentStatus.PROCESSING -> Pay.PaymentStatus.PROCESSING
            YttriumPaymentStatus.SUCCEEDED -> Pay.PaymentStatus.SUCCEEDED
            YttriumPaymentStatus.FAILED -> Pay.PaymentStatus.FAILED
            YttriumPaymentStatus.EXPIRED -> Pay.PaymentStatus.EXPIRED
        }
    }

    // ==================== Signature Mapping ====================

    fun mapSignatureResultToYttrium(result: Pay.SignatureResult): YttriumSignatureResult {
        return YttriumSignatureResult(
            signature = YttriumSignatureValue(value = result.signature.value)
        )
    }

    // ==================== Error Mapping ====================

    fun mapGetPaymentOptionsError(error: YttriumGetPaymentOptionsError): Pay.GetPaymentOptionsError {
        return when (error) {
            is YttriumGetPaymentOptionsError.PaymentExpired ->
                Pay.GetPaymentOptionsError.PaymentExpired(error.message)

            is YttriumGetPaymentOptionsError.PaymentNotFound ->
                Pay.GetPaymentOptionsError.PaymentNotFound(error.message)

            is YttriumGetPaymentOptionsError.InvalidRequest ->
                Pay.GetPaymentOptionsError.InvalidRequest(error.message)

            is YttriumGetPaymentOptionsError.OptionNotFound ->
                Pay.GetPaymentOptionsError.OptionNotFound(error.message)

            is YttriumGetPaymentOptionsError.PaymentNotReady ->
                Pay.GetPaymentOptionsError.PaymentNotReady(error.message)

            is YttriumGetPaymentOptionsError.InvalidAccount ->
                Pay.GetPaymentOptionsError.InvalidAccount(error.message)

            is YttriumGetPaymentOptionsError.ComplianceFailed ->
                Pay.GetPaymentOptionsError.ComplianceFailed(error.message)

            is YttriumGetPaymentOptionsError.Http ->
                Pay.GetPaymentOptionsError.Http(error.message)

            is YttriumGetPaymentOptionsError.InternalException ->
                Pay.GetPaymentOptionsError.InternalError(error.message)
        }
    }

    fun mapGetPaymentRequestError(error: YttriumGetPaymentRequestError): Pay.GetPaymentRequestError {
        return when (error) {
            is YttriumGetPaymentRequestError.OptionNotFound ->
                Pay.GetPaymentRequestError.OptionNotFound(error.message)

            is YttriumGetPaymentRequestError.PaymentNotFound ->
                Pay.GetPaymentRequestError.PaymentNotFound(error.message)

            is YttriumGetPaymentRequestError.InvalidAccount ->
                Pay.GetPaymentRequestError.InvalidAccount(error.message)

            is YttriumGetPaymentRequestError.Http ->
                Pay.GetPaymentRequestError.Http(error.message)

            is YttriumGetPaymentRequestError.FetchException ->
                Pay.GetPaymentRequestError.FetchError(error.message)

            is YttriumGetPaymentRequestError.InternalException ->
                Pay.GetPaymentRequestError.InternalError(error.message)
        }
    }

    fun mapConfirmPaymentError(error: YttriumConfirmPaymentError): Pay.ConfirmPaymentError {
        return when (error) {
            is YttriumConfirmPaymentError.PaymentNotFound ->
                Pay.ConfirmPaymentError.PaymentNotFound(error.message)

            is YttriumConfirmPaymentError.PaymentExpired ->
                Pay.ConfirmPaymentError.PaymentExpired(error.message)

            is YttriumConfirmPaymentError.InvalidOption ->
                Pay.ConfirmPaymentError.InvalidOption(error.message)

            is YttriumConfirmPaymentError.InvalidSignature ->
                Pay.ConfirmPaymentError.InvalidSignature(error.message)

            is YttriumConfirmPaymentError.RouteExpired ->
                Pay.ConfirmPaymentError.RouteExpired(error.message)

            is YttriumConfirmPaymentError.Http ->
                Pay.ConfirmPaymentError.Http(error.message)

            is YttriumConfirmPaymentError.InternalException ->
                Pay.ConfirmPaymentError.InternalError(error.message)
        }
    }

    fun mapPayError(error: YttriumPayError): Pay.PayError {
        return when (error) {
            is YttriumPayError.Http -> Pay.PayError.Http(error.message)
            is YttriumPayError.Api -> Pay.PayError.Api(error.message)
            is YttriumPayError.Timeout -> Pay.PayError.Timeout
        }
    }
}

