package com.reown.sign.engine.model.tvf

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TransactionResult(
    val txID: String,
    val signature: List<String>?,
    val raw_data: RawData?,
    val visible: Boolean?,
    val raw_data_hex: String?
)

@JsonClass(generateAdapter = true)
data class RawData(
    val expiration: Long?,
    val contract: List<Contract>?,
    val ref_block_hash: String?,
    val fee_limit: Long?,
    val timestamp: Long?,
    val ref_block_bytes: String?
)

@JsonClass(generateAdapter = true)
data class Contract(
    val parameter: Parameter?,
    val type: String?
)

@JsonClass(generateAdapter = true)
data class Parameter(
    val type_url: String?,
    @Json(name = "value")
    val value: ContractValue?
)

@JsonClass(generateAdapter = true)
data class ContractValue(
    val data: String?,
    val contract_address: String?,
    val owner_address: String?
)