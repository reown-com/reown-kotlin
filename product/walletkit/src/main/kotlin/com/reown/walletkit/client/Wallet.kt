package com.reown.walletkit.client

import androidx.annotation.Keep
import com.reown.android.Core
import com.reown.android.CoreInterface
import com.reown.android.cacao.SignatureInterface
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object Wallet {

    sealed interface Listeners {
        interface SessionPing : Listeners {
            fun onSuccess(pingSuccess: Model.Ping.Success)
            fun onError(pingError: Model.Ping.Error)
        }
    }

    sealed class Params {
        data class Init(val core: CoreInterface) : Params()

        data class Pair(val uri: String) : Params()

        data class SessionApprove(
            val proposerPublicKey: String,
            val namespaces: Map<String, Model.Namespace.Session>,
            val properties: Map<String, String>? = null,
            val scopedProperties: Map<String, String>? = null,
            val relayProtocol: String? = null,
            val proposalRequestsResponses: Wallet.Model.ProposalRequestsResponses? = null
        ) : Params()

        data class ApproveSessionAuthenticate(val id: Long, val auths: List<Model.Cacao>) : Params()

        data class RejectSessionAuthenticate(val id: Long, val reason: String) : Params()

        data class SessionReject(val proposerPublicKey: String, val reason: String) : Params()

        data class SessionUpdate(val sessionTopic: String, val namespaces: Map<String, Model.Namespace.Session>) : Params()

        data class SessionExtend(val topic: String) : Params()

        data class SessionEmit(val topic: String, val event: Model.SessionEvent, val chainId: String) : Params()

        data class SessionRequestResponse(val sessionTopic: String, val jsonRpcResponse: Model.JsonRpcResponse) : Params()

        data class SessionDisconnect(val sessionTopic: String) : Params()

        data class FormatMessage(val payloadParams: Model.PayloadParams, val issuer: String) : Params()

        data class FormatAuthMessage(val payloadParams: Model.PayloadAuthRequestParams, val issuer: String) : Params()

        data class Ping(val sessionTopic: String, val timeout: Duration = 30.seconds) : Params()

        sealed class AuthRequestResponse : Params() {
            abstract val id: Long

            data class Result(override val id: Long, val signature: Model.Cacao.Signature, val issuer: String) : AuthRequestResponse()
            data class Error(override val id: Long, val code: Int, val message: String) : AuthRequestResponse()
        }

        data class DecryptMessage(val topic: String, val encryptedMessage: String) : Params()
        data class GetSmartAccountAddress(val owner: Account) : Params()
        data class PrepareSendTransactions(val calls: List<Model.Call>, val owner: Account) : Params()
        data class DoSendTransactions(val owner: Account, val signatures: List<OwnerSignature>, val doSendTransactionParams: String) : Params()
        data class PrepareSendTransactionsResult(var hash: String, var doSendTransactionParams: String, val eip712Domain: String) : Params()
        data class DoSendTransactionsResult(var userOperationHash: String) : Params()
        data class WaitForUserOperationReceipt(var owner: Account, var userOperationHash: String) : Params()
        data class OwnerSignature(val address: String, val signature: String) : Params()
        data class Account(val address: String) : Params()

        // Payment params
        data class RequiredPaymentActions(
            val paymentId: String,
            val optionId: String
        ) : Params()

        data class ConfirmPayment(
            val paymentId: String,
            val optionId: String,
            val signatures: List<String>,
            val collectedData: List<Model.CollectDataFieldResult>? = null
        ) : Params()
    }

    sealed class Model {

        data class ProposalRequestsResponses(
            val authentication: List<Cacao>
        ) : Model()

        sealed class Ping : Model() {
            data class Success(val topic: String) : Ping()
            data class Error(val error: Throwable) : Ping()
        }

        data class Call(val to: String, val value: String, val input: String) : Model()

        data class Error(val throwable: Throwable) : Model()

        data class Transaction(
            var from: String,
            var to: String,
            var value: String,
            var gasLimit: String,
            var input: String,
            var nonce: String,
            var chainId: String
        ) : Model()

        data class SolanaTransaction(
            val from: String,
            val chainId: String,
            val versionedTransaction: String
        ) : Model()

        sealed class Transactions : Model() {
            data class Eip155(val transactions: List<Transaction>) : Transactions()
            data class Solana(val transactions: List<SolanaTransaction>) : Transactions()
        }

        data class InitialTransaction(
            var from: String,
            var to: String,
            var value: String,
            var input: String,
            var chainId: String
        ) : Model()

        data class FeeEstimatedTransaction(
            var from: String,
            var to: String,
            var value: String,
            var gasLimit: String,
            var input: String,
            var nonce: String,
            var maxFeePerGas: String,
            var maxPriorityFeePerGas: String,
            var chainId: String
        ) : Model()

        data class FundingMetadata(
            var chainId: String,
            var tokenContract: String,
            var symbol: String,
            var amount: String,
            var bridgingFee: String,
            var decimals: Int
        ) : Model()

        data class InitialTransactionMetadata(
            var symbol: String,
            var amount: String,
            var decimals: Int,
            var tokenContract: String,
            var transferTo: String
        ) : Model()

        data class EstimatedFees(
            val maxFeePerGas: String,
            val maxPriorityFeePerGas: String
        ) : Model()

        sealed class PrepareSuccess : Model() {
            data class Available(
                val orchestratorId: String,
                val checkIn: Long,
                val transactions: List<Transactions>,
                val initialTransaction: Transaction,
                val initialTransactionMetadata: InitialTransactionMetadata,
                val funding: List<FundingMetadata>,
                val transactionsDetails: TransactionsDetails
            ) : PrepareSuccess()

            data class NotRequired(val initialTransaction: Transaction) : PrepareSuccess()
        }

        data class ExecuteSuccess(
            val initialTxHash: String,
            val initialTxReceipt: String
        ) : Model()

        data class Amount(
            var symbol: String,
            var amount: String,
            var unit: String,
            var formatted: String,
            var formattedAlt: String
        ) : Model()

        data class TransactionFee(
            var fee: Amount,
            var localFee: Amount
        ) : Model()

        sealed class Route : Model() {
            data class Eip155(val transactionDetails: List<TransactionDetails>) : Route()
            data class Solana(val solanaTransactionDetails: List<SolanaTransactionDetails>) : Route()
        }

        sealed class RouteSig : Model() {
            data class Eip155(val signatures: List<String>) : RouteSig()
            data class Solana(val signatures: List<String>) : RouteSig()
        }

        data class TransactionDetails(
            var feeEstimatedTransaction: FeeEstimatedTransaction,
            var transactionFee: TransactionFee,
            val transactionHashToSign: String
        ) : Model()

        data class SolanaTransactionDetails(
            var transaction: SolanaTransaction,
            val transactionHashToSign: String
        ) : Model()

        data class TransactionsDetails(
            var route: List<Route>,
            var initialDetails: TransactionDetails,
            var bridgeFees: List<TransactionFee>,
            var localBridgeTotal: Amount,
            var localFulfilmentTotal: Amount,
            var localTotal: Amount
        ) : Model()

        sealed class PrepareError : Model() {
            data class NoRoutesAvailable(val message: String) : PrepareError()
            data class InsufficientFunds(val message: String) : PrepareError()
            data class InsufficientGasFunds(val message: String) : PrepareError()
            data class AssetNotSupported(val message: String) : PrepareError()
            data class TransactionSimulationFailed(val message: String) : PrepareError()
            data class Unknown(val message: String) : PrepareError()
        }

        sealed class Status : Model() {
            data class Completed(val createdAt: Long) : Status()
            data class Error(val reason: String) : Status()
        }

        data class ConnectionState(val isAvailable: Boolean, val reason: Reason? = null) : Model() {
            sealed class Reason : Model() {
                data class ConnectionClosed(val message: String) : Reason()
                data class ConnectionFailed(val throwable: Throwable) : Reason()
            }
        }

        data class ExpiredProposal(val pairingTopic: String, val proposerPublicKey: String) : Model()
        data class ExpiredRequest(val topic: String, val id: Long) : Model()

        data class SessionProposal(
            val pairingTopic: String,
            val name: String,
            val description: String,
            val url: String,
            val icons: List<URI>,
            val redirect: String,
            val requiredNamespaces: Map<String, Namespace.Proposal>,
            val optionalNamespaces: Map<String, Namespace.Proposal>,
            val properties: Map<String, String>?,
            val proposerPublicKey: String,
            val relayProtocol: String,
            val relayData: String?,
            val scopedProperties: Map<String, String>?,
            val requests: ProposalRequests?
        ) : Model()

        data class ProposalRequests(
            val authentication: List<PayloadAuthRequestParams>
        ) : Model()

        data class SessionAuthenticate(
            val id: Long,
            val pairingTopic: String,
            val participant: Participant,
            val payloadParams: PayloadAuthRequestParams,
        ) : Model() {
            data class Participant(
                val publicKey: String,
                val metadata: Core.Model.AppMetaData?,
            ) : Model()
        }

        data class VerifyContext(
            val id: Long,
            val origin: String,
            val validation: Validation,
            val verifyUrl: String,
            val isScam: Boolean?,
        ) : Model()

        enum class Validation {
            VALID, INVALID, UNKNOWN
        }

        data class SessionRequest(
            val topic: String,
            val chainId: String?,
            val peerMetaData: Core.Model.AppMetaData?,
            val request: JSONRPCRequest,
        ) : Model() {

            data class JSONRPCRequest(
                val id: Long,
                val method: String,
                val params: String,
            ) : Model()
        }

        sealed class SettledSessionResponse : Model() {
            data class Result(val session: Session) : SettledSessionResponse()
            data class Error(val errorMessage: String) : SettledSessionResponse()
        }

        sealed class SessionUpdateResponse : Model() {
            data class Result(val topic: String, val namespaces: Map<String, Namespace.Session>) : SessionUpdateResponse()
            data class Error(val errorMessage: String) : SessionUpdateResponse()
        }

        sealed class SessionDelete : Model() {
            data class Success(val topic: String, val reason: String) : SessionDelete()
            data class Error(val error: Throwable) : SessionDelete()
        }

        sealed class Namespace : Model() {

            //Required or Optional
            data class Proposal(
                val chains: List<String>? = null,
                val methods: List<String>,
                val events: List<String>,
            ) : Namespace()

            data class Session(
                val chains: List<String>? = null,
                val accounts: List<String>,
                val methods: List<String>,
                val events: List<String>,
            ) : Namespace()
        }

        sealed class JsonRpcResponse : Model() {
            abstract val id: Long
            val jsonrpc: String = "2.0"

            data class JsonRpcResult(
                override val id: Long,
                val result: String?,
            ) : JsonRpcResponse()

            data class JsonRpcError(
                override val id: Long,
                val code: Int,
                val message: String,
            ) : JsonRpcResponse()
        }

        data class PayloadParams(
            val type: String,
            val chainId: String,
            val domain: String,
            val aud: String,
            val version: String,
            val nonce: String,
            val iat: String,
            val nbf: String?,
            val exp: String?,
            val statement: String?,
            val requestId: String?,
            val resources: List<String>?,
            val signatureTypes: Map<String, List<String>>?,
        ) : Model()

        data class PayloadAuthRequestParams(
            val chains: List<String>,
            val domain: String,
            val nonce: String,
            val aud: String,
            val type: String?,
            val iat: String,
            val nbf: String?,
            val exp: String?,
            val statement: String?,
            val requestId: String?,
            val resources: List<String>?,
            val signatureTypes: Map<String, List<String>>?,
        ) : Model()

        data class SessionEvent(
            val name: String,
            val data: String,
        ) : Model()

        data class Event(
            val topic: String,
            val name: String,
            val data: String,
            val chainId: String,
        ) : Model()

        data class Cacao(
            val header: Header,
            val payload: Payload,
            val signature: Signature,
        ) : Model() {
            @Keep
            data class Signature(override val t: String, override val s: String, override val m: String? = null) : Model(), SignatureInterface
            data class Header(val t: String) : Model()
            data class Payload(
                val iss: String,
                val domain: String,
                val aud: String,
                val version: String,
                val nonce: String,
                val iat: String,
                val nbf: String?,
                val exp: String?,
                val statement: String?,
                val requestId: String?,
                val resources: List<String>?,
            ) : Model() {
                val address: String get() = iss.split(ISS_DELIMITER)[ISS_POSITION_OF_ADDRESS]

                private companion object {
                    const val ISS_DELIMITER = ":"
                    const val ISS_POSITION_OF_ADDRESS = 4
                }
            }
        }

        data class Session(
            @Deprecated("Pairing topic is deprecated")
            val pairingTopic: String,
            val topic: String,
            val expiry: Long,
            val requiredNamespaces: Map<String, Namespace.Proposal>,
            val optionalNamespaces: Map<String, Namespace.Proposal>?,
            val namespaces: Map<String, Namespace.Session>,
            val metaData: Core.Model.AppMetaData?,
        ) : Model() {
            val redirect: String? = metaData?.redirect
        }

        data class PendingSessionRequest(
            val requestId: Long,
            val topic: String,
            val method: String,
            val chainId: String?,
            val params: String,
        ) : Model()

        sealed class Message : Model() {

            data class Simple(
                val title: String,
                val body: String,
            ) : Message()

            data class SessionProposal(
                val id: Long,
                val pairingTopic: String,
                val name: String,
                val description: String,
                val url: String,
                val icons: List<String>,
                val redirect: String,
                val requiredNamespaces: Map<String, Namespace.Proposal>,
                val optionalNamespaces: Map<String, Namespace.Proposal>,
                val properties: Map<String, String>?,
                val proposerPublicKey: String,
                val relayProtocol: String,
                val relayData: String?,
            ) : Message()

            data class SessionRequest(
                val topic: String,
                val chainId: String?,
                val peerMetaData: Core.Model.AppMetaData?,
                val request: JSONRPCRequest,
            ) : Message() {
                data class JSONRPCRequest(
                    val id: Long,
                    val method: String,
                    val params: String,
                ) : Message()
            }
        }

        // Payment models
        enum class PaymentStatus {
            REQUIRES_ACTION,
            PROCESSING,
            SUCCEEDED,
            FAILED,
            EXPIRED
        }

        data class PaymentAmountDisplay(
            val assetSymbol: String,
            val assetName: String,
            val decimals: Int,
            val iconUrl: String?,
            val networkName: String?,
            val networkIconUrl: String?
        ) : Model()

        data class PaymentAmount(
            val value: String,
            val unit: String,
            val display: PaymentAmountDisplay?
        ) : Model()

        data class MerchantInfo(
            val name: String,
            val iconUrl: String?
        ) : Model()

        data class PaymentInfo(
            val status: PaymentStatus,
            val amount: PaymentAmount,
            val expiresAt: Long,
            val merchant: MerchantInfo
        ) : Model()

        data class PaymentOption(
            val id: String,
            val amount: PaymentAmount,
            val account: String,
            val estimatedTxs: Int?
        ) : Model()

        enum class CollectDataFieldType {
            TEXT,
            DATE,
            CHECKBOX
        }

        data class CollectDataField(
            val id: String,
            val name: String,
            val fieldType: CollectDataFieldType,
            val required: Boolean
        ) : Model()

        data class CollectDataAction(
            @Deprecated("Use url for WebView-based data collection or schema to parse field requirements")
            val fields: List<CollectDataField>,
            val url: String?,
            val schema: String?
        ) : Model()

        data class CollectDataFieldResult(
            val id: String,
            val value: String
        ) : Model()

        data class PaymentOptionsResponse(
            val paymentId: String,
            val info: PaymentInfo?,
            val options: List<PaymentOption>,
            val collectDataAction: CollectDataAction?
        ) : Model()

        data class WalletRpcAction(
            val chainId: String,
            val method: String,
            val params: String
        ) : Model()

        sealed class RequiredAction : Model() {
            data class WalletRpc(val action: WalletRpcAction) : RequiredAction()
        }

        data class PaymentResultInfo(
            val txId: String,
            val optionAmount: PaymentAmount
        ) : Model()

        data class ConfirmPaymentResponse(
            val status: PaymentStatus,
            val isFinal: Boolean,
            val pollInMs: Long?
        ) : Model()
    }
}