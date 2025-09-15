@file:JvmSynthetic

package com.reown.sign.di

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.signing.cacao.CacaoVerifier
import com.reown.sign.client.SignListener
import com.reown.sign.client.SignStorage
import com.reown.sign.engine.domain.SignEngine
import com.reown.sign.engine.domain.wallet_service.WalletServiceFinder
import com.reown.sign.engine.domain.wallet_service.WalletServiceRequester
import com.reown.sign.engine.model.tvf.TVF
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import uniffi.yttrium.StorageFfi
import java.util.concurrent.TimeUnit

@JvmSynthetic
internal fun engineModule() = module {

    includes(
        callsModule(),
        requestsModule(),
        responsesModule()
    )

    single { TVF(moshi = get(named(AndroidCommonDITags.MOSHI))) }

//    single { GetPendingSessionRequests(jsonRpcHistory = get(), serializer = get()) }

//    single<GetPendingAuthenticateRequestUseCaseInterface> { GetPendingAuthenticateRequestUseCase(jsonRpcHistory = get(), serializer = get()) }

//    single { DeleteRequestByIdUseCase(jsonRpcHistory = get(), verifyContextStorageRepository = get()) }

//    single { GetPendingJsonRpcHistoryEntryByIdUseCase(jsonRpcHistory = get(), serializer = get()) }

//    single { GetSessionRequestByIdUseCase(jsonRpcHistory = get(), serializer = get()) }

//    single { GetPendingSessionAuthenticateRequest(jsonRpcHistory = get(), serializer = get()) }

//    single { GetSessionAuthenticateRequest(jsonRpcHistory = get(), serializer = get()) }

    single { CacaoVerifier(projectId = get()) }

    single { WalletServiceFinder(logger = get(named(AndroidCommonDITags.LOGGER))) }

    single {
        WalletServiceRequester(
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        )
    }

    single<StorageFfi> {
        SignStorage(
            sessionStorage = get(),
            metadataStorage = get(),
            selfAppMetaData = get(),
            pairingStorage = get()
        )
    }

    single {
        SignListener(
            sessionStorage = get(),
            metadataStorage = get()
        )
    }

    single {
        SignEngine(
            signListener = get(),
//            verifyContextStorageRepository = get(),
//            jsonRpcInteractor = get(),
//            crypto = get(),
//            authenticateResponseTopicRepository = get(),
            proposalStorageRepository = get(),
//            authenticateSessionUseCase = get(),
            sessionStorageRepository = get(),
            metadataStorageRepository = get(),
            approveSessionUseCase = get(),
            disconnectSessionUseCase = get(),
            emitEventUseCase = get(),
            extendSessionUseCase = get(),
//            decryptMessageUseCase = get(named(AndroidCommonDITags.DECRYPT_SIGN_MESSAGE)),
//            getListOfVerifyContextsUseCase = get(),
//            getPairingsUseCase = get(),
//            getPendingRequestsByTopicUseCase = get(),
//            getPendingSessionRequests = get(),
            getSessionProposalsUseCase = get(),
            getSessionsUseCase = get(),
//            onPingUseCase = get(),
//            getVerifyContextByIdUseCase = get(),
//            onSessionDeleteUseCase = get(),
//            onSessionEventUseCase = get(),
//            onSessionExtendUseCase = get(),
//            getPendingSessionRequestByTopicUseCase = get(),
//            onSessionProposalResponseUseCase = get(),
//            onSessionProposeUse = get(),
            signClient = get(named(AndroidCommonDITags.SIGN_RUST_CLIENT)),
//            onSessionRequestResponseUseCase = get(),
//            onSessionRequestUseCase = get(),
//            onSessionSettleResponseUseCase = get(),
//            onSessionSettleUseCase = get(),
//            onSessionUpdateResponseUseCase = get(),
//            onSessionUpdateUseCase = get(),
//            pairingController = get(),
//            pairUseCase = get(),
//            pingUseCase = get(),
            proposeSessionUseCase = get(),
            rejectSessionUseCase = get(),
            respondSessionRequestUseCase = get(),
            sessionRequestUseCase = get(),
            sessionUpdateUseCase = get(),
//            onAuthenticateSessionUseCase = get(),
//            onSessionAuthenticateResponseUseCase = get(),
//            approveSessionAuthenticateUseCase = get(),
//            rejectSessionAuthenticateUseCase = get(),
//            formatAuthenticateMessageUseCase = get(),
//            deleteRequestByIdUseCase = get(),
//            getPendingAuthenticateRequestUseCase = get(),
//            insertEventUseCase = get(),
//            linkModeJsonRpcInteractor = get(),
//            logger = get(named(AndroidCommonDITags.LOGGER)),
//            signClient = get(named(AndroidCommonDITags.SIGN_RUST_CLIENT)),
//            signStorage = get()
        )
    }
}