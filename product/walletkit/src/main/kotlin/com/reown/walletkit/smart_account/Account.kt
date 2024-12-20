package com.reown.walletkit.smart_account

data class Account(val owner: String) {
    val address: String
        get() = owner.split(":").last()
    val reference: String
        get() = owner.split(":")[1]

    val namespace: String
        get() = owner.split(":").first()
}