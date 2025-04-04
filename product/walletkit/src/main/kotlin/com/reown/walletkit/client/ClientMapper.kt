package com.reown.walletkit.client

import com.reown.android.internal.common.signing.cacao.CacaoType
import com.reown.sign.client.Sign
import com.squareup.moshi.Moshi
import uniffi.uniffi_yttrium.Eip1559Estimation
import uniffi.yttrium.Amount
import uniffi.yttrium.Call
import uniffi.yttrium.DoSendTransactionParams
import uniffi.yttrium.ExecuteDetails
import uniffi.yttrium.FeeEstimatedTransaction
import uniffi.yttrium.FundingMetadata
import uniffi.yttrium.InitialTransactionMetadata
import uniffi.yttrium.OwnerSignature
import uniffi.yttrium.PrepareResponseAvailable
import uniffi.yttrium.PreparedSendTransaction
import uniffi.yttrium.Route
import uniffi.yttrium.RouteSig
import uniffi.yttrium.SolanaTransaction
import uniffi.yttrium.SolanaTxnDetails
import uniffi.yttrium.Metadata as YMetadata
import uniffi.yttrium.Transaction
import uniffi.yttrium.TransactionFee
import uniffi.yttrium.Transactions
import uniffi.yttrium.TxnDetails
import uniffi.yttrium.UiFields

@JvmSynthetic
internal fun Map<String, Wallet.Model.Namespace.Session>.toSign(): Map<String, Sign.Model.Namespace.Session> =
    mapValues { (_, namespace) ->
        Sign.Model.Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events)
    }

@JvmSynthetic
internal fun Map<String, Sign.Model.Namespace.Session>.toWallet(): Map<String, Wallet.Model.Namespace.Session> =
    mapValues { (_, namespace) ->
        Wallet.Model.Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events)
    }

@JvmSynthetic
internal fun Map<String, Sign.Model.Namespace.Proposal>.toWalletProposalNamespaces(): Map<String, Wallet.Model.Namespace.Proposal> =
    mapValues { (_, namespace) ->
        Wallet.Model.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }

@JvmSynthetic
internal fun Map<String, Wallet.Model.Namespace.Proposal>.toSignProposalNamespaces(): Map<String, Sign.Model.Namespace.Proposal> =
    mapValues { (_, namespace) ->
        Sign.Model.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }

@JvmSynthetic
internal fun Wallet.Model.JsonRpcResponse.toSign(): Sign.Model.JsonRpcResponse =
    when (this) {
        is Wallet.Model.JsonRpcResponse.JsonRpcResult -> this.toSign()
        is Wallet.Model.JsonRpcResponse.JsonRpcError -> this.toSign()
    }

@JvmSynthetic
internal fun Wallet.Model.JsonRpcResponse.JsonRpcResult.toSign(): Sign.Model.JsonRpcResponse.JsonRpcResult =
    Sign.Model.JsonRpcResponse.JsonRpcResult(id, result)

@JvmSynthetic
internal fun Wallet.Model.JsonRpcResponse.JsonRpcError.toSign(): Sign.Model.JsonRpcResponse.JsonRpcError =
    Sign.Model.JsonRpcResponse.JsonRpcError(id, code, message)

@JvmSynthetic
internal fun Wallet.Model.Cacao.Signature.toSign(): Sign.Model.Cacao.Signature = Sign.Model.Cacao.Signature(t, s, m)

@JvmSynthetic
internal fun Wallet.Model.SessionEvent.toSign(): Sign.Model.SessionEvent = Sign.Model.SessionEvent(name, data)

@JvmSynthetic
internal fun Wallet.Model.Event.toSign(): Sign.Model.Event = Sign.Model.Event(topic, name, data, chainId)

@JvmSynthetic
internal fun Wallet.Model.PayloadAuthRequestParams.toSign(): Sign.Model.PayloadParams =
    Sign.Model.PayloadParams(
        type = type ?: CacaoType.CAIP222.header,
        chains = chains,
        domain = domain,
        aud = aud,
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
        iat = iat
    )

@JvmSynthetic
internal fun Sign.Model.Session.toWallet(): Wallet.Model.Session = Wallet.Model.Session(
    pairingTopic,
    topic,
    expiry,
    requiredNamespaces.toWalletProposalNamespaces(),
    optionalNamespaces?.toWalletProposalNamespaces(),
    namespaces.toWallet(),
    metaData
)

@JvmSynthetic
internal fun List<Sign.Model.PendingRequest>.mapToPendingRequests(): List<Wallet.Model.PendingSessionRequest> = map { request ->
    Wallet.Model.PendingSessionRequest(
        request.requestId,
        request.topic,
        request.method,
        request.chainId,
        request.params
    )
}

@JvmSynthetic
internal fun List<Sign.Model.SessionRequest>.mapToPendingSessionRequests(): List<Wallet.Model.SessionRequest> = map { request ->
    Wallet.Model.SessionRequest(
        request.topic,
        request.chainId,
        request.peerMetaData,
        Wallet.Model.SessionRequest.JSONRPCRequest(request.request.id, request.request.method, request.request.params)
    )
}

@JvmSynthetic
internal fun Sign.Model.SessionProposal.toWallet(): Wallet.Model.SessionProposal =
    Wallet.Model.SessionProposal(
        pairingTopic,
        name,
        description,
        url,
        icons,
        redirect,
        requiredNamespaces.toWalletProposalNamespaces(),
        optionalNamespaces.toWalletProposalNamespaces(),
        properties,
        proposerPublicKey,
        relayProtocol,
        relayData,
        scopedProperties
    )

@JvmSynthetic
internal fun Sign.Model.SessionAuthenticate.toWallet(): Wallet.Model.SessionAuthenticate =
    Wallet.Model.SessionAuthenticate(id, topic, participant.toWallet(), payloadParams.toWallet())

@JvmSynthetic
internal fun Sign.Model.SessionAuthenticate.Participant.toWallet(): Wallet.Model.SessionAuthenticate.Participant =
    Wallet.Model.SessionAuthenticate.Participant(publicKey, metadata)

@JvmSynthetic
internal fun Sign.Model.PayloadParams.toWallet(): Wallet.Model.PayloadAuthRequestParams =
    Wallet.Model.PayloadAuthRequestParams(
        chains = chains,
        type = type ?: CacaoType.CAIP222.header,
        domain = domain,
        aud = aud,
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
        iat = iat
    )

internal fun Sign.Model.VerifyContext.toWallet(): Wallet.Model.VerifyContext =
    Wallet.Model.VerifyContext(id, origin, this.validation.toWallet(), verifyUrl, isScam)

internal fun Sign.Model.Validation.toWallet(): Wallet.Model.Validation =
    when (this) {
        Sign.Model.Validation.VALID -> Wallet.Model.Validation.VALID
        Sign.Model.Validation.INVALID -> Wallet.Model.Validation.INVALID
        Sign.Model.Validation.UNKNOWN -> Wallet.Model.Validation.UNKNOWN
    }

@JvmSynthetic
internal fun Sign.Model.SessionRequest.toWallet(): Wallet.Model.SessionRequest =
    Wallet.Model.SessionRequest(
        topic = topic,
        chainId = chainId,
        peerMetaData = peerMetaData,
        request = Wallet.Model.SessionRequest.JSONRPCRequest(
            id = request.id,
            method = request.method,
            params = request.params
        )
    )

@JvmSynthetic
internal fun Sign.Model.DeletedSession.toWallet(): Wallet.Model.SessionDelete =
    when (this) {
        is Sign.Model.DeletedSession.Success -> Wallet.Model.SessionDelete.Success(topic, reason)
        is Sign.Model.DeletedSession.Error -> Wallet.Model.SessionDelete.Error(error)
    }

@JvmSynthetic
internal fun Sign.Model.SettledSessionResponse.toWallet(): Wallet.Model.SettledSessionResponse =
    when (this) {
        is Sign.Model.SettledSessionResponse.Result -> Wallet.Model.SettledSessionResponse.Result(session.toWallet())
        is Sign.Model.SettledSessionResponse.Error -> Wallet.Model.SettledSessionResponse.Error(errorMessage)
    }

@JvmSynthetic
internal fun Sign.Model.SessionUpdateResponse.toWallet(): Wallet.Model.SessionUpdateResponse =
    when (this) {
        is Sign.Model.SessionUpdateResponse.Result -> Wallet.Model.SessionUpdateResponse.Result(topic, namespaces.toWallet())
        is Sign.Model.SessionUpdateResponse.Error -> Wallet.Model.SessionUpdateResponse.Error(errorMessage)
    }

@JvmSynthetic
internal fun Sign.Model.ExpiredProposal.toWallet(): Wallet.Model.ExpiredProposal = Wallet.Model.ExpiredProposal(pairingTopic, proposerPublicKey)

@JvmSynthetic
internal fun Sign.Model.ExpiredRequest.toWallet(): Wallet.Model.ExpiredRequest = Wallet.Model.ExpiredRequest(topic, id)

@JvmSynthetic
internal fun Wallet.Model.SessionProposal.toSign(): Sign.Model.SessionProposal =
    Sign.Model.SessionProposal(
        pairingTopic,
        name,
        description,
        url,
        icons,
        redirect,
        requiredNamespaces.toSignProposalNamespaces(),
        optionalNamespaces.toSignProposalNamespaces(),
        properties,
        proposerPublicKey,
        relayProtocol,
        relayData,
        scopedProperties
    )

@JvmSynthetic
internal fun Sign.Model.Message.SessionProposal.toWallet(): Wallet.Model.Message.SessionProposal =
    Wallet.Model.Message.SessionProposal(
        id,
        pairingTopic,
        name,
        description,
        url,
        icons,
        redirect,
        requiredNamespaces.toWalletProposalNamespaces(),
        optionalNamespaces.toWalletProposalNamespaces(),
        properties,
        proposerPublicKey,
        relayProtocol,
        relayData
    )

@JvmSynthetic
internal fun Sign.Model.Message.SessionRequest.toWallet(): Wallet.Model.Message.SessionRequest =
    Wallet.Model.Message.SessionRequest(
        topic,
        chainId,
        peerMetaData,
        Wallet.Model.Message.SessionRequest.JSONRPCRequest(request.id, request.method, request.params)
    )

@JvmSynthetic
internal fun List<Wallet.Model.Cacao>.toSign(): List<Sign.Model.Cacao> = mutableListOf<Sign.Model.Cacao>().apply {
    this@toSign.forEach { cacao: Wallet.Model.Cacao ->
        with(cacao) {
            add(
                Sign.Model.Cacao(
                    Sign.Model.Cacao.Header(header.t),
                    Sign.Model.Cacao.Payload(
                        payload.iss,
                        payload.domain,
                        payload.aud,
                        payload.version,
                        payload.nonce,
                        payload.iat,
                        payload.nbf,
                        payload.exp,
                        payload.statement,
                        payload.requestId,
                        payload.resources
                    ),
                    Sign.Model.Cacao.Signature(signature.t, signature.s, signature.m)
                )
            )
        }
    }
}

@JvmSynthetic
internal fun Sign.Model.Cacao.toWallet(): Wallet.Model.Cacao = with(this) {
    Wallet.Model.Cacao(
        Wallet.Model.Cacao.Header(header.t),
        Wallet.Model.Cacao.Payload(
            payload.iss,
            payload.domain,
            payload.aud,
            payload.version,
            payload.nonce,
            payload.iat,
            payload.nbf,
            payload.exp,
            payload.statement,
            payload.requestId,
            payload.resources
        ),
        Wallet.Model.Cacao.Signature(signature.t, signature.s, signature.m)
    )
}

@JvmSynthetic
internal fun Sign.Model.ConnectionState.Reason.toWallet(): Wallet.Model.ConnectionState.Reason = when (this) {
    is Sign.Model.ConnectionState.Reason.ConnectionClosed -> Wallet.Model.ConnectionState.Reason.ConnectionClosed(this.message)
    is Sign.Model.ConnectionState.Reason.ConnectionFailed -> Wallet.Model.ConnectionState.Reason.ConnectionFailed(this.throwable)
}

@JvmSynthetic
internal fun PreparedSendTransaction.toWallet(moshi: Moshi): Wallet.Params.PrepareSendTransactionsResult {
    val jsonParams = moshi.adapter(DoSendTransactionParams::class.java).toJson(doSendTransactionParams)
    return Wallet.Params.PrepareSendTransactionsResult(hash = hash, doSendTransactionParams = jsonParams, eip712Domain = domain)
}

@JvmSynthetic
internal fun Call.toWallet(): Wallet.Model.Call = Wallet.Model.Call(to = to, value = value, input = input)

@JvmSynthetic
internal fun Wallet.Params.OwnerSignature.toYttrium(): OwnerSignature = OwnerSignature(owner = address, signature = signature)

@JvmSynthetic
internal fun ExecuteDetails.toWallet(): Wallet.Model.ExecuteSuccess = Wallet.Model.ExecuteSuccess(
    initialTxHash = initialTxnHash,
    initialTxReceipt = initialTxnReceipt,
)

@JvmSynthetic
internal fun UiFields.toWallet(): Wallet.Model.PrepareSuccess.Available =
    Wallet.Model.PrepareSuccess.Available(
        orchestratorId = routeResponse.orchestrationId,
        checkIn = routeResponse.metadata.checkIn.toLong(),
        transactions = routeResponse.transactions.map {
            when (it) {
                is Transactions.Eip155 -> Wallet.Model.Transactions.Eip155(it.v1.map { eip155 -> eip155.toWallet() })
                is Transactions.Solana -> Wallet.Model.Transactions.Solana(it.v1.map { solana -> solana.toWallet() })
                else -> throw Exception("Unsupported Transaction type")
            }
        },
        initialTransaction = routeResponse.initialTransaction.toWallet(),
        initialTransactionMetadata = routeResponse.metadata.initialTransaction.toWallet(),
        funding = routeResponse.metadata.fundingFrom.map { it.toWallet() },
        transactionsDetails = toTransactionsDetails()
    )

private fun UiFields.toTransactionsDetails() = Wallet.Model.TransactionsDetails(
    localTotal = localTotal.toWallet(),
    initialDetails = initial.toWallet(),
    route = route.map {
        when (it) {
            is Route.Eip155 -> Wallet.Model.Route.Eip155(it.v1.map { eip155 -> eip155.toWallet() })
            is Route.Solana -> Wallet.Model.Route.Solana(it.v1.map { solana -> solana.toWallet() })
            else -> throw Exception("Unsupported Route type")
        }
    },
    bridgeFees = bridge.map { it.toWallet() },
    localFulfilmentTotal = localRouteTotal.toWallet(),
    localBridgeTotal = localBridgeTotal.toWallet()
)

@JvmSynthetic
internal fun Wallet.Model.PrepareSuccess.Available.toResponseYttrium(): PrepareResponseAvailable =
    PrepareResponseAvailable(
        orchestratorId,
        metadata = YMetadata(
            checkIn = checkIn.toULong(),
            initialTransaction = initialTransactionMetadata.toYttrium(),
            fundingFrom = funding.map { it.toYttrium() }
        ),
        initialTransaction = initialTransaction.toYttrium(),
        transactions = transactions.map {
            when (it) {
                is Wallet.Model.Transactions.Eip155 -> Transactions.Eip155(it.transactions.map { eip155 -> eip155.toYttrium() })
                is Wallet.Model.Transactions.Solana -> Transactions.Solana(it.transactions.map { solana -> solana.toYttrium() })
            }
        })

@JvmSynthetic
internal fun Wallet.Model.RouteSig.toYttrium(): RouteSig = when (this) {
    is Wallet.Model.RouteSig.Eip155 -> RouteSig.Eip155(this.signatures)
    is Wallet.Model.RouteSig.Solana -> RouteSig.Solana(this.signatures)
}

@JvmSynthetic
internal fun Wallet.Model.PrepareSuccess.Available.toYttrium(): UiFields =
    UiFields(
        route = transactionsDetails.route.map {
            when (it) {
                is Wallet.Model.Route.Eip155 -> Route.Eip155(it.transactionDetails.map { eip155 -> eip155.toYttrium() })
                is Wallet.Model.Route.Solana -> Route.Solana(it.solanaTransactionDetails.map { solana -> solana.toYttrium() })
            }
        },
        localTotal = transactionsDetails.localTotal.toYttrium(),
        localRouteTotal = transactionsDetails.localFulfilmentTotal.toYttrium(),
        bridge = transactionsDetails.bridgeFees.map { it.toYttrium() },
        localBridgeTotal = transactionsDetails.localBridgeTotal.toYttrium(),
        initial = transactionsDetails.initialDetails.toYttrium(),
        routeResponse = toResponseYttrium()
    )

@JvmSynthetic
private fun Wallet.Model.InitialTransactionMetadata.toYttrium(): InitialTransactionMetadata =
    InitialTransactionMetadata(
        transferTo = transferTo,
        symbol = symbol,
        amount = amount,
        tokenContract = tokenContract,
        decimals = decimals.toUByte()
    )

@JvmSynthetic
private fun InitialTransactionMetadata.toWallet(): Wallet.Model.InitialTransactionMetadata =
    Wallet.Model.InitialTransactionMetadata(
        transferTo = transferTo,
        symbol = symbol,
        amount = amount,
        tokenContract = tokenContract,
        decimals = decimals.toInt()
    )

@JvmSynthetic
fun Transaction.toWallet(): Wallet.Model.Transaction = Wallet.Model.Transaction(
    from = from,
    to = to,
    value = value,
    gasLimit = gasLimit,
    input = input,
    nonce = nonce,
    chainId = chainId
)

@JvmSynthetic
fun SolanaTransaction.toWallet(): Wallet.Model.SolanaTransaction = Wallet.Model.SolanaTransaction(
    from = from,
    chainId = chainId,
    versionedTransaction = transaction
)

@JvmSynthetic
fun Wallet.Model.Transaction.toYttrium(): Transaction = Transaction(
    from = from,
    to = to,
    value = value,
    gasLimit = gasLimit,
    input = input,
    nonce = nonce,
    chainId = chainId
)

@JvmSynthetic
fun Wallet.Model.SolanaTransaction.toYttrium(): SolanaTransaction = SolanaTransaction(
    from = from,
    chainId = chainId,
    transaction = versionedTransaction
)

@JvmSynthetic
private fun Wallet.Model.FundingMetadata.toYttrium(): FundingMetadata =
    FundingMetadata(chainId, tokenContract, symbol, amount = amount, bridgingFee = bridgingFee, decimals = decimals.toUByte())

@JvmSynthetic
private fun FundingMetadata.toWallet(): Wallet.Model.FundingMetadata =
    Wallet.Model.FundingMetadata(chainId, tokenContract, symbol, amount, bridgingFee, 1)

@JvmSynthetic
internal fun Eip1559Estimation.toWallet(): Wallet.Model.EstimatedFees =
    Wallet.Model.EstimatedFees(maxFeePerGas = maxFeePerGas, maxPriorityFeePerGas = maxPriorityFeePerGas)

@JvmSynthetic
internal fun Amount.toWallet(): Wallet.Model.Amount = Wallet.Model.Amount(
    symbol = symbol,
    amount = amount,
    unit = unit.toString(),
    formattedAlt = formattedAlt,
    formatted = formatted
)

@JvmSynthetic
internal fun Wallet.Model.Amount.toYttrium(): Amount = Amount(
    symbol = symbol,
    amount = amount,
    unit = unit.toUByte(),
    formattedAlt = formattedAlt,
    formatted = formatted
)

private fun TxnDetails.toWallet(): Wallet.Model.TransactionDetails = Wallet.Model.TransactionDetails(
    feeEstimatedTransaction = transaction.toWallet(),
    transactionFee = fee.toWallet(),
    transactionHashToSign = transactionHashToSign
)

private fun SolanaTxnDetails.toWallet(): Wallet.Model.SolanaTransactionDetails = Wallet.Model.SolanaTransactionDetails(
    transaction = transaction.toWallet(),
    transactionHashToSign = transactionHashToSign
)

private fun Wallet.Model.TransactionDetails.toYttrium(): TxnDetails = TxnDetails(
    transaction = feeEstimatedTransaction.toWallet(),
    fee = transactionFee.toYttrium(),
    transactionHashToSign = transactionHashToSign
)

private fun Wallet.Model.SolanaTransactionDetails.toYttrium(): SolanaTxnDetails = SolanaTxnDetails(
    transaction = SolanaTransaction(chainId = transaction.chainId, from = transaction.from, transaction = transaction.versionedTransaction),
    transactionHashToSign = transactionHashToSign
)

fun FeeEstimatedTransaction.toWallet(): Wallet.Model.FeeEstimatedTransaction = Wallet.Model.FeeEstimatedTransaction(
    from = from,
    to = to,
    value = value,
    gasLimit = gasLimit,
    input = input,
    nonce = nonce,
    maxFeePerGas = maxFeePerGas,
    maxPriorityFeePerGas = maxPriorityFeePerGas,
    chainId = chainId
)

fun Wallet.Model.FeeEstimatedTransaction.toWallet(): FeeEstimatedTransaction = FeeEstimatedTransaction(
    from = from,
    to = to,
    value = value,
    gasLimit = gasLimit,
    input = input,
    nonce = nonce,
    maxFeePerGas = maxFeePerGas,
    maxPriorityFeePerGas = maxPriorityFeePerGas,
    chainId = chainId
)

private fun TransactionFee.toWallet() = Wallet.Model.TransactionFee(
    fee = Wallet.Model.Amount(
        symbol = fee.symbol,
        amount = fee.amount,
        unit = fee.unit.toString(),
        formattedAlt = fee.formattedAlt,
        formatted = fee.formatted
    ),
    localFee = Wallet.Model.Amount(
        symbol = localFee.symbol,
        amount = localFee.amount,
        unit = localFee.unit.toString(),
        formattedAlt = localFee.formattedAlt,
        formatted = localFee.formatted
    )
)

private fun Wallet.Model.TransactionFee.toYttrium() = TransactionFee(
    fee = Amount(
        symbol = fee.symbol,
        amount = fee.amount,
        unit = fee.unit.toUByte(),
        formattedAlt = fee.formattedAlt,
        formatted = fee.formatted
    ),
    localFee = Amount(
        symbol = localFee.symbol,
        amount = localFee.amount,
        unit = localFee.unit.toUByte(),
        formattedAlt = localFee.formattedAlt,
        formatted = localFee.formatted
    )
)