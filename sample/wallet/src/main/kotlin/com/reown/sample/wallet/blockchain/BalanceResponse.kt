package com.reown.sample.wallet.blockchain

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BalanceResponse(
    @SerializedName("balances")
    val balances: List<TokenBalance>
)

@Keep
data class TokenBalance(
    @SerializedName("name")
    val name: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("chainId")
    val chainId: String,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("value")
    val value: Double,
    @SerializedName("price")
    val price: Double,
    @SerializedName("quantity")
    val quantity: TokenQuantity,
    @SerializedName("iconUrl")
    val iconUrl: String? = null
)

@Keep
data class TokenQuantity(
    @SerializedName("decimals")
    val decimals: String,
    @SerializedName("numeric")
    val numeric: String
)

