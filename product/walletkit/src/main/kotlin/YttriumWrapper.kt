import uniffi.uniffi_yttrium.AccountClient
import uniffi.uniffi_yttrium.AccountClientConfig
import uniffi.yttrium.Config
import uniffi.yttrium.Endpoint
import uniffi.yttrium.Endpoints


lateinit var accountClient: AccountClient
private var apiKey: String =  ""
private var projectId: String =  ""

private val endpoints = Endpoints(
    rpc = Endpoint(baseUrl = "https://rpc.walletconnect.com/v1?chainId=eip155:11155111&projectId=$projectId", apiKey = ""), //http://$host:8545
    bundler = Endpoint(baseUrl = "https://api.pimlico.io/v2/11155111/rpc?apikey=$apiKey", apiKey = ""), //http://$host:4337
    paymaster = Endpoint(baseUrl = "https://api.pimlico.io/v2/11155111/rpc?apikey=$apiKey", apiKey = ""), // "http://$host:3000"
)

private val config = Config(endpoints)

val accountConfig = AccountClientConfig(
    ownerAddress = "",//EthAccountDelegate.account,
    chainId = 1u,
    config = config,
    privateKey = "",
    safe = true,
    signerType = "PrivateKey"
)

var smartAccountAddress: String = ""