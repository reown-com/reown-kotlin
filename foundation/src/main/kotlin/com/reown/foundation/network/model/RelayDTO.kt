package com.reown.foundation.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.reown.foundation.common.adapters.SubscriptionIdAdapter
import com.reown.foundation.common.adapters.TopicAdapter
import com.reown.foundation.common.adapters.TtlAdapter
import com.reown.foundation.common.model.SubscriptionId
import com.reown.foundation.common.model.Topic
import com.reown.foundation.common.model.Ttl
import com.reown.foundation.network.model.RelayDTO.Publish.Request.Params
import com.reown.util.generateClientToServerId

sealed class RelayDTO {
    abstract val id: Long
    abstract val jsonrpc: String

    sealed class ProposeSession : RelayDTO() {

        @JsonClass(generateAdapter = true)
        data class Request(
            @Json(name = "id")
            override val id: Long = generateClientToServerId(),
            @Json(name = "jsonrpc")
            override val jsonrpc: String = "2.0",
            @Json(name = "method")
            val method: String = WC_PROPOSE_SESSION,
            @Json(name = "params")
            val params: Params,
        ) : ProposeSession() {

            @JsonClass(generateAdapter = true)
            data class Params(
                @Json(name = "pairingTopic")
                @field:TopicAdapter.Qualifier
                val pairingTopic: Topic,
                @Json(name = "sessionProposal")
                val sessionProposal: String,
                @Json(name = "attestation")
                val attestation: String?,
                @Json(name = "correlationId")
                val correlationId: Long
            )
        }

        sealed class Result : ProposeSession() {

            @JsonClass(generateAdapter = true)
            data class Acknowledgement(
                @Json(name = "id")
                override val id: Long,
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "result")
                val result: Boolean,
            ) : Result()

            @JsonClass(generateAdapter = true)
            data class JsonRpcError(
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "error")
                val error: Error,
                @Json(name = "id")
                override val id: Long,
            ) : Result()
        }
    }

    sealed class ApproveSession : RelayDTO() {

        @JsonClass(generateAdapter = true)
        data class Request(
            @Json(name = "id")
            override val id: Long = generateClientToServerId(),
            @Json(name = "jsonrpc")
            override val jsonrpc: String = "2.0",
            @Json(name = "method")
            val method: String = WC_APPROVE_SESSION,
            @Json(name = "params")
            val params: Params,
        ) : ApproveSession() {

            @JsonClass(generateAdapter = true)
            data class Params(
                @Json(name = "pairingTopic")
                @field:TopicAdapter.Qualifier
                val pairingTopic: Topic,
                @Json(name = "sessionTopic")
                @field:TopicAdapter.Qualifier
                val sessionTopic: Topic,
                @Json(name = "sessionProposalResponse")
                val sessionProposalResponse: String,
                @Json(name = "sessionSettlementRequest")
                val sessionSettlementRequest: String,
                @Json(name = "correlationId")
                val correlationId: Long
            )
        }

        sealed class Result : ApproveSession() {

            @JsonClass(generateAdapter = true)
            data class Acknowledgement(
                @Json(name = "id")
                override val id: Long,
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "result")
                val result: Boolean,
            ) : Result()

            @JsonClass(generateAdapter = true)
            data class JsonRpcError(
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "error")
                val error: Error,
                @Json(name = "id")
                override val id: Long,
            ) : Result()
        }
    }

    sealed class Publish : RelayDTO() {

        @JsonClass(generateAdapter = true)
        data class Request(
            @Json(name = "id")
            override val id: Long = generateClientToServerId(),
            @Json(name = "jsonrpc")
            override val jsonrpc: String = "2.0",
            @Json(name = "method")
            val method: String = IRN_PUBLISH,
            @Json(name = "params")
            val params: Params,
        ) : Publish() {

            @JsonClass(generateAdapter = true)
            data class Params(
                @Json(name = "topic")
                @field:TopicAdapter.Qualifier
                val topic: Topic,
                @Json(name = "message")
                val message: String,
                @Json(name = "ttl")
                @field:TtlAdapter.Qualifier
                val ttl: Ttl,
                @Json(name = "tag")
                val tag: Int,
                @Json(name = "prompt")
                val prompt: Boolean?,
                @Json(name = "correlationId")
                val correlationId: Long?,
                @Json(name = "rpcMethods")
                val rpcMethods: List<String>?,
                @Json(name = "chainId")
                val chainId: String?,
                @Json(name = "txHashes")
                val txHashes: List<String>?,
                @Json(name = "contractAddresses")
                val contractAddresses: List<String>?,
            )
        }

        sealed class Result : Publish() {

            @JsonClass(generateAdapter = true)
            data class Acknowledgement(
                @Json(name = "id")
                override val id: Long,
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "result")
                val result: Boolean,
            ) : Result()

            @JsonClass(generateAdapter = true)
            data class JsonRpcError(
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "error")
                val error: Error,
                @Json(name = "id")
                override val id: Long,
            ) : Result()
        }
    }

    sealed class Subscribe : RelayDTO() {

        @JsonClass(generateAdapter = true)
        data class Request(
            @Json(name = "id")
            override val id: Long = generateClientToServerId(),
            @Json(name = "jsonrpc")
            override val jsonrpc: String = "2.0",
            @Json(name = "method")
            val method: String = IRN_SUBSCRIBE,
            @Json(name = "params")
            val params: Params,
        ) : Subscribe() {

            @JsonClass(generateAdapter = true)
            data class Params(
                @Json(name = "topic")
                @field:TopicAdapter.Qualifier
                val topic: Topic,
            )
        }

        sealed class Result : Subscribe() {

            @JsonClass(generateAdapter = true)
            data class Acknowledgement(
                @Json(name = "id")
                override val id: Long,
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "result")
                @field:SubscriptionIdAdapter.Qualifier
                val result: SubscriptionId,
            ) : Result()

            @JsonClass(generateAdapter = true)
            data class JsonRpcError(
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "error")
                val error: Error,
                @Json(name = "id")
                override val id: Long,
            ) : Result()
        }
    }

    sealed class BatchSubscribe : RelayDTO() {

        @JsonClass(generateAdapter = true)
        data class Request(
            @Json(name = "id")
            override val id: Long = generateClientToServerId(),
            @Json(name = "jsonrpc")
            override val jsonrpc: String = "2.0",
            @Json(name = "method")
            val method: String = IRN_BATCH_SUBSCRIBE,
            @Json(name = "params")
            val params: Params,
        ) : BatchSubscribe() {

            @JsonClass(generateAdapter = true)
            data class Params(
                @Json(name = "topics")
                val topics: List<String>,
            )
        }

        sealed class Result : BatchSubscribe() {

            @JsonClass(generateAdapter = true)
            data class Acknowledgement(
                @Json(name = "id")
                override val id: Long,
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "result")
                val result: List<String>,
            ) : Result()

            @JsonClass(generateAdapter = true)
            data class JsonRpcError(
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "error")
                val error: Error,
                @Json(name = "id")
                override val id: Long,
            ) : Result()
        }
    }

    sealed class Subscription : RelayDTO() {

        @JsonClass(generateAdapter = true)
        data class Request(
            @Json(name = "id")
            override val id: Long = generateClientToServerId(),
            @Json(name = "jsonrpc")
            override val jsonrpc: String = "2.0",
            @Json(name = "method")
            val method: String = IRN_SUBSCRIPTION,
            @Json(name = "params")
            val params: Params,
        ) : Subscription() {

            @JsonClass(generateAdapter = true)
            data class Params(
                @Json(name = "id")
                @field:SubscriptionIdAdapter.Qualifier
                val subscriptionId: SubscriptionId,
                @Json(name = "data")
                val subscriptionData: SubscriptionData,
            ) {

                @JsonClass(generateAdapter = true)
                data class SubscriptionData(
                    @Json(name = "topic")
                    @field:TopicAdapter.Qualifier
                    val topic: Topic,
                    @Json(name = "message")
                    val message: String, //ack, jsonrpc error, eth_sign
                    @Json(name = "publishedAt")
                    val publishedAt: Long,
                    @Json(name = "attestation")
                    val attestation: String?,
                    @Json(name = "tag")
                    val tag: Int,
                )
            }
        }

        sealed class Result : Subscription() {

            @JsonClass(generateAdapter = true)
            data class Acknowledgement(
                @Json(name = "id")
                override val id: Long,
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "result")
                val result: Boolean,
            ) : Subscription()

            @JsonClass(generateAdapter = true)
            data class JsonRpcError(
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "error")
                val error: Error,
                @Json(name = "id")
                override val id: Long,
            ) : Subscription()
        }
    }

    sealed class Unsubscribe : RelayDTO() {

        @JsonClass(generateAdapter = true)
        data class Request(
            @Json(name = "id")
            override val id: Long = generateClientToServerId(),
            @Json(name = "jsonrpc")
            override val jsonrpc: String = "2.0",
            @Json(name = "method")
            val method: String = IRN_UNSUBSCRIBE,
            @Json(name = "params")
            val params: Params,
        ) : Unsubscribe() {

            @JsonClass(generateAdapter = true)
            data class Params(
                @Json(name = "topic")
                @field:TopicAdapter.Qualifier
                val topic: Topic,
                @Json(name = "id")
                @field:SubscriptionIdAdapter.Qualifier
                val subscriptionId: SubscriptionId,
            )
        }

        sealed class Result : Unsubscribe() {

            @JsonClass(generateAdapter = true)
            data class Acknowledgement(
                @Json(name = "id")
                override val id: Long,
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "result")
                val result: Boolean,
            ) : Result()

            @JsonClass(generateAdapter = true)
            data class JsonRpcError(
                @Json(name = "jsonrpc")
                override val jsonrpc: String = "2.0",
                @Json(name = "error")
                val error: Error,
                @Json(name = "id")
                override val id: Long,
            ) : Result()
        }
    }

    @JsonClass(generateAdapter = true)
    data class Error(
        @Json(name = "code")
        val code: Long,
        @Json(name = "message")
        val message: String,
    ) {
        val errorMessage: String = "Error code: $code; Error message: $message"
    }
}