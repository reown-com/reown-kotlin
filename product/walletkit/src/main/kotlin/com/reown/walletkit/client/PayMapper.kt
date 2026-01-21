@file:JvmSynthetic

package com.reown.walletkit.client

import com.walletconnect.pay.Pay

// Pay -> Wallet.Model mappers

@JvmSynthetic
internal fun Pay.PaymentOptionsResponse.toWallet(): Wallet.Model.PaymentOptionsResponse =
    Wallet.Model.PaymentOptionsResponse(
        paymentId = paymentId,
        info = info?.toWallet(),
        options = options.map { it.toWallet() },
        collectDataAction = collectDataAction?.toWallet()
    )

@JvmSynthetic
internal fun Pay.PaymentInfo.toWallet(): Wallet.Model.PaymentInfo =
    Wallet.Model.PaymentInfo(
        status = status.toWallet(),
        amount = amount.toWallet(),
        expiresAt = expiresAt,
        merchant = merchant.toWallet()
    )

@JvmSynthetic
internal fun Pay.PaymentStatus.toWallet(): Wallet.Model.PaymentStatus =
    when (this) {
        Pay.PaymentStatus.REQUIRES_ACTION -> Wallet.Model.PaymentStatus.REQUIRES_ACTION
        Pay.PaymentStatus.PROCESSING -> Wallet.Model.PaymentStatus.PROCESSING
        Pay.PaymentStatus.SUCCEEDED -> Wallet.Model.PaymentStatus.SUCCEEDED
        Pay.PaymentStatus.FAILED -> Wallet.Model.PaymentStatus.FAILED
        Pay.PaymentStatus.EXPIRED -> Wallet.Model.PaymentStatus.EXPIRED
    }

@JvmSynthetic
internal fun Pay.Amount.toWallet(): Wallet.Model.PaymentAmount =
    Wallet.Model.PaymentAmount(
        value = value,
        unit = unit,
        display = display?.toWallet()
    )

@JvmSynthetic
internal fun Pay.AmountDisplay.toWallet(): Wallet.Model.PaymentAmountDisplay =
    Wallet.Model.PaymentAmountDisplay(
        assetSymbol = assetSymbol,
        assetName = assetName,
        decimals = decimals,
        iconUrl = iconUrl,
        networkName = networkName,
        networkIconUrl = networkIconUrl
    )

@JvmSynthetic
internal fun Pay.MerchantInfo.toWallet(): Wallet.Model.MerchantInfo =
    Wallet.Model.MerchantInfo(
        name = name,
        iconUrl = iconUrl
    )

@JvmSynthetic
internal fun Pay.PaymentOption.toWallet(): Wallet.Model.PaymentOption =
    Wallet.Model.PaymentOption(
        id = id,
        amount = amount.toWallet(),
        account = account,
        estimatedTxs = estimatedTxs
    )

@JvmSynthetic
internal fun Pay.CollectDataAction.toWallet(): Wallet.Model.CollectDataAction =
    Wallet.Model.CollectDataAction(
        fields = fields.map { it.toWallet() }
    )

@JvmSynthetic
internal fun Pay.CollectDataField.toWallet(): Wallet.Model.CollectDataField =
    Wallet.Model.CollectDataField(
        id = id,
        name = name,
        fieldType = fieldType.toWallet(),
        required = required
    )

@JvmSynthetic
internal fun Pay.CollectDataFieldType.toWallet(): Wallet.Model.CollectDataFieldType =
    when (this) {
        Pay.CollectDataFieldType.TEXT -> Wallet.Model.CollectDataFieldType.TEXT
        Pay.CollectDataFieldType.DATE -> Wallet.Model.CollectDataFieldType.DATE
    }

@JvmSynthetic
internal fun Pay.RequiredAction.toWallet(): Wallet.Model.RequiredAction =
    when (this) {
        is Pay.RequiredAction.WalletRpc -> Wallet.Model.RequiredAction.WalletRpc(
            action = action.toWallet()
        )
    }

@JvmSynthetic
internal fun Pay.WalletRpcAction.toWallet(): Wallet.Model.WalletRpcAction =
    Wallet.Model.WalletRpcAction(
        chainId = chainId,
        method = method,
        params = params
    )

@JvmSynthetic
internal fun Pay.ConfirmPaymentResponse.toWallet(): Wallet.Model.ConfirmPaymentResponse =
    Wallet.Model.ConfirmPaymentResponse(
        status = status.toWallet(),
        isFinal = isFinal,
        pollInMs = pollInMs
    )

// Wallet.Model -> Pay mappers (for request types)

@JvmSynthetic
internal fun Wallet.Model.CollectDataFieldResult.toPay(): Pay.CollectDataFieldResult =
    Pay.CollectDataFieldResult(
        id = id,
        value = value
    )
