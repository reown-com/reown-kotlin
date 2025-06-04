package com.reown.sign.tvf

import com.reown.sign.engine.model.tvf.TVF
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test

class TVFTests {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val tvf = TVF(moshi)

    @Test
    fun `collect should return null when rpcMethod is not in the allowed list`() {
        // Arrange
        val rpcMethod = "unsupported_method"
        val rpcParams = "{}"
        val chainId = "1"

        // Act
        val result = tvf.collect(rpcMethod, rpcParams, chainId)

        // Assert
        assertEquals("1", result.third)
        assertEquals(listOf("unsupported_method"), result.first)
        assertNull(result.second)
    }

    @Test
    fun `collect should parse eth_sendTransaction correctly`() {
        // Arrange
        val rpcMethod = "eth_sendTransaction"
        val rpcParams = "[{\"to\": \"0x1234567890abcdef\", \"from\": \"0x1234567890abcdef\"}]"
        val chainId = "1"

        // Act
        val result = tvf.collect(rpcMethod, rpcParams, chainId)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("eth_sendTransaction"), result.first)
        assertEquals(listOf("0x1234567890abcdef"), result.second)
        assertEquals("1", result.third)
    }

    @Test
    fun `collect should return default value when parsing eth_sendTransaction fails`() {
        // Arrange
        val rpcMethod = "eth_sendTransaction"
        val rpcParams = "{malformed_json}"
        val chainId = "1"

        // Act
        val result = tvf.collect(rpcMethod, rpcParams, chainId)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("eth_sendTransaction"), result.first)
        assertEquals(null, result.second)
        assertEquals("1", result.third)
    }

    @Test
    fun `collectTxHashes should return the rpcResult for evm and wallet methods`() {
        // Arrange
        val rpcMethod = "eth_sendTransaction"
        val rpcResult = "0x123abc"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("0x123abc"), result)
    }

    @Test
    fun `collectTxHashes should parse solana_signTransaction and return the signature`() {
        // Arrange
        val rpcMethod = "solana_signTransaction"
        val rpcResult = "{\"signature\": \"0xsignature123\"}"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("0xsignature123"), result)
    }

    @Test
    fun `collectTxHashes should return null when parsing solana_signTransaction fails`() {
        // Arrange
        val rpcMethod = "solana_signTransaction"
        val rpcResult = "{malformed_json}"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNull(result)
    }

    @Test
    fun `collectTxHashes should parse solana_signAndSendTransaction and return the signature`() {
        // Arrange
        val rpcMethod = "solana_signAndSendTransaction"
        val rpcResult = "{\"signature\": \"0xsendAndSignSignature\"}"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals(listOf("0xsendAndSignSignature"), result)
    }

    @Test
    fun `collectTxHashes should parse solana_signAllTransactions and return all transactions`() {
        // Arrange
        val rpcMethod = "solana_signAllTransactions"
        val rpcResult =
            "{\"transactions\": [\"AYxQUCwuEoBMHp45bxp9yyegtoVUcyyc0idYrBan1PW/mWWA4MrXsbytuJt9FP1tXH5ZxYYyKc3YmBM+hcueqA4BAAIDb3ObYkq6BFd46JrMFy1h0Q+dGmyRGtpelqTKkIg82isAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMGRm/lIRcy/+ytunLDm+e8jOW7xfcSayxDmzpAAAAAanHwLXEo8xArFhOhqld18H+7VdHJSIY4f27y1qCK4AoDAgAFAlgCAAABAgAADAIAAACghgEAAAAAAAIACQMgTgAAAAAAAA==\", \"AWHu1QYry2PqYQAxDBXUtxBjRorQecJEVzje2rVY2rKJ6usAMAC/f0GGSqxpWlaS93wIfg3FqPPMzAKDdxgTwQwBAAIDb3ObYkq6BFd46JrMFy1h0Q+dGmyRGtpelqTKkIg82isAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMGRm/lIRcy/+ytunLDm+e8jOW7xfcSayxDmzpAAAAA58ONgFXrro2UqR0pvpUDFIqAYRJMYUnemdWXhfWu8VcDAgAFAlgCAAABAgAADAIAAACghgEAAAAAAAIACQMgTgAAAAAAAA==\", \"AeJw688VKMWEeOHsYhe03By/2rqJHTQeq6W4L1ZLdbT2l/Nim8ctL3erMyH9IWPsQP73uaarRmiVfanEJHx7uQ4BAAIDb3ObYkq6BFd46JrMFy1h0Q+dGmyRGtpelqTKkIg82isAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMGRm/lIRcy/+ytunLDm+e8jOW7xfcSayxDmzpAAAAAtIy17v5fs39LuoitzpBhVrg8ZIQF/3ih1N9dQ+X3shEDAgAFAlgCAAABAgAADAIAAACghgEAAAAAAAIACQMjTgAAAAAAAA==\"]}"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        //Assert
        assert(result != null)
        assert(result!!.isNotEmpty())
    }

    @Test
    fun `collectTxHashes should parse solana_signAllTransactions and null`() {
        // Arrange
        val rpcMethod = "solana_signAllTransactions"
        val rpcResult =
            "{\"transactions\": [\"asdasdsfasdfsd\"]}"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        //Assert
        assert(result == null)
    }

    @Test
    fun `collectTxHashes should not parse malformed solana_signAllTransactions and null`() {
        // Arrange
        val rpcMethod = "solana_signAllTransactions"
        val rpcResult = "{\"transactions\": }"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        //Assert
        assert(result == null)
    }

    @Test
    fun `collectTxHashes should not parse empty solana_signAllTransactions result`() {
        // Arrange
        val rpcMethod = "solana_signAllTransactions"
        val rpcResult = ""

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        //Assert
        assert(result == null)
    }

    @Test
    fun `collectTxHashes should parse cosmos_signDirect and calculate transaction hash`() {
        // Arrange
        val rpcMethod = "cosmos_signDirect"
        val rpcResult = """
            {
                "signature": {
                    "pub_key": {
                        "type": "tendermint/PubKeySecp256k1",
                        "value": "A0uwn5KKyQOJRHOJEf+voctVp6RTbW89eaNPy2Ds6xfJ"
                    },
                    "signature": "2JzNmXJSA1I+LjWBoW9ZoE+VyhHTQ9kFHdv81jA6wf0s/vjdneeTrErp+wXfjACFkCoCTmUq8I28W8kmonEUDw=="
                },
                "signed": {
                    "chainId": "cosmoshub-4",
                    "accountNumber": "1",
                    "authInfoBytes": "0a0a0a0012040a020801180112130a0d0a0575636f736d12043230303010c09a0c",
                    "bodyBytes": "0a90010a1c2f636f736d6f732e62616e6b2e763162657461312e4d736753656e6412700a2d636f736d6f7331706b707472653766646b6c366766727a6c65736a6a766878686c63337234676d6d6b38727336122d636f736d6f7331717970717870713971637273737a673270767871367273307a716733797963356c7a763778751a100a0575636f736d120731323334353637"
                }
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        println("Cosmos transaction hash: ${result?.firstOrNull()}")
        assert(result!!.isNotEmpty())
    }

    @Test
    fun `collectTxHashes should parse tron_signTransaction and extract txID`() {
        // Arrange
        val rpcMethod = "tron_signTransaction"
        val rpcResult = """
            {
                "txID": "66e79c6993f29b02725da54ab146ffb0453ee6a43b4083568ad9585da305374a",
                "signature": [
                  "7e760cef94bc82a7533bc1e8d4ab88508c6e13224cd50cc8da62d3f4d4e19b99514f..."
                ],
                "raw_data": {
                  "expiration": 1745849082000,
                  "contract": [
                    {
                      "parameter": {
                        "type_url": "type.googleapis.com/protocol.TriggerSmartContract",
                        "value": {
                          "data": "095ea7b30000000000000000000000001cb0b7348eded93b8d0816bbeb819fc1d7a51f310000000000000000000000000000000000000000000000000000000000000000",
                          "contract_address": "41a614f803b6fd780986a42c78ec9c7f77e6ded13c",
                          "owner_address": "411cb0b7348eded93b8d0816bbeb819fc1d7a51f31"
                        }
                      },
                      "type": "TriggerSmartContract"
                    }
                  ],
                  "ref_block_hash": "baa1c278fd0a309f",
                  "fee_limit": 200000000,
                  "timestamp": 1745849022978,
                  "ref_block_bytes": "885b"
                },
                "visible": false,
                "raw_data_hex": "0a02885b2208baa1c278fd0a309f4090c1dbe5e7325aae01081f12a9010a31747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e54726967676572536d617274436f6e747261637412740a15411cb0b7348eded93b8d0816bbeb819fc1d7a51f31121541a614f803b6fd780986a42c78ec9c7f77e6ded13c2244095ea7b30000000000000000000000001cb0b7348eded93b8d0816bbeb819fc1d7a51f3100000000000000000000000000000000000000000000000000000000000000007082f4d7e5e73290018084af5f"
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals("66e79c6993f29b02725da54ab146ffb0453ee6a43b4083568ad9585da305374a", result?.firstOrNull())
        println("Tron transaction ID: ${result?.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should parse hedera_signAndExecuteTransaction and extract transactionId`() {
        // Arrange
        val rpcMethod = "hedera_signAndExecuteTransaction"
        val rpcResult = """
            {
                "nodeId": "0.0.3",
                "transactionHash": "252b8fd...",
                "transactionId": "0.0.12345678@1689281510.675369303"
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals("0.0.12345678@1689281510.675369303", result?.firstOrNull())
        println("Hedera transaction ID: ${result?.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should parse hedera_executeTransaction and extract transactionId`() {
        // Arrange
        val rpcMethod = "hedera_executeTransaction"
        val rpcResult = """
            {
                "nodeId": "0.0.3",
                "transactionHash": "252b8fd...",
                "transactionId": "0.0.12345678@1689281510.675369303"
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals("0.0.12345678@1689281510.675369303", result?.firstOrNull())
        println("Hedera transaction ID: ${result?.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should parse xrpl_signTransaction and extract hash`() {
        // Arrange
        val rpcMethod = "xrpl_signTransaction"
        val rpcResult = """
            {
                "tx_json": {
                    "Account": "rMBzp8CgpE441cp5PVyA9rpVV7oT8hP3ys",
                    "Expiration": 595640108,
                    "Fee": "10",
                    "Flags": 524288,
                    "OfferSequence": 1752791,
                    "Sequence": 1752792,
                    "LastLedgerSequence": 7108682,
                    "SigningPubKey": "03EE83BB432547885C219634A1BC407A9DB0474145D69737D09CCDC63E1DEE7FE3",
                    "TakerGets": "15000000000",
                    "TakerPays": {
                        "currency": "USD",
                        "issuer": "rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B",
                        "value": "7072.8"
                    },
                    "TransactionType": "OfferCreate",
                    "TxnSignature": "30440220143759437C04F7B61F012563AFE90D8DAFC46E86035E1D965A9CED282C97D4CE02204CFD241E86F17E011298FC1A39B63386C74306A5DE047E213B0F29EFA4571C2C",
                    "hash": "73734B611DDA23D3F5F62E20A173B78AB8406AC5015094DA53F53D39B9EDB06C"
                }
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals("73734B611DDA23D3F5F62E20A173B78AB8406AC5015094DA53F53D39B9EDB06C", result?.firstOrNull())
        println("XRPL transaction hash: ${result?.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should parse xrpl_signTransactionFor and extract hash`() {
        // Arrange
        val rpcMethod = "xrpl_signTransactionFor"
        val rpcResult = """
            {
                "submit": true,
                "tx_signer": "rJ4wmkgK8n93UjtaqQTaj1vxBwQWdLrBjP",
                "tx_json": {
                    "Account": "rh2EsAe2xVE71ZBjx7oEL2zpD4zmSs3sY9",
                    "TransactionType": "Payment",
                    "Amount": "400000000000000",
                    "Destination": "r9NpyVfLfUG8hatuCCHKzosyDtKnBdsEN3",
                    "Fee": "5000",
                    "Flags": 2147483648,
                    "LastLedgerSequence": 73541531,
                    "Sequence": 38,
                    "Signers": [
                        {
                            "Signer": {
                                "Account": "re3LGjhrCvthtWWwrfKbVJjXN9PYDeQDJ",
                                "SigningPubKey": "0320ECD5569CAFA4E23147BE238DBFB268DB3B5A502ED339387AC7DCA0ADC6FB90",
                                "TxnSignature": "3045022100EC2BF025E748A028187EDB3C350D518F91F05BC201EAFC9C92566DE9E48AA1B7022018847D172386E93679630E3905BD30481359E5766931944F79F1BA6D910F5C01"
                            }
                        },
                        {
                            "Signer": {
                                "Account": "rpcL6T32dYb6FDgdm4CnC1DZQSoMvvkLRd",
                                "SigningPubKey": "030BF97DA9A563A9A0679DD527F615CF8EA6B2DB55543075B72822B8D39910B5E1",
                                "TxnSignature": "304402201A891AF3945C81E2D6B95213B79E9A31635209AF0FB94DA8C0983D15F454179B0220388679E02CE6DE2AAC904A9C2F42208418BEF60743A7F9F76FC36D519902DA8C"
                            }
                        },
                        {
                            "Signer": {
                                "Account": "r3vw3FnkXn2L7St45tzpySZsXVgG75seNk",
                                "SigningPubKey": "030BE281F6DFF9AFD260003375B64235DDBCD5B7A54511BE3DA1FEF1ADE4A85D87",
                                "TxnSignature": "3044022049D36ACE39F1208B4C78A1550F458E54E21161FA4B52B3763C8FA9C4FE45B52C022003BE3579B5B5558A27BB7DC6A8ED163999A451665974138298469C1FDACA615F"
                            }
                        }
                    ],
                    "SigningPubKey": "",
                    "hash": "31CB9DD1B9B7ADC7DCF820FAF4188053F47AE474B71AF0C6F88AF91440C0B822"
                }
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals("31CB9DD1B9B7ADC7DCF820FAF4188053F47AE474B71AF0C6F88AF91440C0B822", result?.firstOrNull())
        println("XRPL signTransactionFor hash: ${result?.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should parse sendTransfer and extract txid`() {
        // Arrange
        val rpcMethod = "sendTransfer"
        val rpcResult = """
            {
                "txid": "f007551f169722ce74104d6673bd46ce193c624b8550889526d1b93820d725f7"
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals("f007551f169722ce74104d6673bd46ce193c624b8550889526d1b93820d725f7", result?.firstOrNull())
        println("Bitcoin transaction ID: ${result?.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should parse stacks_stxTransfer and extract txId`() {
        // Arrange
        val rpcMethod = "stacks_stxTransfer"
        val rpcResult = """
            {
                "txId": "stack_tx_id",
                "txRaw": "raw_tx_hex"
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assertEquals("stack_tx_id", result?.firstOrNull())
        println("Stacks transaction ID: ${result?.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should calculate the correct hash for cosmos_signDirect`() {
        // Arrange
        val rpcMethod = "cosmos_signDirect"
        val rpcResult = """
            {
              "signature": {
                "pub_key": {
                  "type": "tendermint/PubKeySecp256k1",
                  "value": "AgSEjOuOr991QlHCORRmdE5ahVKeyBrmtgoYepCpQGOW"
                },
                "signature": "S7BJEbiXQ6vxvF9o4Wj7qAcocMQqBsqz+NVH4wilhidFsRpyqpSP5XiXoQZxTDrT9uET/S5SH6+5gUmjYntH/Q=="
              },
              "signed": {
                "chainId": "cosmoshub-4",
                "accountNumber": "1",
                "authInfoBytes": "ClAKRgofL2Nvc21vcy5jcnlwdG8uc2VjcDI1NmsxLlB1YktleRIjCiEDNOXj4u60JFq00+VbLBCNBTYy76Pn864AvYNFG/9cQwMSBAoCCH8YAhITCg0KBXVhdG9tEgQ0NTM1EIaJCw==",
                "bodyBytes": "CpoICikvaWJjLmFwcGxpY2F0aW9ucy50cmFuc2Zlci52MS5Nc2dUcmFuc2ZlchLsBwoIdHJhbnNmZXISC2NoYW5uZWwtMTQxGg8KBXVhdG9tEgYxODg4MDYiLWNvc21vczFhanBkZndsZmRqY240MG5yZXN5ZHJxazRhOGo2ZG0wemY0MGszcSo/b3NtbzEwYTNrNGh2azM3Y2M0aG54Y3R3NHA5NWZoc2NkMno2aDJybXgwYXVrYzZybTh1OXFxeDlzbWZzaDd1MgcIARDFt5YRQsgGeyJ3YXNtIjp7ImNvbnRyYWN0Ijoib3NtbzEwYTNrNGh2azM3Y2M0aG54Y3R3NHA5NWZoc2NkMno2aDJybXgwYXVrYzZybTh1OXFxeDlzbWZzaDd1IiwibXNnIjp7InN3YXBfYW5kX2FjdGlvbiI6eyJ1c2VyX3N3YXAiOnsic3dhcF9leGFjdF9hc3NldF9pbiI6eyJzd2FwX3ZlbnVlX25hbWUiOiJvc21vc2lzLXBvb2xtYW5hZ2VyIiwib3BlcmF0aW9ucyI6W3sicG9vbCI6IjE0MDAiLCJkZW5vbV9pbiI6ImliYy8yNzM5NEZCMDkyRDJFQ0NENTYxMjNDNzRGMzZFNEMxRjkyNjAwMUNFQURBOUNBOTdFQTYyMkIyNUY0MUU1RUIyIiwiZGVub21fb3V0IjoidW9zbW8ifSx7InBvb2wiOiIxMzQ3IiwiZGVub21faW4iOiJ1b3NtbyIsImRlbm9tX291dCI6ImliYy9ENzlFN0Q4M0FCMzk5QkZGRjkzNDMzRTU0RkFBNDgwQzE5MTI0OEZDNTU2OTI0QTJBODM1MUFFMjYzOEIzODc3In1dfX0sIm1pbl9hc3NldCI6eyJuYXRpdmUiOnsiZGVub20iOiJpYmMvRDc5RTdEODNBQjM5OUJGRkY5MzQzM0U1NEZBQTQ4MEMxOTEyNDhGQzU1NjkyNEEyQTgzNTFBRTI2MzhCMzg3NyIsImFtb3VudCI6IjMzOTQ2NyJ9fSwidGltZW91dF90aW1lc3RhbXAiOjE3NDc3NDY3MzM3OTU4OTgzNjQsInBvc3Rfc3dhcF9hY3Rpb24iOnsiaWJjX3RyYW5zZmVyIjp7ImliY19pbmZvIjp7InNvdXJjZV9jaGFubmVsIjoiY2hhbm5lbC02OTk0IiwicmVjZWl2ZXIiOiJjZWxlc3RpYTFhanBkZndsZmRqY240MG5yZXN5ZHJxazRhOGo2ZG0wemNsN3h0ZCIsIm1lbW8iOiIiLCJyZWNvdmVyX2FkZHJlc3MiOiJvc21vMWFqcGRmd2xmZGpjbjQwbnJlc3lkcnFrNGE4ajZkbTB6cHd1eDhqIn19fSwiYWZmaWxpYXRlcyI6W119fX19"
              }
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        val expectedHash = "A7284BA475C55983E5BCB7D52F5C82CBFF19FD75725F5E0E33BA4384FCFC6052"
        assertEquals(expectedHash, result?.firstOrNull())
        println("Cosmos transaction hash: ${result?.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should calculate the correct hash for cosmos_signAmino`() {
        // Arrange
        val rpcMethod = "cosmos_signAmino"
        val rpcResult = """
            {
            "signature": {
            "pub_key": {
              "type": "tendermint/PubKeySecp256k1",
              "value": "ApEvbc+4QJiDoaaXG6v0K3A4zmPON8pu4uzCm4o142u/"
            },
            "signature":
                "pbJYwlEfO7kZijRY+ut0dCauF84iQR/10/Y81KfLOWISNVJIjQrQhN9PxbzqQSWeNAcNgH3kbozjIuiBaoIQgQ=="
          },
          "signed": {
            "chain_id": "cosmoshub-2",
            "account_number": "15652",
            "sequence": "12323",
            "memo": "",
            "msgs": [
              {
                "type": "cosmos-sdk/MsgSend",
                "value": {
                  "from_address":
                      "cosmos1nm0rrq86ucezaf8uj35pq9fpwr5r82cl8sc7p5",
                  "to_address": "cosmos1z89utvygweg5l56fsk8ak7t6hh88fd0a52369g",
                  "amount": [
                    {
                      "denom": "uatom",
                      "amount": "518046831",
                    }
                  ]
                }
              }
            ],
            "fee": {
              "amount": [
                {
                  "denom": "uatom",
                  "amount": "300",
                }
              ],
              "gas": "40000",
            }
          }
        }
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        val expectedHash = "00438AC8446A751FFC2D1DF837941D526F7CAF1838A3B3EB9C1EAB90EEFC8B85"
        assertEquals(expectedHash, result?.firstOrNull())
        println("Cosmos transaction hash: ${result?.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should calculate the correct txid for algo_signTxn`() {
        // Arrange
        val rpcMethod = "algo_signTxn"
        val rpcResult = """
                [
                    "gqNzaWfEQNGPgbxS9pTu0sTikT3cJVO48WFltc8MM8meFR+aAnGwOo3FO+0nFkAludT0jNqHRM6E65gW6k/m92sHVCxVnQWjdHhuiaNhbXTOAAehIKNmZWXNA+iiZnbOAv0CO6NnZW6sbWFpbm5ldC12MS4womdoxCDAYcTY/B293tLXYEvkVo4/bQQZh6w3veS2ILWrOSSK36Jsds4C/QYjo3JjdsQgeqRNTBEXudHx2kO9Btq289aRzj5DlNUw0jwX9KEnaZqjc25kxCDH1s5tvgARbjtHceUG07Sj5IDfqzn7Zwx0P+XuvCYMz6R0eXBlo3BheQ=="
                ]
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        val expectedTxId = "OM5JS3AE4HVAT5ZMCIMY32HPD6KJAQVPFS2LL2ZW2R5JKUKZFVNA"
        println("Algorand transaction ID: ${result?.firstOrNull()}")
        assertEquals(expectedTxId, result?.firstOrNull())
    }

    @Test
    fun `collectTxHashes should calculate the correct txid for sui_signTransaction`() {
        // Arrange
        val rpcMethod = "sui_signTransaction"
        val rpcResult = """
                {
                    "transactionBytes": "AAACAAhkAAAAAAAAAAAg1fZH7bd9T9ox0DBFBkR/s8kuVar3e8XtS3fDMt1GBfoCAgABAQAAAQEDAAAAAAEBANX2R+23fU/aMdAwRQZEf7PJLlWq93vF7Ut3wzLdRgX6At/pRJzj2VpZgqXpSvEtd3GzPvt99hR8e/yOCGz/8nbRmA7QFAAAAAAgBy5vStJizn76LmJTBlDiONdR/2rSuzzS4L+Tp/Zs4hZ8cBxYkcSlxBD6QXvgS11E6d+DNek8LiA/beba6iH3l5gO0BQAAAAAIMpdmZjiqJ5GG9di1MAgD4S3uRr2gaMC7S1WsaeBwNIx1fZH7bd9T9ox0DBFBkR/s8kuVar3e8XtS3fDMt1GBfroAwAAAAAAAECrPAAAAAAAAA==",
                    "signature": "AGte9GqgPwHIzFSr/A4RdYqcgk2QJof0m8pHt06+WsIrw3vU40B+HGpQS/KaA9nPh12i/A7tIp6j5DGPoKM44AEZXLv/ORduxMYX0fw8dbHlnWC8WG0ymrlAmARpEibbhw=="
                }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        val digest = "6J6rTsYexRVWS2umCHVkDekUP61AEcXaJfVDZHXRJVCJ"//"C98G1Uwh5soPMtZZmjUFwbVzWLMoAHzi5jrX2BtABe8v"
        println("SUI digest: ${result?.firstOrNull()}")
        assertEquals(digest, result?.firstOrNull())
    }

    @Test
    fun `collectTxHashes should calculate the correct hash for near_signTransaction`() {
        // Arrange
        val rpcMethod = "near_signTransaction"
        val rpcResult = """
            {
                "type": "Buffer",
                "data": [16, 0, 0, 0, 48, 120, 103, 97, 110, 99, 104, 111, 46, 116, 101, 115, 116, 110, 101, 116, 0, 243, 74, 204, 31, 29, 80, 146, 149, 102, 175, 8, 83, 231, 187, 5, 120, 41, 115, 247, 22, 197, 120, 182, 242, 120, 135, 73, 137, 166, 246, 171, 103, 77, 243, 34, 42, 212, 180, 0, 0, 16, 0, 0, 0, 48, 120, 103, 97, 110, 99, 104, 111, 46, 116, 101, 115, 116, 110, 101, 116, 5, 233, 95, 227, 45, 10, 101, 176, 111, 124, 190, 86, 106, 27, 143, 54, 148, 125, 132, 252, 25, 71, 125, 78, 60, 242, 100, 219, 40, 168, 65, 3, 1, 0, 0, 0, 3, 0, 0, 0, 161, 237, 204, 206, 27, 194, 211, 0, 0, 0, 0, 0, 0]
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assert(result!!.isNotEmpty())
        assertEquals("EpHx79wKAn6br4G9aKaCGLpdzNc8YjrthiFonXQgskAx", result.firstOrNull())
        println("NEAR transaction hash: ${result.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should calculate the correct hash for near_signTransaction with data as map`() {
        // Arrange
        val rpcMethod = "near_signTransaction"
        val rpcResult = """
            {
                "type": "Buffer",
                "data": {
                  "0": 16,
                  "1": 0,
                  "2": 0,
                  "3": 0,
                  "4": 48,
                  "5": 120,
                  "6": 103,
                  "7": 97,
                  "8": 110,
                  "9": 99,
                  "10": 104,
                  "11": 111,
                  "12": 46,
                  "13": 116,
                  "14": 101,
                  "15": 115,
                  "16": 116,
                  "17": 110,
                  "18": 101,
                  "19": 116,
                  "20": 0,
                  "21": 243,
                  "22": 74,
                  "23": 204,
                  "24": 31,
                  "25": 29,
                  "26": 80,
                  "27": 146,
                  "28": 149,
                  "29": 102,
                  "30": 175,
                  "31": 8,
                  "32": 83,
                  "33": 231,
                  "34": 187,
                  "35": 5,
                  "36": 120,
                  "37": 41,
                  "38": 115,
                  "39": 247,
                  "40": 22,
                  "41": 197,
                  "42": 120,
                  "43": 182,
                  "44": 242,
                  "45": 120,
                  "46": 135,
                  "47": 73,
                  "48": 137,
                  "49": 166,
                  "50": 246,
                  "51": 171,
                  "52": 103,
                  "53": 77,
                  "54": 243,
                  "55": 34,
                  "56": 42,
                  "57": 212,
                  "58": 180,
                  "59": 0,
                  "60": 0,
                  "61": 16,
                  "62": 0,
                  "63": 0,
                  "64": 0,
                  "65": 48,
                  "66": 120,
                  "67": 103,
                  "68": 97,
                  "69": 110,
                  "70": 99,
                  "71": 104,
                  "72": 111,
                  "73": 46,
                  "74": 116,
                  "75": 101,
                  "76": 115,
                  "77": 116,
                  "78": 110,
                  "79": 101,
                  "80": 116,
                  "81": 5,
                  "82": 233,
                  "83": 95,
                  "84": 227,
                  "85": 45,
                  "86": 10,
                  "87": 101,
                  "88": 176,
                  "89": 111,
                  "90": 124,
                  "91": 190,
                  "92": 86,
                  "93": 106,
                  "94": 27,
                  "95": 143,
                  "96": 54,
                  "97": 148,
                  "98": 125,
                  "99": 132,
                  "100": 252,
                  "101": 25,
                  "102": 71,
                  "103": 125,
                  "104": 78,
                  "105": 60,
                  "106": 242,
                  "107": 100,
                  "108": 219,
                  "109": 40,
                  "110": 168,
                  "111": 65,
                  "112": 3,
                  "113": 1,
                  "114": 0,
                  "115": 0,
                  "116": 0,
                  "117": 3,
                  "118": 0,
                  "119": 0,
                  "120": 0,
                  "121": 161,
                  "122": 237,
                  "123": 204,
                  "124": 206,
                  "125": 27,
                  "126": 194,
                  "127": 211,
                  "128": 0,
                  "129": 0,
                  "130": 0,
                  "131": 0,
                  "132": 0,
                  "133": 0
               }
            }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assert(result!!.isNotEmpty())
        assertEquals("EpHx79wKAn6br4G9aKaCGLpdzNc8YjrthiFonXQgskAx", result.firstOrNull())
        println("NEAR transaction hash (map format): ${result.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should calculate the correct hash for near_signTransactions list`() {
        // Arrange
        val rpcMethod = "near_signTransactions"
        val rpcResult = """
            [
                {
                    "type": "Buffer",
                    "data": [16, 0, 0, 0, 48, 120, 103, 97, 110, 99, 104, 111, 46, 116, 101, 115, 116, 110, 101, 116, 0, 243, 74, 204, 31, 29, 80, 146, 149, 102, 175, 8, 83, 231, 187, 5, 120, 41, 115, 247, 22, 197, 120, 182, 242, 120, 135, 73, 137, 166, 246, 171, 103, 77, 243, 34, 42, 212, 180, 0, 0, 16, 0, 0, 0, 48, 120, 103, 97, 110, 99, 104, 111, 46, 116, 101, 115, 116, 110, 101, 116, 5, 233, 95, 227, 45, 10, 101, 176, 111, 124, 190, 86, 106, 27, 143, 54, 148, 125, 132, 252, 25, 71, 125, 78, 60, 242, 100, 219, 40, 168, 65, 3, 1, 0, 0, 0, 3, 0, 0, 0, 161, 237, 204, 206, 27, 194, 211, 0, 0, 0, 0, 0, 0]
                },
                {
                    "type": "Buffer",
                    "data": [16, 0, 0, 0, 48, 120, 103, 97, 110, 99, 104, 111, 46, 116, 101, 115, 116, 110, 101, 116, 0, 243, 74, 204, 31, 29, 80, 146, 149, 102, 175, 8, 83, 231, 187, 5, 120, 41, 115, 247, 22, 197, 120, 182, 242, 120, 135, 73, 137, 166, 246, 171, 103, 77, 243, 34, 42, 212, 180, 0, 0, 16, 0, 0, 0, 48, 120, 103, 97, 110, 99, 104, 111, 46, 116, 101, 115, 116, 110, 101, 116, 5, 233, 95, 227, 45, 10, 101, 176, 111, 124, 190, 86, 106, 27, 143, 54, 148, 125, 132, 252, 25, 71, 125, 78, 60, 242, 100, 219, 40, 168, 65, 3, 1, 0, 0, 0, 3, 0, 0, 0, 161, 237, 204, 206, 27, 194, 211, 0, 0, 0, 0, 0, 0]
                }
            ]
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assert(result!!.isNotEmpty())
        assertEquals("EpHx79wKAn6br4G9aKaCGLpdzNc8YjrthiFonXQgskAx", result.firstOrNull())
        assertEquals("EpHx79wKAn6br4G9aKaCGLpdzNc8YjrthiFonXQgskAx", result[1])
        println("NEAR transaction hash: ${result.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should calculate the correct hash for near_signTransactions map`() {
        // Arrange
        val rpcMethod = "near_signTransactions"
        val rpcResult = """
            [
                {
                    "type": "Buffer",
                    "data": {
                          "0": 16,
                  "1": 0,
                  "2": 0,
                  "3": 0,
                  "4": 48,
                  "5": 120,
                  "6": 103,
                  "7": 97,
                  "8": 110,
                  "9": 99,
                  "10": 104,
                  "11": 111,
                  "12": 46,
                  "13": 116,
                  "14": 101,
                  "15": 115,
                  "16": 116,
                  "17": 110,
                  "18": 101,
                  "19": 116,
                  "20": 0,
                  "21": 243,
                  "22": 74,
                  "23": 204,
                  "24": 31,
                  "25": 29,
                  "26": 80,
                  "27": 146,
                  "28": 149,
                  "29": 102,
                  "30": 175,
                  "31": 8,
                  "32": 83,
                  "33": 231,
                  "34": 187,
                  "35": 5,
                  "36": 120,
                  "37": 41,
                  "38": 115,
                  "39": 247,
                  "40": 22,
                  "41": 197,
                  "42": 120,
                  "43": 182,
                  "44": 242,
                  "45": 120,
                  "46": 135,
                  "47": 73,
                  "48": 137,
                  "49": 166,
                  "50": 246,
                  "51": 171,
                  "52": 103,
                  "53": 77,
                  "54": 243,
                  "55": 34,
                  "56": 42,
                  "57": 212,
                  "58": 180,
                  "59": 0,
                  "60": 0,
                  "61": 16,
                  "62": 0,
                  "63": 0,
                  "64": 0,
                  "65": 48,
                  "66": 120,
                  "67": 103,
                  "68": 97,
                  "69": 110,
                  "70": 99,
                  "71": 104,
                  "72": 111,
                  "73": 46,
                  "74": 116,
                  "75": 101,
                  "76": 115,
                  "77": 116,
                  "78": 110,
                  "79": 101,
                  "80": 116,
                  "81": 5,
                  "82": 233,
                  "83": 95,
                  "84": 227,
                  "85": 45,
                  "86": 10,
                  "87": 101,
                  "88": 176,
                  "89": 111,
                  "90": 124,
                  "91": 190,
                  "92": 86,
                  "93": 106,
                  "94": 27,
                  "95": 143,
                  "96": 54,
                  "97": 148,
                  "98": 125,
                  "99": 132,
                  "100": 252,
                  "101": 25,
                  "102": 71,
                  "103": 125,
                  "104": 78,
                  "105": 60,
                  "106": 242,
                  "107": 100,
                  "108": 219,
                  "109": 40,
                  "110": 168,
                  "111": 65,
                  "112": 3,
                  "113": 1,
                  "114": 0,
                  "115": 0,
                  "116": 0,
                  "117": 3,
                  "118": 0,
                  "119": 0,
                  "120": 0,
                  "121": 161,
                  "122": 237,
                  "123": 204,
                  "124": 206,
                  "125": 27,
                  "126": 194,
                  "127": 211,
                  "128": 0,
                  "129": 0,
                  "130": 0,
                  "131": 0,
                  "132": 0,
                  "133": 0}
                },
                {
                    "type": "Buffer",
                    "data": {      "0": 16,
                  "1": 0,
                  "2": 0,
                  "3": 0,
                  "4": 48,
                  "5": 120,
                  "6": 103,
                  "7": 97,
                  "8": 110,
                  "9": 99,
                  "10": 104,
                  "11": 111,
                  "12": 46,
                  "13": 116,
                  "14": 101,
                  "15": 115,
                  "16": 116,
                  "17": 110,
                  "18": 101,
                  "19": 116,
                  "20": 0,
                  "21": 243,
                  "22": 74,
                  "23": 204,
                  "24": 31,
                  "25": 29,
                  "26": 80,
                  "27": 146,
                  "28": 149,
                  "29": 102,
                  "30": 175,
                  "31": 8,
                  "32": 83,
                  "33": 231,
                  "34": 187,
                  "35": 5,
                  "36": 120,
                  "37": 41,
                  "38": 115,
                  "39": 247,
                  "40": 22,
                  "41": 197,
                  "42": 120,
                  "43": 182,
                  "44": 242,
                  "45": 120,
                  "46": 135,
                  "47": 73,
                  "48": 137,
                  "49": 166,
                  "50": 246,
                  "51": 171,
                  "52": 103,
                  "53": 77,
                  "54": 243,
                  "55": 34,
                  "56": 42,
                  "57": 212,
                  "58": 180,
                  "59": 0,
                  "60": 0,
                  "61": 16,
                  "62": 0,
                  "63": 0,
                  "64": 0,
                  "65": 48,
                  "66": 120,
                  "67": 103,
                  "68": 97,
                  "69": 110,
                  "70": 99,
                  "71": 104,
                  "72": 111,
                  "73": 46,
                  "74": 116,
                  "75": 101,
                  "76": 115,
                  "77": 116,
                  "78": 110,
                  "79": 101,
                  "80": 116,
                  "81": 5,
                  "82": 233,
                  "83": 95,
                  "84": 227,
                  "85": 45,
                  "86": 10,
                  "87": 101,
                  "88": 176,
                  "89": 111,
                  "90": 124,
                  "91": 190,
                  "92": 86,
                  "93": 106,
                  "94": 27,
                  "95": 143,
                  "96": 54,
                  "97": 148,
                  "98": 125,
                  "99": 132,
                  "100": 252,
                  "101": 25,
                  "102": 71,
                  "103": 125,
                  "104": 78,
                  "105": 60,
                  "106": 242,
                  "107": 100,
                  "108": 219,
                  "109": 40,
                  "110": 168,
                  "111": 65,
                  "112": 3,
                  "113": 1,
                  "114": 0,
                  "115": 0,
                  "116": 0,
                  "117": 3,
                  "118": 0,
                  "119": 0,
                  "120": 0,
                  "121": 161,
                  "122": 237,
                  "123": 204,
                  "124": 206,
                  "125": 27,
                  "126": 194,
                  "127": 211,
                  "128": 0,
                  "129": 0,
                  "130": 0,
                  "131": 0,
                  "132": 0,
                  "133": 0}
                }
            ]
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNotNull(result)
        assert(result!!.isNotEmpty())
        assertEquals("EpHx79wKAn6br4G9aKaCGLpdzNc8YjrthiFonXQgskAx", result.firstOrNull())
        assertEquals("EpHx79wKAn6br4G9aKaCGLpdzNc8YjrthiFonXQgskAx", result[1])
        println("NEAR transaction hash: ${result.firstOrNull()}")
    }

    @Test
    fun `collectTxHashes should return null for unsupported methods`() {
        // Arrange
        val rpcMethod = "unsupported_method"
        val rpcResult = "some_result"

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult)

        // Assert
        assertNull(result)
    }

    @Test
    fun `collectTxHashes should calculate the correct hash for polkadot_signTransaction`() {
        val rpcMethod = "polkadot_signTransaction"

        val rpcParams = """
                {
                    "address": "15JBFhDp1rQycRFuCtkr2VouMiWyDzh3qRUPA8STY53mdRmM",
                    "transactionPayload": {
                          "method": "050300c07d211d3c181df768d9d9d41df6f14f9d116d9c1906f38153b208259c315b4b02286bee",
                          "specVersion": "c9550f00",
                          "transactionVersion": "1a000000",
                          "genesisHash": "91b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c3",
                          "blockHash": "af027e6af85e62cb4673e9aab17992c7d9a5952c92b9e3f008cb5ebff5e9e120",
                          "era": "f502",
                          "nonce": "4c",
                          "tip": "00",
                          "mode": "00",
                          "metadataHash": "00",
                          "blockNumber": "19113af",
                          "address": "15JBFhDp1rQycRFuCtkr2VouMiWyDzh3qRUPA8STY53mdRmM",
                          "version": 4
                        }
            }
        """.trimIndent()

        val rpcResult = """
                {
                    "id": 123456789,
                    "signature": "eefafdb542b591ab8f65cb6a85a43e9f267e5129394a0d80950cc7a89b98870da1d4b90c817c546fb960ac0e8c23073a912720c970379efcdbc845924e83588e"
                }
        """.trimIndent()

        // Act
        val result = tvf.collectTxHashes(rpcMethod, rpcResult, rpcParams)

        // Assert
        assertNotNull(result)
        assert(result!!.isNotEmpty())
        assertEquals("665cd321870f1e416dc61bac60010614d7a0892328feec468c57540b8ba1a99e", result.firstOrNull())
        println("Polkadot transaction hash: ${result.firstOrNull()}")
    }
}