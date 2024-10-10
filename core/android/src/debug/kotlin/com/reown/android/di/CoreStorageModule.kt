package com.reown.android.di

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.reown.android.internal.common.di.DatabaseConfig
import com.reown.android.internal.common.di.baseStorageModule
import com.reown.android.sdk.core.AndroidCoreDatabase
import com.reown.utils.Empty
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun coreStorageModule(storagePrefix: String = String.Empty, packageName: String) = module {

    includes(baseStorageModule(storagePrefix, packageName))

    single<SqlDriver>(named(AndroidBuildVariantDITags.ANDROID_CORE_DATABASE_DRIVER)) {
        AndroidSqliteDriver(
            schema = AndroidCoreDatabase.Schema,
            context = androidContext(),
            name = get<DatabaseConfig>().ANDROID_CORE_DB_NAME,
        )
    }
}

fun sdkBaseStorageModule(databaseSchema: SqlSchema<QueryResult.Value<Unit>>, databaseName: String) = module {
    single<SqlDriver>(named(databaseName)) {
        AndroidSqliteDriver(
            schema = databaseSchema,
            context = androidContext(),
            name = databaseName,
        )
    }
}