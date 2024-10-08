package com.reown.android.internal.common.exception

import com.reown.android.internal.common.model.AccountId

abstract class WalletConnectException(override val message: String?) : Exception(message)
internal class UnableToExtractDomainException(keyserverUrl: String) : WalletConnectException("Unable to extract domain from: $keyserverUrl")
internal class InvalidAccountIdException(accountId: AccountId) : WalletConnectException("AccountId: $accountId is not CAIP-10 complaint") // todo: https://github.com/WalletConnect/WalletConnectKotlinV2/issues/768
internal class UserRejectedSigning() : WalletConnectException("User rejected signing")
internal class InvalidIdentityCacao() : WalletConnectException("Invalid identity cacao")
internal class AccountHasNoIdentityStored(accountId: AccountId) : WalletConnectException("AccountId: $accountId has no identity stored")
class AccountHasNoCacaoPayloadStored(accountId: AccountId) : WalletConnectException("AccountId: $accountId has no message stored")
class AccountHasDifferentStatementStored(accountId: AccountId) : WalletConnectException("AccountId: $accountId has old statement stored")
