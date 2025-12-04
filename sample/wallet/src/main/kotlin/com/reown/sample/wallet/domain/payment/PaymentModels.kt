package com.reown.sample.wallet.domain.payment

import com.google.gson.JsonElement

// =============================================================================
// GET /getPaymentInfo/:paymentId - Response
// =============================================================================

/**
 * Supported asset for payment (CAIP-19 format)
 */
data class SupportedAsset(
    val asset: String,      // CAIP-19 asset identifier, e.g., "eip155:8453/erc20:0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"
    val symbol: String,     // Token symbol, e.g., "USDC"
    val name: String,       // Token name, e.g., "USD Coin"
    val chain: String,      // Chain display name, e.g., "Base"
    val chainId: String,    // CAIP-2 chain ID, e.g., "eip155:8453"
    val decimals: Int       // Token decimals, e.g., 6
)

/**
 * Response from GET /getPaymentInfo/:paymentId
 * Retrieves payment details for display (pre-wallet connect)
 */
data class PaymentInfoResponse(
    val paymentId: String,
    val amount: Int,                        // Amount in cents
    val currency: String,                   // Currency code, e.g., "USD"
    val referenceId: String,                // Merchant's reference ID
    val status: String,                     // Payment status: "pending" | "processing" | "completed" | "failed" | "expired"
    val createdAt: Long,                    // Unix timestamp
    val supportedAssets: List<SupportedAsset>   // Available payment assets (CAIP-19)
)

// =============================================================================
// POST /buildPayment - Request/Response
// =============================================================================

/**
 * Request for POST /buildPayment
 * Builds payment options for the user's connected wallet(s)
 */
data class BuildPaymentRequest(
    val paymentId: String,
    val accounts: List<String>              // User wallet accounts in CAIP-10 format, e.g., "eip155:8453:0x742d..."
)

/**
 * Signing request object returned in payment options
 */
data class SigningRequest(
    val method: String,                     // "eth_signTypedData_v4" | "eth_sendTransaction" | "solana_signTransaction"
    val params: List<JsonElement>           // Parameters for the signing method
)

/**
 * Payment option with signing request
 */
data class PaymentOption(
    val asset: String,                      // CAIP-19 asset identifier
    val symbol: String,                     // Token symbol
    val name: String,                       // Token name
    val chain: String,                      // Chain display name
    val chainId: String,                    // CAIP-2 chain ID
    val decimals: Int,                      // Token decimals
    val amount: String,                     // Amount to pay (in smallest unit)
    val balance: String,                    // User's balance (in smallest unit)
    val sufficient: Boolean,                // Whether balance covers payment
    val signingRequest: SigningRequest?,    // Ready-to-use signing request (null if insufficient balance)
    val priceUsd: Double? = null,           // (native tokens only) USD price at quote time
    val expiresAt: Long? = null             // (native tokens only) Quote expiration timestamp
)

/**
 * Response from POST /buildPayment
 * Returns signing requests for all viable options based on balances
 */
data class BuildPaymentResponse(
    val options: List<PaymentOption>
)

// =============================================================================
// POST /submit - Request/Response
// =============================================================================

/**
 * Request for POST /submit
 * Submits the signed transaction to execute the payment
 */
data class SubmitPaymentRequest(
    val paymentId: String,
    val signature: String,                  // Signed transaction/authorization
    val asset: String                       // CAIP-19 asset ID for the option being executed
)

/**
 * Response from POST /submit
 */
data class SubmitPaymentResponse(
    val status: String,                     // "completed" | "failed"
    val txHash: String? = null,             // Transaction hash
    val chainId: Int? = null,               // Numeric chain ID
    val chainName: String? = null,          // Human-readable chain name
    val token: String? = null,              // Token symbol used
    val error: String? = null               // Error message (if failed)
)

// =============================================================================
// Typed Data structures for EIP-712 signing
// =============================================================================

/**
 * Typed Data payload for eth_signTypedData_v4
 */
data class PaymentTypedDataPayload(
    val types: Map<String, List<PaymentTypeField>>,  // Flexible map to handle different authorization types
    val domain: PaymentDomain,
    val primaryType: String,                // e.g., "TransferWithAuthorization" or "ReceiveWithAuthorization"
    val message: PaymentAuthorizationMessage
)

data class PaymentTypeField(
    val name: String,
    val type: String
)

data class PaymentDomain(
    val name: String,                       // Token name, e.g., "USD Coin"
    val version: String,                    // Token version, e.g., "2"
    val chainId: Long,                      // Chain ID
    val verifyingContract: String           // Token contract address
)

data class PaymentAuthorizationMessage(
    val from: String,                       // Payer address
    val to: String,                         // Merchant address
    val value: String,                      // Amount as string
    val validAfter: Long,                   // Unix timestamp
    val validBefore: Long,                  // Unix timestamp
    val nonce: String                       // Unique nonce for this authorization
)

// =============================================================================
// ERC-3009 Authorization with signature components
// =============================================================================

data class PaymentAuthorization(
    val from: String,
    val to: String,
    val value: String,
    val validAfter: Long,
    val validBefore: Long,
    val nonce: String,
    val v: Int,
    val r: String,
    val s: String
)
