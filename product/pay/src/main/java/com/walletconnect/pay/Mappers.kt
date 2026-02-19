package com.walletconnect.pay

import com.walletconnect.pay.Pay.ConfirmPaymentError.*
import com.walletconnect.pay.Pay.GetPaymentOptionsError.*
import com.walletconnect.pay.Pay.GetPaymentRequestError.*
import com.walletconnect.pay.Pay.PayError.*
import com.walletconnect.pay.Pay.RequiredAction.*
import uniffi.yttrium_wcpay.PayAmount
import uniffi.yttrium_wcpay.AmountDisplay as YttriumAmountDisplay
import uniffi.yttrium_wcpay.PaymentOption as YttriumPaymentOption
import uniffi.yttrium_wcpay.PaymentOptionsResponse as YttriumPaymentOptionsResponse
import uniffi.yttrium_wcpay.PaymentStatus as YttriumPaymentStatus
import uniffi.yttrium_wcpay.PaymentInfo as YttriumPaymentInfo
import uniffi.yttrium_wcpay.MerchantInfo as YttriumMerchantInfo
import uniffi.yttrium_wcpay.Action as YttriumRequiredAction
import uniffi.yttrium_wcpay.WalletRpcAction as YttriumWalletRpcAction
import uniffi.yttrium_wcpay.CollectDataAction as YttriumCollectDataAction
import uniffi.yttrium_wcpay.CollectDataField as YttriumCollectDataField
import uniffi.yttrium_wcpay.CollectDataFieldType as YttriumCollectDataFieldType
import uniffi.yttrium_wcpay.ConfirmPaymentResultResponse as YttriumConfirmPaymentResponse
import uniffi.yttrium_wcpay.CollectDataFieldResult as YttriumCollectDataFieldResult
import uniffi.yttrium_wcpay.PaymentResultInfo as YttriumPaymentResultInfo
import uniffi.yttrium_wcpay.GetPaymentOptionsException as YttriumGetPaymentOptionsError
import uniffi.yttrium_wcpay.GetPaymentRequestException as YttriumGetPaymentRequestError
import uniffi.yttrium_wcpay.ConfirmPaymentException as YttriumConfirmPaymentError
import uniffi.yttrium_wcpay.PayException as YttriumPayError

internal object Mappers {
    fun mapPaymentOptionsResponse(response: YttriumPaymentOptionsResponse): Pay.PaymentOptionsResponse {
        return Pay.PaymentOptionsResponse(
            info = response.info?.let { mapPaymentInfo(it) },
            options = response.options.map { mapPaymentOption(it) },
            paymentId = response.paymentId,
            collectDataAction = response.collectData?.let { mapCollectDataAction(it) }
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
            estimatedTxs = option.etaS.coerceAtMost(Int.MAX_VALUE.toULong().toLong()).toInt(),
            account = option.account,
            collectData = option.collectData?.let { mapCollectDataAction(it) }
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
            networkName = display.networkName,
            networkIconUrl = display.networkIconUrl
        )
    }

    fun mapRequiredAction(action: YttriumRequiredAction): Pay.RequiredAction {
        return when (action) {
            else -> WalletRpc(mapWalletRpcAction(action.walletRpc))
        }
    }

    private fun mapWalletRpcAction(action: YttriumWalletRpcAction): Pay.WalletRpcAction {
        return Pay.WalletRpcAction(
            chainId = action.chainId,
            method = action.method,
            params = action.params
        )
    }

    private fun mapCollectDataAction(action: YttriumCollectDataAction): Pay.CollectDataAction {
        return Pay.CollectDataAction(
            fields = action.fields.map { mapCollectDataField(it) },
            url = action.url,
            schema = action.schema
        )
    }

    private fun mapCollectDataField(field: YttriumCollectDataField): Pay.CollectDataField {
        return Pay.CollectDataField(
            id = field.id,
            name = field.name,
            fieldType = mapCollectDataFieldType(field.fieldType),
            required = field.required
        )
    }

    private fun mapCollectDataFieldType(type: YttriumCollectDataFieldType): Pay.CollectDataFieldType {
        return when (type) {
            YttriumCollectDataFieldType.TEXT -> Pay.CollectDataFieldType.TEXT
            YttriumCollectDataFieldType.DATE -> Pay.CollectDataFieldType.DATE
            YttriumCollectDataFieldType.CHECKBOX -> Pay.CollectDataFieldType.CHECKBOX
        }
    }

    fun mapConfirmPaymentResponse(response: YttriumConfirmPaymentResponse): Pay.ConfirmPaymentResponse {
        return Pay.ConfirmPaymentResponse(
            status = mapPaymentStatus(response.status),
            isFinal = response.isFinal,
            pollInMs = response.pollInMs,
            info = response.info?.let { mapPaymentResultInfo(it) }
        )
    }

    private fun mapPaymentResultInfo(info: YttriumPaymentResultInfo): Pay.PaymentResultInfo {
        return Pay.PaymentResultInfo(
            txId = info.txId,
            optionAmount = mapAmount(info.optionAmount)
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

    fun mapCollectDataFieldResultToYttrium(result: Pay.CollectDataFieldResult): YttriumCollectDataFieldResult {
        return YttriumCollectDataFieldResult(id = result.id, value = result.value)
    }

    fun mapGetPaymentOptionsError(error: YttriumGetPaymentOptionsError): Pay.GetPaymentOptionsError {
        return when (error) {
            is YttriumGetPaymentOptionsError.PaymentExpired ->
                Pay.GetPaymentOptionsError.PaymentExpired(error.v1)

            is YttriumGetPaymentOptionsError.PaymentNotFound ->
                Pay.GetPaymentOptionsError.PaymentNotFound(error.v1)

            is YttriumGetPaymentOptionsError.InvalidRequest ->
                InvalidRequest(error.v1)

            is YttriumGetPaymentOptionsError.OptionNotFound ->
                Pay.GetPaymentOptionsError.OptionNotFound(error.v1)

            is YttriumGetPaymentOptionsError.PaymentNotReady ->
                PaymentNotReady(error.v1)

            is YttriumGetPaymentOptionsError.InvalidAccount ->
                Pay.GetPaymentOptionsError.InvalidAccount(error.v1)

            is YttriumGetPaymentOptionsError.ComplianceFailed ->
                ComplianceFailed(error.v1)

            is YttriumGetPaymentOptionsError.Http ->
                Pay.GetPaymentOptionsError.Http(error.v1)

            is YttriumGetPaymentOptionsError.InternalException ->
                Pay.GetPaymentOptionsError.InternalError(error.v1)
            is YttriumGetPaymentOptionsError.ConnectionFailed -> Pay.GetPaymentOptionsError.Http(error.v1)
            is YttriumGetPaymentOptionsError.NoConnection -> Pay.GetPaymentOptionsError.Http(error.v1)
            is YttriumGetPaymentOptionsError.RequestTimeout -> Pay.GetPaymentOptionsError.Http(error.v1)
        }
    }

    fun mapGetPaymentRequestError(error: YttriumGetPaymentRequestError): Pay.GetPaymentRequestError {
        return when (error) {
            is YttriumGetPaymentRequestError.OptionNotFound ->
                Pay.GetPaymentRequestError.OptionNotFound(error.v1)

            is YttriumGetPaymentRequestError.PaymentNotFound ->
                Pay.GetPaymentRequestError.PaymentNotFound(error.v1)

            is YttriumGetPaymentRequestError.InvalidAccount ->
                Pay.GetPaymentRequestError.InvalidAccount(error.v1)

            is YttriumGetPaymentRequestError.Http ->
                Pay.GetPaymentRequestError.Http(error.v1)

            is YttriumGetPaymentRequestError.FetchException ->
                FetchError(error.v1)

            is YttriumGetPaymentRequestError.InternalException ->
                Pay.GetPaymentRequestError.InternalError(error.v1)

            is YttriumGetPaymentRequestError.ConnectionFailed ->  Pay.GetPaymentRequestError.Http(error.v1)
            is YttriumGetPaymentRequestError.NoConnection ->  Pay.GetPaymentRequestError.Http(error.v1)
            is YttriumGetPaymentRequestError.RequestTimeout ->  Pay.GetPaymentRequestError.Http(error.v1)
        }
    }

    fun mapConfirmPaymentError(error: YttriumConfirmPaymentError): Pay.ConfirmPaymentError {
        return when (error) {
            is YttriumConfirmPaymentError.PaymentNotFound ->
                Pay.ConfirmPaymentError.PaymentNotFound(error.v1)

            is YttriumConfirmPaymentError.PaymentExpired ->
                Pay.ConfirmPaymentError.PaymentExpired(error.v1)

            is YttriumConfirmPaymentError.InvalidOption ->
                InvalidOption(error.v1)

            is YttriumConfirmPaymentError.InvalidSignature ->
                InvalidSignature(error.v1)

            is YttriumConfirmPaymentError.RouteExpired ->
                RouteExpired(error.v1)

            is YttriumConfirmPaymentError.Http ->
                Pay.ConfirmPaymentError.Http(error.v1)

            is YttriumConfirmPaymentError.InternalException ->
                Pay.ConfirmPaymentError.InternalError(error.v1)

            is YttriumConfirmPaymentError.UnsupportedMethod -> UnsupportedMethod(error.v1)
            is YttriumConfirmPaymentError.ConnectionFailed -> Pay.ConfirmPaymentError.Http(error.v1)
            is YttriumConfirmPaymentError.NoConnection -> Pay.ConfirmPaymentError.Http(error.v1)
            is YttriumConfirmPaymentError.RequestTimeout -> Pay.ConfirmPaymentError.Http(error.v1)
            is YttriumConfirmPaymentError.PollingTimeout -> Pay.ConfirmPaymentError.Http(error.v1)
        }
    }

    fun mapPayError(error: YttriumPayError): Pay.PayError {
        return when (error) {
            is YttriumPayError.Http -> Pay.PayError.Http(error.v1)
            is YttriumPayError.Api -> Api(error.v1)
            is YttriumPayError.Timeout -> Pay.PayError.Timeout
            is YttriumPayError.ConnectionFailed ->  Pay.PayError.Http(error.v1)
            is YttriumPayError.NoConnection ->  Pay.PayError.Http(error.v1)
            is YttriumPayError.RequestTimeout ->  Pay.PayError.Http(error.v1)
        }
    }
}

