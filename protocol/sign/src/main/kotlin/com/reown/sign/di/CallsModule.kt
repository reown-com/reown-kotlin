package com.reown.sign.di

import com.reown.android.internal.common.di.AndroidCommonDITags
import com.reown.android.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractorInterface
import com.reown.android.internal.common.model.Tags
import com.reown.android.push.notifications.DecryptMessageUseCaseInterface
import com.reown.sign.engine.use_case.calls.ApproveSessionAuthenticateUseCase
import com.reown.sign.engine.use_case.calls.ApproveSessionAuthenticateUseCaseInterface
import com.reown.sign.engine.use_case.calls.ApproveSessionUseCase
import com.reown.sign.engine.use_case.calls.ApproveSessionUseCaseInterface
import com.reown.sign.engine.use_case.calls.DecryptSignMessageUseCase
import com.reown.sign.engine.use_case.calls.DisconnectSessionUseCase
import com.reown.sign.engine.use_case.calls.DisconnectSessionUseCaseInterface
import com.reown.sign.engine.use_case.calls.EmitEventUseCase
import com.reown.sign.engine.use_case.calls.EmitEventUseCaseInterface
import com.reown.sign.engine.use_case.calls.ExtendSessionUseCase
import com.reown.sign.engine.use_case.calls.ExtendSessionUseCaseInterface
import com.reown.sign.engine.use_case.calls.FormatAuthenticateMessageUseCase
import com.reown.sign.engine.use_case.calls.FormatAuthenticateMessageUseCaseInterface
import com.reown.sign.engine.use_case.calls.GetListOfVerifyContextsUseCase
import com.reown.sign.engine.use_case.calls.GetListOfVerifyContextsUseCaseInterface
import com.reown.sign.engine.use_case.calls.GetNamespacesFromReCaps
import com.reown.sign.engine.use_case.calls.GetPairingsUseCase
import com.reown.sign.engine.use_case.calls.GetPairingsUseCaseInterface
import com.reown.sign.engine.use_case.calls.GetSessionProposalsUseCase
import com.reown.sign.engine.use_case.calls.GetSessionProposalsUseCaseInterface
import com.reown.sign.engine.use_case.calls.GetSessionsUseCase
import com.reown.sign.engine.use_case.calls.GetSessionsUseCaseInterface
import com.reown.sign.engine.use_case.calls.GetVerifyContextByIdUseCase
import com.reown.sign.engine.use_case.calls.GetVerifyContextByIdUseCaseInterface
import com.reown.sign.engine.use_case.calls.PairUseCase
import com.reown.sign.engine.use_case.calls.PairUseCaseInterface
import com.reown.sign.engine.use_case.calls.PingUseCase
import com.reown.sign.engine.use_case.calls.PingUseCaseInterface
import com.reown.sign.engine.use_case.calls.ProposeSessionUseCase
import com.reown.sign.engine.use_case.calls.ProposeSessionUseCaseInterface
import com.reown.sign.engine.use_case.calls.RejectSessionAuthenticateUseCase
import com.reown.sign.engine.use_case.calls.RejectSessionAuthenticateUseCaseInterface
import com.reown.sign.engine.use_case.calls.RejectSessionUseCase
import com.reown.sign.engine.use_case.calls.RejectSessionUseCaseInterface
import com.reown.sign.engine.use_case.calls.RespondSessionRequestUseCase
import com.reown.sign.engine.use_case.calls.RespondSessionRequestUseCaseInterface
import com.reown.sign.engine.use_case.calls.SessionAuthenticateUseCase
import com.reown.sign.engine.use_case.calls.SessionAuthenticateUseCaseInterface
import com.reown.sign.engine.use_case.calls.SessionRequestUseCase
import com.reown.sign.engine.use_case.calls.SessionRequestUseCaseInterface
import com.reown.sign.engine.use_case.calls.SessionUpdateUseCase
import com.reown.sign.engine.use_case.calls.SessionUpdateUseCaseInterface
import com.reown.sign.json_rpc.domain.GetPendingRequestsUseCaseByTopic
import com.reown.sign.json_rpc.domain.GetPendingRequestsUseCaseByTopicInterface
import com.reown.sign.json_rpc.domain.GetPendingSessionRequestByTopicUseCase
import com.reown.sign.json_rpc.domain.GetPendingSessionRequestByTopicUseCaseInterface
import org.koin.core.qualifier.named
import org.koin.dsl.module

@JvmSynthetic
internal fun callsModule() = module {

    single<ProposeSessionUseCaseInterface> {
        ProposeSessionUseCase(
//            jsonRpcInteractor = get(),
            crypto = get(),
            selfAppMetaData = get(),
            proposalStorageRepository = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single<SessionAuthenticateUseCaseInterface> {
        SessionAuthenticateUseCase(
//            jsonRpcInteractor = get(),
            crypto = get(),
            selfAppMetaData = get(),
            authenticateResponseTopicRepository = get(),
            proposeSessionUseCase = get(),
//            getPairingForSessionAuthenticate = get(),
            getNamespacesFromReCaps = get(),
//            linkModeJsonRpcInteractor = get<LinkModeJsonRpcInteractorInterface>(),
            linkModeStorageRepository = get(),
//            insertEventUseCase = get(),
//            clientId = get(named(AndroidCommonDITags.CLIENT_ID)),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single<PairUseCaseInterface> { PairUseCase(pairingInterface = get()) }

    single<ApproveSessionUseCaseInterface> {
        ApproveSessionUseCase(
            proposalStorageRepository = get(),
            selfAppMetaData = get(),
            crypto = get(),
//            jsonRpcInteractor = get(),
            metadataStorageRepository = get(),
            sessionStorageRepository = get(),
            verifyContextStorageRepository = get(),
//            insertEventUseCase = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
            signClient = get(named(AndroidCommonDITags.SIGN_RUST_CLIENT)),
            pairingRepository = get()
        )
    }

    single<ApproveSessionAuthenticateUseCaseInterface> {
        ApproveSessionAuthenticateUseCase(
//            jsonRpcInteractor = get(),
            crypto = get(),
            cacaoVerifier = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
            verifyContextStorageRepository = get(),
            getPendingSessionAuthenticateRequest = get(),
            selfAppMetaData = get(),
            sessionStorageRepository = get(),
            metadataStorageRepository = get(),
            insertTelemetryEventUseCase = get(),
//            insertEventUseCase = get(),
//            clientId = get(named(AndroidCommonDITags.CLIENT_ID)),
//            linkModeJsonRpcInteractor = get<LinkModeJsonRpcInteractorInterface>()
        )
    }

    single<RejectSessionAuthenticateUseCaseInterface> {
        RejectSessionAuthenticateUseCase(
//            jsonRpcInteractor = get(),
            crypto = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
            verifyContextStorageRepository = get(),
            getPendingSessionAuthenticateRequest = get(),
//            linkModeJsonRpcInteractor = get<LinkModeJsonRpcInteractorInterface>(),
//            clientId = get(named(AndroidCommonDITags.CLIENT_ID)),
//            insertEventUseCase = get()
        )
    }

    single<RejectSessionUseCaseInterface> {
        RejectSessionUseCase(
            verifyContextStorageRepository = get(),
            proposalStorageRepository = get(),
//            jsonRpcInteractor = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single<SessionUpdateUseCaseInterface> {
        SessionUpdateUseCase(
//            jsonRpcInteractor = get(),
            sessionStorageRepository = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single<SessionRequestUseCaseInterface> {
        SessionRequestUseCase(
//            jsonRpcInteractor = get(),
            sessionStorageRepository = get(),
//            linkModeJsonRpcInteractor = get(),
            metadataStorageRepository = get(),
//            insertEventUseCase = get(),
//            clientId = get(named(AndroidCommonDITags.CLIENT_ID)),
            logger = get(named(AndroidCommonDITags.LOGGER)),
//            tvf = get(),
            walletServiceFinder = get(),
            walletServiceRequester = get()
        )
    }

    single<RespondSessionRequestUseCaseInterface> {
        RespondSessionRequestUseCase(
//            jsonRpcInteractor = get(),
            verifyContextStorageRepository = get(),
            sessionStorageRepository = get(),
//            logger = get(named(AndroidCommonDITags.LOGGER)),
//            getPendingJsonRpcHistoryEntryByIdUseCase = get(),
//            linkModeJsonRpcInteractor = get(),
            metadataStorageRepository = get(),
//            insertEventUseCase = get(),
//            clientId = get(named(AndroidCommonDITags.CLIENT_ID)),
//            tvf = get(),
            signClient = get(named(AndroidCommonDITags.SIGN_RUST_CLIENT))
        )
    }

    single<DecryptMessageUseCaseInterface>(named(AndroidCommonDITags.DECRYPT_SIGN_MESSAGE)) {
        val useCase = DecryptSignMessageUseCase(
            codec = get(),
            serializer = get(),
            metadataRepository = get(),
            pushMessageStorage = get(),
        )

        get<MutableMap<String, DecryptMessageUseCaseInterface>>(named(AndroidCommonDITags.DECRYPT_USE_CASES))[Tags.SESSION_PROPOSE.id.toString()] =
            useCase
        useCase
    }

    single<PingUseCaseInterface> {
        PingUseCase(
            sessionStorageRepository = get(),
//            jsonRpcInteractor = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single<EmitEventUseCaseInterface> {
        EmitEventUseCase(
//            jsonRpcInteractor = get(),
            sessionStorageRepository = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single<ExtendSessionUseCaseInterface> {
        ExtendSessionUseCase(
//            jsonRpcInteractor = get(),
            sessionStorageRepository = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single<DisconnectSessionUseCaseInterface> {
        DisconnectSessionUseCase(
//            jsonRpcInteractor = get(),
            sessionStorageRepository = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single<GetSessionsUseCaseInterface> {
        GetSessionsUseCase(
            sessionStorageRepository = get(),
            metadataStorageRepository = get(),
            selfAppMetaData = get()
        )
    }

//    single<GetPairingsUseCaseInterface> { GetPairingsUseCase(pairingInterface = get()) }

//    single { GetPairingForSessionAuthenticateUseCase(pairingProtocol = get()) }

    single { GetNamespacesFromReCaps() }

//    single<GetPendingRequestsUseCaseByTopicInterface> { GetPendingRequestsUseCaseByTopic(serializer = get(), jsonRpcHistory = get()) }
//
//    single<GetPendingSessionRequestByTopicUseCaseInterface> {
//        GetPendingSessionRequestByTopicUseCase(
//            jsonRpcHistory = get(),
//            serializer = get(),
//            metadataStorageRepository = get()
//        )
//    }

    single<GetSessionProposalsUseCaseInterface> { GetSessionProposalsUseCase(proposalStorageRepository = get()) }

    single<GetVerifyContextByIdUseCaseInterface> { GetVerifyContextByIdUseCase(verifyContextStorageRepository = get()) }

    single<GetListOfVerifyContextsUseCaseInterface> { GetListOfVerifyContextsUseCase(verifyContextStorageRepository = get()) }

    single<FormatAuthenticateMessageUseCaseInterface> { FormatAuthenticateMessageUseCase() }
}