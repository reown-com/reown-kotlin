import com.reown.android.cacao.signature.SignatureType
import com.reown.android.internal.common.model.ProjectId
import com.reown.android.internal.common.signing.message.MessageSignatureVerifier
import com.reown.android.utils.cacao.sign
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.domain.generateKeys
import com.reown.util.hexToBytes
import com.reown.walletkit.utils.CacaoSigner
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test


internal class EthAccountDelegateTest {

    @Test
    fun canSigMessagesWithGeneratedAccount() {
        with(EthAccountDelegate) {
            val (_, privateKey, address) = generateKeys()
            val message = "dummy message"
            val signatureType = SignatureType.EIP191
            assertEquals(SignatureType.EIP191, signatureType) // Will fail with other types i.e SignatureType.EIP1271 requires projectId

            val signature = CacaoSigner.sign(message, privateKey.hexToBytes(), signatureType)
            val isValid = MessageSignatureVerifier(ProjectId(BuildConfig.PROJECT_ID)).verify(signature.s, message, address, "eip155:1", signatureType)

            assertTrue(isValid)
        }
    }

    @Test
    fun cannotVerifySignaturesWithAnotherGeneratedAccount() {
        with(EthAccountDelegate) {
            val (_, privateKey, _) = generateKeys()
            val message = "dummy message"
            val signatureType = SignatureType.EIP191
            assertEquals(SignatureType.EIP191, signatureType) // Will fail with other types i.e SignatureType.EIP1271 requires projectId

            val signature = CacaoSigner.sign(message, privateKey.hexToBytes(), signatureType)
            val address = generateKeys().third
            val isValid = MessageSignatureVerifier(ProjectId("dummy projectid")).verify(signature.s, message, address, "eip155:1", signatureType)

            assertFalse(isValid)
        }
    }
}