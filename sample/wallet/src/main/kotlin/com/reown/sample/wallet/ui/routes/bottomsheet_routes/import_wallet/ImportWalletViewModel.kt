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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.web3j.crypto.ECKeyPair

internal enum class ImportWalletChain(val label: String, val placeholder: String) {
    EVM("EVM", "Enter mnemonic phrase or private key (0x...)"),
    TON("TON", "Enter secret key:public key (hex, colon separated)"),
    SOLANA("Solana", "Enter base58-encoded keypair"),
    SUI("SUI", "Enter keypair string"),
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

        viewModelScope.launch {
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
                ImportResult.Success(address)
            } catch (e: Exception) {
                ImportResult.Error(e.message ?: "Import failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun importEvm(input: String): String {
        // Auto-detect: spaces → mnemonic, otherwise → private key
        return if (input.contains(" ")) {
            EthAccountDelegate.importFromMnemonic(input)
            EthAccountDelegate.address
        } else {
            val key = normalizePrivateKeyHex(input)
            EthAccountDelegate.privateKey = key
            EthAccountDelegate.address
        }
    }

    private fun importTon(input: String): String {
        val parts = input.split(":", limit = 2).map { it.trim() }
        require(parts.size == 2) {
            "Enter secret key and public key separated by ':'"
        }
        val (sk, pk) = parts
        require(sk.length in listOf(64, 128) && sk.all { it.isHexChar() }) {
            "Invalid secret key format"
        }
        require(pk.length == 64 && pk.all { it.isHexChar() }) {
            "Public key must be 64 hex characters"
        }
        // Order matters: set publicKey first, then secretKey
        TONAccountDelegate.publicKey = pk
        TONAccountDelegate.secretKey = sk
        return TONAccountDelegate.addressFriendly
    }

    private fun importSolana(input: String): String {
        require(input.isNotBlank()) { "Enter a base58-encoded keypair" }
        SolanaAccountDelegate.keyPair = input
        return SolanaAccountDelegate.keys.second
    }

    private fun importSui(input: String): String {
        require(input.isNotBlank()) { "Enter a keypair string" }
        SuiAccountDelegate.keypair = input
        return SuiAccountDelegate.address
    }

    private fun importTron(input: String): String {
        val sk = if (input.contains(" ")) {
            derivePrivateKeyFromMnemonic(input, coinType = 195)
        } else {
            normalizePrivateKeyHex(input)
        }
        val ecKeypair = ECKeyPair.create(io.ipfs.multibase.Base16.decode(sk.lowercase()))
        val pk = ecKeypair.publicKey.toByteArray().bytesToHex()
        TronAccountDelegate.publicKey = pk
        TronAccountDelegate.secretKey = sk
        return TronAccountDelegate.address
    }

    private fun importStacks(input: String): String {
        require(input.isNotBlank()) { "Enter a wallet string" }
        StacksAccountDelegate.importedWallet = input
        return StacksAccountDelegate.mainnetAddress
    }

    private fun Char.isHexChar(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
