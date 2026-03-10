@file:JvmSynthetic

package com.reown.sample.wallet.ui.routes.bottomsheet_routes.import_wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reown.sample.wallet.domain.StacksAccountDelegate
import com.reown.sample.wallet.domain.account.EthAccountDelegate
import com.reown.sample.wallet.domain.account.SolanaAccountDelegate
import com.reown.sample.wallet.domain.account.SuiAccountDelegate
import com.reown.sample.wallet.domain.account.TONAccountDelegate
import com.reown.sample.wallet.domain.account.TronAccountDelegate
import com.reown.sample.wallet.domain.account.bytesToHex
import com.reown.sample.wallet.domain.account.derivePrivateKeyFromMnemonic
import com.reown.sample.wallet.domain.account.normalizePrivateKeyHex
import com.reown.sample.wallet.domain.client.Keypair
import com.reown.sample.wallet.domain.client.Stacks
import com.reown.sample.wallet.domain.client.SuiUtils
import com.reown.sample.wallet.domain.client.TONClient
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.web3j.crypto.Sign
import uniffi.yttrium_utils.TronKeypair
import uniffi.yttrium_utils.solanaDeriveKeypairFromMnemonic
import uniffi.yttrium_utils.tronGetAddress
import uniffi.yttrium_utils.suiDeriveKeypairFromMnemonic
import kotlin.coroutines.resume

internal enum class ImportWalletChain(val label: String, val placeholder: String) {
    EVM("EVM", "Enter mnemonic phrase or private key (0x...)"),
    TON("TON", "Enter secret key:public key (colon separated)"),
    SOLANA("Solana", "Enter mnemonic phrase or base58 keypair"),
    SUI("SUI", "Enter mnemonic phrase or keypair string"),
    TRON("Tron", "Enter mnemonic phrase or private key (64 hex)"),
    STACKS("Stacks", "Enter wallet string"),
}

internal sealed interface ImportResult {
    data class Success(val address: String) : ImportResult
    data class Error(val message: String) : ImportResult
}

internal class ImportWalletViewModel : ViewModel() {
    private val _selectedChain = MutableStateFlow(ImportWalletChain.EVM)
    val selectedChain = _selectedChain.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult = _importResult.asStateFlow()

    fun selectChain(chain: ImportWalletChain) {
        _selectedChain.value = chain
        _inputText.value = ""
    }

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun clearResult() {
        _importResult.value = null
    }

    fun importWallet() {
        val input = _inputText.value.trim().replace(Regex("\\s+"), " ")
        if (input.isEmpty()) return

        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            _importResult.value = try {
                val address = when (_selectedChain.value) {
                    ImportWalletChain.EVM -> importEvm(input)
                    ImportWalletChain.TON -> importTon(input)
                    ImportWalletChain.SOLANA -> importSolana(input)
                    ImportWalletChain.SUI -> importSui(input)
                    ImportWalletChain.TRON -> importTron(input)
                    ImportWalletChain.STACKS -> importStacks(input)
                }
                disconnectAllSessions()
                ImportResult.Success(address)
            } catch (e: Exception) {
                ImportResult.Error(e.message ?: "Import failed")
            } finally {
                _inputText.value = ""
                _isLoading.value = false
            }
        }
    }

    private fun importEvm(input: String): String {
        return if (input.contains(" ")) {
            EthAccountDelegate.importFromMnemonic(input.lowercase())
            EthAccountDelegate.address
        } else {
            EthAccountDelegate.privateKey = normalizePrivateKeyHex(input)
            EthAccountDelegate.address
        }
    }

    private fun importTon(input: String): String {
        val parts = input.split(":", limit = 2).map { it.trim() }
        require(parts.size == 2 && parts.all { it.isNotEmpty() }) {
            "Enter secret key and public key separated by ':'"
        }
        val (sk, pk) = parts
        val wallet = TONClient.getAddressFromKeyPair(Keypair(sk, pk))
        TONAccountDelegate.importKeypair(sk, pk)
        return wallet.friendly
    }

    private fun importSolana(input: String): String {
        require(input.isNotBlank()) { "Enter a mnemonic phrase or base58 keypair" }
        val keypair = if (input.contains(" ")) {
            solanaDeriveKeypairFromMnemonic(input.lowercase(), "m/44'/501'/0'/0'")
        } else {
            input
        }
        // Validate before persistence so failed imports don't corrupt stored account state.
        val publicKey = SolanaAccountDelegate.getSolanaPubKeyForKeyPair(keypair)
        SolanaAccountDelegate.keyPair = keypair
        return publicKey
    }

    private fun importSui(input: String): String {
        require(input.isNotBlank()) { "Enter a mnemonic phrase or keypair string" }
        val keypair = if (input.contains(" ")) {
            suiDeriveKeypairFromMnemonic(input.lowercase())
        } else {
            input
        }
        // Validate before persistence so failed imports don't corrupt stored account state.
        val publicKey = SuiUtils.getPublicKeyFromKeyPair(keypair)
        val address = SuiUtils.getAddressFromPublicKey(publicKey)
        SuiAccountDelegate.keypair = keypair
        return address
    }

    private fun importTron(input: String): String {
        val sk = if (input.contains(" ")) {
            derivePrivateKeyFromMnemonic(input.lowercase(), coinType = 195)
        } else {
            normalizePrivateKeyHex(input)
        }
        val compressedPk = Sign.publicPointFromPrivate(BigInteger(sk, 16))
            .getEncoded(true)
            .bytesToHex()
        val keypair = TronKeypair(sk, compressedPk)
        val address = tronGetAddress(keypair).base58
        TronAccountDelegate.importKeypair(keypair)
        return address
    }

    private fun importStacks(input: String): String {
        require(input.isNotBlank()) { "Enter a wallet string" }
        // Validate before persistence so failed imports don't corrupt stored account state.
        Stacks.getAddress(input, Stacks.Version.mainnetP2PKH)
        StacksAccountDelegate.importedWallet = input
        return StacksAccountDelegate.mainnetAddress
    }

    private suspend fun disconnectAllSessions() {
        val sessions = WalletKit.getListOfActiveSessions()
        sessions.forEach { session ->
            disconnectSession(session.topic)
        }
    }

    private suspend fun disconnectSession(topic: String) {
        withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { continuation ->
                try {
                    WalletKit.disconnectSession(
                        params = Wallet.Params.SessionDisconnect(topic),
                        onSuccess = { if (continuation.isActive) continuation.resume(Unit) },
                        onError = { if (continuation.isActive) continuation.resume(Unit) }
                    )
                } catch (_: Exception) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }

}
