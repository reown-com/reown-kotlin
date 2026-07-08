@file:JvmSynthetic

package com.reown.sample.wallet.payment

import com.reown.walletkit.client.Wallet

internal enum class InitialPaymentDestination {
    OPTIONS,
    WEB_VIEW_DATA_COLLECTION,
    SUMMARY,
}

internal data class InitialPaymentRoute(
    val destination: InitialPaymentDestination,
    val selectedOption: Wallet.Model.PaymentOption? = null,
)

internal object PaymentSelectionResolver {

    fun resolve(
        options: List<Wallet.Model.PaymentOption>,
        lastPaidTokenUnit: String?,
    ): InitialPaymentRoute {
        if (options.isEmpty()) return InitialPaymentRoute(InitialPaymentDestination.OPTIONS)

        val singleOption = options.singleOrNull()
        if (singleOption != null) {
            return if (singleOption.collectData?.url != null) {
                InitialPaymentRoute(
                    destination = InitialPaymentDestination.WEB_VIEW_DATA_COLLECTION,
                    selectedOption = singleOption,
                )
            } else {
                InitialPaymentRoute(
                    destination = InitialPaymentDestination.SUMMARY,
                    selectedOption = singleOption,
                )
            }
        }

        val preferredOption = lastPaidTokenUnit
            ?.let { preferredUnit -> options.firstOrNull { it.amount.unit == preferredUnit } }
            ?: return InitialPaymentRoute(InitialPaymentDestination.OPTIONS)

        return if (preferredOption.collectData?.url == null) {
            InitialPaymentRoute(
                destination = InitialPaymentDestination.SUMMARY,
                selectedOption = preferredOption,
            )
        } else {
            InitialPaymentRoute(InitialPaymentDestination.OPTIONS)
        }
    }
}
