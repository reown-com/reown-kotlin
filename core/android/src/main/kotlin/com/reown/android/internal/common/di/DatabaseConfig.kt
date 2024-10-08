package com.reown.android.internal.common.di

import com.reown.utils.Empty
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

@Suppress("PropertyName")
class DatabaseConfig(private val storagePrefix: String = String.Empty) {
    val ANDROID_CORE_DB_NAME
        get() = storagePrefix + "WalletConnectAndroidCore.db"

    val SIGN_SDK_DB_NAME
        get() = storagePrefix + "WalletConnectV2.db"

    val NOTIFY_SDK_DB_NAME
        get() = storagePrefix + "WalletConnectV2_notify.db"

    val dbNames: List<String> = listOf(ANDROID_CORE_DB_NAME, SIGN_SDK_DB_NAME, NOTIFY_SDK_DB_NAME)
}

fun Scope.deleteDatabase(dbName: String) {
    androidContext().deleteDatabase(dbName)
}

fun Scope.deleteDatabases() {
    androidContext().databaseList().forEach { dbName ->
        if (get<DatabaseConfig>().dbNames.contains(dbName)) {
            deleteDatabase(dbName)
        }
    }
}