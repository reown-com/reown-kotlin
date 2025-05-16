package com.reown.sign.engine.model.tvf

import com.squareup.moshi.JsonClass

object XRPLSignTransaction {
    @JsonClass(generateAdapter = true)
    data class TransactionWrapper(
        val tx_json: TransactionJson
    )

    @JsonClass(generateAdapter = true)
    data class TransactionJson(
        val Account: String,
        val Expiration: Long,
        val Fee: String,
        val Flags: Int,
        val OfferSequence: Long,
        val Sequence: Long,
        val LastLedgerSequence: Long,
        val SigningPubKey: String,
        val TakerGets: String,
        val TakerPays: TakerPays,
        val TransactionType: String,
        val TxnSignature: String,
        val hash: String
    )

    @JsonClass(generateAdapter = true)
    data class TakerPays(
        val currency: String,
        val issuer: String,
        val value: String
    )
}

object XRPLSignTransactionFor {
    @JsonClass(generateAdapter = true)
    class TransactionWrapper(
        val tx_json: TransactionJson
    )

    @JsonClass(generateAdapter = true)
    data class TransactionJson(
        val Account: String,
        val TransactionType: String,
        val Amount: String,
        val Destination: String,
        val Fee: String,
        val Flags: Long,
        val LastLedgerSequence: Long,
        val Sequence: Long,
        val Signers: List<SignerWrapper>,
        val SigningPubKey: String,
        val hash: String
    )

    @JsonClass(generateAdapter = true)
    data class SignerWrapper(
        val Signer: Signer
    )

    @JsonClass(generateAdapter = true)
    data class Signer(
        val Account: String,
        val SigningPubKey: String,
        val TxnSignature: String
    )

}