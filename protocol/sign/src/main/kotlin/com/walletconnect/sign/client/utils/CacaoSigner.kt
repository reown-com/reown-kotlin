package com.walletconnect.sign.client.utils

import com.reown.android.cacao.signature.ISignatureType
import com.reown.android.utils.cacao.CacaoSignerInterface
import com.walletconnect.sign.client.Sign

/**
 * @deprecated Only added to have backwards compatibility. Newer SDKs should only add CacaoSigner object below.
 */

@Deprecated("Moved to android-core module, as other SDKs also need CACAO.", ReplaceWith("com.reown.android.internal.common.cacao.signature.SignatureType"))
enum class SignatureType(override val header: String) : ISignatureType {
    EIP191("eip191"), EIP1271("eip1271");
}

object CacaoSigner : CacaoSignerInterface<Sign.Model.Cacao.Signature>