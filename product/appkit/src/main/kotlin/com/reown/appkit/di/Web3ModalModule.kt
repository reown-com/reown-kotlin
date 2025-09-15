package com.reown.appkit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.appkit.domain.RecentWalletsRepository
import com.reown.appkit.domain.SessionRepository
import com.reown.appkit.domain.model.Session
import com.reown.appkit.domain.usecase.DeleteSessionDataUseCase
import com.reown.appkit.domain.usecase.GetRecentWalletUseCase
import com.reown.appkit.domain.usecase.GetSelectedChainUseCase
import com.reown.appkit.domain.usecase.GetSessionUseCase
import com.reown.appkit.domain.usecase.ObserveSelectedChainUseCase
import com.reown.appkit.domain.usecase.ObserveSessionUseCase
import com.reown.appkit.domain.usecase.SaveChainSelectionUseCase
import com.reown.appkit.domain.usecase.SaveRecentWalletUseCase
import com.reown.appkit.domain.usecase.SaveSessionUseCase
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_store")

internal fun appKitModule(projectId: String) = module {

    single { RecentWalletsRepository(sharedPreferences = get()) }

    single { GetRecentWalletUseCase(repository = get()) }
    single { SaveRecentWalletUseCase(repository = get()) }

    single<PolymorphicJsonAdapterFactory<Session>> {
        PolymorphicJsonAdapterFactory.of(Session::class.java, "type")
            .withSubtype(Session.WalletConnect::class.java, "wcsession")
            .withSubtype(Session.Coinbase::class.java, "coinbase")
    }

    single<Moshi>(named(AppKitDITags.MOSHI)) {
        get<Moshi.Builder>(named(AndroidCommonDITags.MOSHI))
            .add(get<PolymorphicJsonAdapterFactory<Session>>())
            .build()
    }

    single(named(AppKitDITags.SESSION_DATA_STORE)) { androidContext().sessionDataStore }
    single {
        SessionRepository(
            sessionStore = get(named(AppKitDITags.SESSION_DATA_STORE)),
            moshi = get<Moshi>(named(AppKitDITags.MOSHI))
        )
    }

    single { GetSessionUseCase(repository = get()) }
    single { SaveSessionUseCase(repository = get()) }
    single { DeleteSessionDataUseCase(repository = get()) }
    single { SaveChainSelectionUseCase(repository = get()) }
    single { GetSelectedChainUseCase(repository = get()) }
    single { ObserveSessionUseCase(repository = get()) }
    single { ObserveSelectedChainUseCase(repository = get()) }

    includes(blockchainApiModule(projectId), balanceRpcModule(), engineModule())
}
