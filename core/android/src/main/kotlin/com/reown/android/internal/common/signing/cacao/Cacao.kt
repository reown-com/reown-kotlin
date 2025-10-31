package com.reown.android.internal.common.signing.cacao

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.reown.android.cacao.SignatureInterface
import com.reown.android.internal.common.signing.cacao.Cacao.Payload.Companion.RECAPS_PREFIX
import com.reown.android.internal.common.signing.signature.Signature

@JsonClass(generateAdapter = true)
data class Cacao(
    @param:Json(name = "h")
    val header: Header,
    @param:Json(name = "p")
    val payload: Payload,
    @param:Json(name = "s")
    val signature: Signature,
) {
    @Keep
    @JsonClass(generateAdapter = true)
    data class Signature(
        @param:Json(name = "t")
        override val t: String,
        @param:Json(name = "s")
        override val s: String,
        @param:Json(name = "m")
        override val m: String? = null,
    ) : SignatureInterface

    @JsonClass(generateAdapter = true)
    data class Header(
        @param:Json(name = "t")
        val t: String,
    )

    @JsonClass(generateAdapter = true)
    data class Payload(
        @param:Json(name = "iss")
        val iss: String,
        @param:Json(name = "domain")
        val domain: String,
        @param:Json(name = "aud")
        val aud: String,
        @param:Json(name = "version")
        val version: String,
        @param:Json(name = "nonce")
        val nonce: String,
        @param:Json(name = "iat")
        val iat: String,
        @param:Json(name = "nbf")
        val nbf: String?,
        @param:Json(name = "exp")
        val exp: String?,
        @param:Json(name = "statement")
        val statement: String?,
        @param:Json(name = "requestId")
        val requestId: String?,
        @param:Json(name = "resources")
        val resources: List<String>?,
    ) {
        @get:Throws(Exception::class)
        val actionsString get() = resources.getActionsString()

        @get:Throws(Exception::class)
        val methods get() = resources.getMethods()

        companion object {
            const val CURRENT_VERSION = "1"
            const val ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZZZZZ"
            const val RECAPS_PREFIX = "urn:recap:"
            const val ATT_KEY = "att"
        }
    }
}

@JvmSynthetic
internal fun Cacao.Signature.toSignature(): Signature = Signature.fromString(s)

fun Cacao.Payload.toCAIP222Message(chainName: String = "Ethereum"): String {
    var message = "$domain wants you to sign in with your $chainName account:\n${Issuer(iss).address}\n\n"
    if (statement?.contains(RECAPS_STATEMENT) == true) {
        message += "$statement\n"
    } else {
        if (statement != null) message += "$statement"
        if (resources?.find { r -> r.startsWith(RECAPS_PREFIX) } != null) {
            message += if (statement != null) " " else ""
            message += "$RECAPS_STATEMENT: ${resources.getActionsString()}.\n"
        } else if (statement != null) {
            message += "\n"
        }
    }
    message += "\nURI: $aud\nVersion: $version\nChain ID: ${Issuer(iss).chainIdReference}\nNonce: $nonce\nIssued At: $iat"
    if (exp != null) message += "\nExpiration Time: $exp"
    if (nbf != null) message += "\nNot Before: $nbf"
    if (requestId != null) message += "\nRequest ID: $requestId"
    if (resources != null) {
        message += "\nResources:"
        resources.forEach { resource -> message += "\n- $resource" }
    }

    return message
}

fun Pair<String?, List<String>?>.getStatement(): String? {
    val (statement, resources) = this
    var newStatement = ""
    if (statement != null) newStatement += "$statement"
    if (resources?.find { r -> r.startsWith(RECAPS_PREFIX) } != null) {
        newStatement += if (statement != null) " " else ""
        newStatement += "$RECAPS_STATEMENT: ${resources.getActionsString()}."
    }

    return if (newStatement == "") null else newStatement
}

private fun List<String>?.getActionsString(): String {
    val map = this.decodeReCaps().parseReCaps()
    if (map.isEmpty()) throw Exception("Decoded ReCaps map is empty")
    var result = ""
    var index = 1

    map.forEach { (key, values) ->
        val prefix = values.keys.firstOrNull()?.substringBefore('/') ?: ""
        val itemsFormatted = values.keys.sorted().joinToString(", ") { "'${it.substringAfter('/')}'" }

        result += if (index == map.size) {
            "($index) '$prefix': $itemsFormatted for '$key'"
        } else {
            "($index) '$prefix': $itemsFormatted for '$key'. "
        }
        index++
    }

    return result
}

const val RECAPS_STATEMENT: String = "I further authorize the stated URI to perform the following actions on my behalf"