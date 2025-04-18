package com.reown.sample.common

import androidx.annotation.DrawableRes
import org.json.JSONArray
import org.json.JSONObject

fun getPersonalSignBody(account: String): String {
    val msg = "My email is john@doe.com - ${System.currentTimeMillis()}".encodeToByteArray()
        .joinToString(separator = "", prefix = "0x") { eachByte -> "%02x".format(eachByte) }
    return "[\"$msg\", \"$account\"]"
}

fun getEthSignBody(account: String): String {
    val msg = "My email is john@doe.com - ${System.currentTimeMillis()}".encodeToByteArray()
        .joinToString(separator = "", prefix = "0x") { eachByte -> "%02x".format(eachByte) }
    return "[\"$account\", \"$msg\"]"
}

fun getEthSendTransaction(account: String): String {
    return "[{\"from\":\"$account\",\"to\":\"0x70012948c348CBF00806A3C79E3c5DAdFaAa347B\",\"data\":\"0x\",\"gasLimit\":\"0x5208\",\"gasPrice\":\"0x0649534e00\",\"value\":\"0\",\"nonce\":\"0x07\"}]"
}

fun getEthSignTypedData(account: String): String {
    val stringifiedParams =
        """{\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallet\",\"type\":\"address\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\": \"to\",\"type\":\"Person\"},{\"name\":\"contents\",\"type\":\"string\"}]},\"primaryType\":\"Mail\",\"domain\":{\"name\":\"Ether Mail\",\"version\":\"1\",\"chainId\":1,\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\"},\"message\":{\"from\": {\"name\":\"Cow\",\"wallet\":\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\"},\"to\":{\"name\":\"Bob\",\"wallet\":\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\"},\"contents\":\"Hello, Bob!\"}}"""

    return "[\"$account\",\"$stringifiedParams\"]"
}

fun getSolanaSignAndSendParams(): String {
    return "{\"transaction\":\"4hXTCkRzt9WyecNzV1XPgCDfGAZzQKNxLXgynz5QDuWWPSAZBZSHptvWRL3BjCvzUXRdKvHL2b7yGrRQcWyaqsaBCncVG7BFggS8w9snUts67BSh3EqKpXLUm5UMHfD7ZBe9GhARjbNQMLJ1QD3Spr6oMTBU6EhdB4RD8CP2xUxr2u3d6fos36PD98XS6oX8TQjLpsMwncs5DAMiD4nNnR8NBfyghGCWvCVifVwvA8B8TJxE1aiyiv2L429BCWfyzAme5sZW8rDb14NeCQHhZbtNqfXhcp2tAnaAT\",\"sendOptions\": {\"skipPreflight\":true,\"preflightCommitment\":\"confirmed\",\"maxRetries\":2,\"minContextSlot\"?:1}}"
}

fun getGetWalletAssetsParams(account: String): String {
    return JSONObject()
        .put("account", account)
        .put("chainFilter", JSONArray().put("0xa")) //hardcoded chain filter
        .toString()
}

enum class Chains(
    val chainName: String,
    val chainNamespace: String,
    val chainReference: String,
    @DrawableRes val icon: Int,
    val color: String,
    val methods: List<String>,
    val events: List<String>,
    val order: Int,
    val chainId: String = "$chainNamespace:$chainReference"
) {

    ETHEREUM_MAIN(
        chainName = "Ethereum",
        chainNamespace = Info.Eth.chain,
        chainReference = "1",
        icon = R.drawable.ic_ethereum,
        color = "#617de8",
        methods = Info.Eth.defaultMethods,
        events = Info.Eth.defaultEvents,
        order = 1
    ),

    POLYGON_MATIC(
        chainName = "Polygon Matic",
        chainNamespace = Info.Eth.chain,
        chainReference = "137",
        icon = R.drawable.ic_polygon,
        color = "#8145e4",
        methods = Info.Eth.defaultMethods,
        events = Info.Eth.defaultEvents,
        order = 2
    ),

    ETHEREUM_KOVAN(
        chainName = "Ethereum Kovan",
        chainNamespace = Info.Eth.chain,
        chainReference = "42",
        icon = R.drawable.ic_ethereum,
        color = "#617de8",
        methods = Info.Eth.defaultMethods,
        events = Info.Eth.defaultEvents,
        order = 3
    ),

    OPTIMISM_KOVAN(
        chainName = "Optimism Kovan",
        chainNamespace = Info.Eth.chain,
        chainReference = "69",
        icon = R.drawable.ic_optimism,
        color = "#e70000",
        methods = Info.Eth.defaultMethods,
        events = Info.Eth.defaultEvents,
        order = 4
    ),

    POLYGON_MUMBAI(
        chainName = "Polygon Mumbai",
        chainNamespace = Info.Eth.chain,
        chainReference = "80001",
        icon = R.drawable.ic_polygon,
        color = "#8145e4",
        methods = Info.Eth.defaultMethods,
        events = Info.Eth.defaultEvents,
        order = 5
    ),

    ARBITRUM_RINKBY(
        chainName = "Arbitrum Rinkeby",
        chainNamespace = Info.Eth.chain,
        chainReference = "421611",
        icon = R.drawable.ic_arbitrum,
        color = "#95bbda",
        methods = Info.Eth.defaultMethods,
        events = Info.Eth.defaultEvents,
        order = 6
    ),

    CELO_ALFAJORES(
        chainName = "Celo Alfajores",
        chainNamespace = Info.Eth.chain,
        chainReference = "44787",
        icon = R.drawable.ic_celo,
        color = "#f9cb5b",
        methods = Info.Eth.defaultMethods,
        events = Info.Eth.defaultEvents,
        order = 7
    ),
    COSMOS(
        chainName = "Cosmos",
        chainNamespace = Info.Cosmos.chain,
        chainReference = "cosmoshub-4",
        icon = R.drawable.ic_cosmos,
        color = "#B2B2B2",
        methods = Info.Cosmos.defaultMethods,
        events = Info.Cosmos.defaultEvents,
        order = 7
    ),
    BNB(
        chainName = "BNB Smart Chain",
        chainNamespace = Info.Eth.chain,
        chainReference = "56",
        icon = R.drawable.bnb,
        color = "#F3BA2F",
        methods = Info.Eth.defaultMethods,
        events = Info.Eth.defaultEvents,
        order = 8
    ),
    SOLANA(
        chainName = "Solana Mainnet",
        chainNamespace = Info.Solana.chain,
        chainReference = "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp",
        icon = R.drawable.ic_solana,
        color = "#F3BA2F",
        methods = Info.Solana.defaultMethods,
        events = Info.Solana.defaultEvents,
        order = 9
    );

    sealed class Info {
        abstract val chain: String
        abstract val defaultEvents: List<String>
        abstract val defaultMethods: List<String>

        object Eth : Info() {
            override val chain = "eip155"
            override val defaultEvents: List<String> = listOf("chainChanged", "accountsChanged")
            override val defaultMethods: List<String> = listOf("eth_sendTransaction", "personal_sign", "eth_sign", "eth_signTypedData")
        }

        object Cosmos : Info() {
            override val chain = "cosmos"
            override val defaultEvents: List<String> = listOf()
            override val defaultMethods: List<String> = listOf("cosmos_signDirect", "cosmos_signAmino")
        }

        object Solana : Info() {
            override val chain = "solana"
            override val defaultEvents: List<String> = listOf()
            override val defaultMethods: List<String> = listOf("solana_signMessage", "solana_signTransaction", "solana_signAndSendTransaction", "solana_signAllTransactions")
        }
    }
}
