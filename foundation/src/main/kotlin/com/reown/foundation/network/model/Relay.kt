package com.reown.foundation.network.model

object Relay {

    sealed class Model {

        sealed class Call : Model() {
            abstract val id: Long
            abstract val jsonrpc: String

            sealed class ProposeSession : Call() {

                data class Request(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val method: String = WC_PROPOSE_SESSION,
                    val params: Params,
                ) : ProposeSession() {

                    data class Params(
                        val pairingTopic: String,
                        val sessionProposal: String,
                        val attestation: String,
                        val correlationId: Long
                    )
                }

                data class Acknowledgement(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val result: Boolean,
                ) : ProposeSession()

                data class JsonRpcError(
                    override val jsonrpc: String = "2.0",
                    val error: Error,
                    override val id: Long,
                ) : ProposeSession()
            }

            sealed class ApproveSession : Call() {

                data class Request(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val method: String = WC_APPROVE_SESSION,
                    val params: Params,
                ) : ApproveSession() {

                    data class Params(
                        val pairingTopic: String,
                        val sessionTopic: String,
                        val sessionProposalResponse: String,
                        val sessionSettlementRequest: String,
                        val correlationId: Long
                    )
                }

                data class Acknowledgement(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val result: Boolean,
                ) : ApproveSession()

                data class JsonRpcError(
                    override val jsonrpc: String = "2.0",
                    val error: Error,
                    override val id: Long,
                ) : ApproveSession()
            }

            sealed class Publish : Call() {

                data class Request(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val method: String = IRN_PUBLISH,
                    val params: Params,
                ) : Publish() {

                    data class Params(
                        val topic: String,
                        val message: String,
                        val ttl: Long,
                        val tag: Int,
                        val prompt: Boolean?,
                    )
                }

                data class Acknowledgement(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val result: Boolean,
                ) : Publish()

                data class JsonRpcError(
                    override val jsonrpc: String = "2.0",
                    val error: Error,
                    override val id: Long,
                ) : Publish()
            }

            sealed class Subscribe : Call() {

                data class Request(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val method: String = IRN_SUBSCRIBE,
                    val params: Params,
                ) : Subscribe() {

                    data class Params(
                        val topic: String,
                    )
                }

                data class Acknowledgement(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val result: String,
                ) : Subscribe()

                data class JsonRpcError(
                    override val jsonrpc: String = "2.0",
                    val error: Error,
                    override val id: Long,
                ) : Subscribe()
            }

            sealed class BatchSubscribe : Call() {

                data class Request(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val method: String = IRN_BATCH_SUBSCRIBE,
                    val params: Params,
                ) : Subscribe() {
                    data class Params(val topics: List<String>)
                }

                data class Acknowledgement(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val result: List<String>,
                ) : Subscribe()

                data class JsonRpcError(
                    override val jsonrpc: String = "2.0",
                    val error: Error,
                    override val id: Long,
                ) : Subscribe()
            }

            sealed class Subscription : Call() {

                data class Request(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val method: String = IRN_SUBSCRIPTION,
                    val params: Params,
                ) : Subscription() {

                    val subscriptionTopic: String = params.subscriptionData.topic
                    val message: String = params.subscriptionData.message
                    val tag: Int = params.subscriptionData.tag
                    val publishedAt: Long = params.subscriptionData.publishedAt
                    val attestation: String? = params.subscriptionData.attestation

                    data class Params(
                        val subscriptionId: String,
                        val subscriptionData: SubscriptionData,
                    ) {

                        data class SubscriptionData(
                            val topic: String,
                            val message: String,
                            val publishedAt: Long,
                            val attestation: String?,
                            val tag: Int
                        )
                    }
                }

                data class Acknowledgement(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val result: Boolean,
                ) : Subscription()

                data class JsonRpcError(
                    override val jsonrpc: String = "2.0",
                    val error: Error,
                    override val id: Long,
                ) : Subscription()
            }

            sealed class Unsubscribe : Call() {

                data class Request(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val method: String = IRN_UNSUBSCRIBE,
                    val params: Params,
                ) : Unsubscribe() {

                    data class Params(
                        val topic: String,
                        val subscriptionId: String,
                    )
                }

                data class Acknowledgement(
                    override val id: Long,
                    override val jsonrpc: String = "2.0",
                    val result: Boolean,
                ) : Unsubscribe()

                data class JsonRpcError(
                    override val jsonrpc: String = "2.0",
                    val error: Error,
                    override val id: Long,
                ) : Unsubscribe()
            }
        }

        data class Error(
            val code: Long,
            val message: String,
        ) : Model() {
            val errorMessage: String = "Error code: $code; Error message: $message"
        }

        sealed class Event : Model() {
            data class OnConnectionOpened<out WEB_SOCKET : Any>(val webSocket: WEB_SOCKET) : Event()
            data class OnMessageReceived(val message: Message) : Event()
            data class OnConnectionClosing(val shutdownReason: ShutdownReason) : Event()
            data class OnConnectionClosed(val shutdownReason: ShutdownReason) : Event()
            data class OnConnectionFailed(val throwable: Throwable) : Event()
        }

        sealed class Message : Model() {
            data class Text(val value: String) : Message()
            class Bytes(val value: ByteArray) : Message()
        }

        data class ShutdownReason(val code: Int, val reason: String) : Model()

        data class IrnParams(
            val tag: Int,
            val ttl: Long,
            val correlationId: Long?,
            val rpcMethods: List<String>? = null,
            val chainId: String? = null,
            val txHashes: List<String>? = null,
            val contractAddresses: List<String>? = null,
            val prompt: Boolean = false
        ) : Model()
    }
}