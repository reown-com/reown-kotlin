import com.reown.android.BuildConfig
import com.reown.walletkit.client.WalletKit
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildWalletServiceTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test buildWalletService with empty methods list`() {
        // Arrange
        val projectId = "test-project"
        val methods = emptyList<String>()

        // Act
        val result = WalletKit.buildWalletService(projectId, methods)

        // Assert
        val expected = "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=test-project&st=wkca&sv=reown-kotlin-${BuildConfig.SDK_VERSION}\", \"methods\":[]}]}"
        assertEquals(expected, result)
    }

    @Test
    fun `test buildWalletService with single method`() {
        // Arrange
        val projectId = "test-project"
        val methods = listOf("wallet_getAssets")

        // Act
        val result = WalletKit.buildWalletService(projectId, methods)

        // Assert
        val expected = "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=test-project&st=wkca&sv=reown-kotlin-${BuildConfig.SDK_VERSION}\", \"methods\":[\"wallet_getAssets\"]}]}"
        assertEquals(expected, result)
    }

    @Test
    fun `test buildWalletService with multiple methods`() {
        // Arrange
        val projectId = "test-project"
        val methods = listOf("wallet_getAssets", "wallet_signMessage", "wallet_sendTransaction")

        // Act
        val result = WalletKit.buildWalletService(projectId, methods)

        // Assert
        val expected = "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=test-project&st=wkca&sv=reown-kotlin-${BuildConfig.SDK_VERSION}\", \"methods\":[\"wallet_getAssets\",\"wallet_signMessage\",\"wallet_sendTransaction\"]}]}"
        assertEquals(expected, result)
    }

    @Test
    fun `test buildWalletService with special characters in projectId`() {
        // Arrange
        val projectId = "test-project@123"
        val methods = listOf("wallet_getAssets")

        // Act
        val result = WalletKit.buildWalletService(projectId, methods)

        // Assert
        val expected = "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=test-project@123&st=wkca&sv=reown-kotlin-${BuildConfig.SDK_VERSION}\", \"methods\":[\"wallet_getAssets\"]}]}"
        assertEquals(expected, result)
    }

    @Test
    fun `test buildWalletService with special characters in methods`() {
        // Arrange
        val projectId = "test-project"
        val methods = listOf("wallet_getAssets", "method-with:special@characters")

        // Act
        val result = WalletKit.buildWalletService(projectId, methods)

        // Assert
        val expected = "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=test-project&st=wkca&sv=reown-kotlin-${BuildConfig.SDK_VERSION}\", \"methods\":[\"wallet_getAssets\",\"method-with:special@characters\"]}]}"
        assertEquals(expected, result)
    }

    @Test
    fun `test buildWalletService with empty projectId`() {
        // Arrange
        val projectId = ""
        val methods = listOf("wallet_getAssets")

        // Act
        val result = WalletKit.buildWalletService(projectId, methods)

        // Assert
        val expected = "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=&st=wkca&sv=reown-kotlin-${BuildConfig.SDK_VERSION}\", \"methods\":[\"wallet_getAssets\"]}]}"
        assertEquals(expected, result)
    }

    @Test
    fun `test buildWalletService with methods containing quotes`() {
        // Arrange
        val projectId = "test-project"
        val methods = listOf("wallet_getAssets", "wallet_\"quoted\"_method")

        // Act
        val result = WalletKit.buildWalletService(projectId, methods)

        // Assert
        val expected = "{\"walletService\":[{\"url\":\"https://rpc.walletconnect.org/v1/wallet?projectId=test-project&st=wkca&sv=reown-kotlin-${BuildConfig.SDK_VERSION}\", \"methods\":[\"wallet_getAssets\",\"wallet_\"quoted\"_method\"]}]}"
        assertEquals(expected, result)
    }
}