@file:OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterialNavigationApi::class,
    ExperimentalAnimationApi::class
)

package com.reown.sample.wallet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.pandulapeter.beagle.Beagle
import com.pandulapeter.beagle.modules.DividerModule
import com.pandulapeter.beagle.modules.HeaderModule
import com.pandulapeter.beagle.modules.LogListModule
import com.pandulapeter.beagle.modules.NetworkLogListModule
import com.pandulapeter.beagle.modules.PaddingModule
import com.pandulapeter.beagle.modules.ScreenCaptureToolboxModule
import com.pandulapeter.beagle.modules.TextInputModule
import com.pandulapeter.beagle.modules.TextModule
import com.reown.android.CoreClient
import com.reown.android.cacao.signature.SignatureType
import com.reown.android.utils.cacao.sign
import com.reown.notify.client.Notify
import com.reown.notify.client.NotifyClient
import com.reown.notify.client.cacao.CacaoSigner
import com.reown.sample.common.ui.theme.WCSampleAppTheme
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.R
import com.reown.sample.wallet.domain.EthAccountDelegate
import com.reown.sample.wallet.domain.NotifyDelegate
import com.reown.sample.wallet.ui.routes.Route
import com.reown.sample.wallet.ui.routes.composable_routes.connections.ConnectionsViewModel
import com.reown.sample.wallet.ui.routes.host.WalletSampleHost
import com.reown.util.hexToBytes
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.URLEncoder

class WalletKitActivity : AppCompatActivity() {
    private lateinit var navController: NavHostController
    private val web3walletViewModel = Web3WalletViewModel()
    private val connectionsViewModel = ConnectionsViewModel()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent(web3walletViewModel, connectionsViewModel)
        handleWeb3WalletEvents(web3walletViewModel, connectionsViewModel)
        askNotificationPermission()
        handleErrors()
        handleAppLink(intent)
        registerAccount()
        setBeagle()
    }

    private fun setContent(
        web3walletViewModel: Web3WalletViewModel,
        connectionsViewModel: ConnectionsViewModel,
    ) {
        setContent {
            val sheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            )
            val bottomSheetNavigator = BottomSheetNavigator(sheetState)
            val navController = rememberAnimatedNavController(bottomSheetNavigator)
            this.navController = navController
            val sharedPref = getPreferences(MODE_PRIVATE)
            val getStartedVisited = sharedPref.getBoolean("get_started_visited", false)
            WCSampleAppTheme {
                WalletSampleHost(
                    bottomSheetNavigator,
                    navController,
                    web3walletViewModel,
                    connectionsViewModel,
                    getStartedVisited
                )
            }
        }
    }

    private fun handleErrors() {
        NotifyDelegate.notifyErrors
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { error -> Timber.e(error.throwable) }
            .launchIn(lifecycleScope)
    }

    private fun handleWeb3WalletEvents(
        web3walletViewModel: Web3WalletViewModel,
        connectionsViewModel: ConnectionsViewModel,
    ) {
        web3walletViewModel.sessionRequestStateFlow
            .onEach {
                if (it.arrayOfArgs.isNotEmpty()) {
                    web3walletViewModel.showRequestLoader(false)
                    navigateWhenReady {
                        navController.navigate(Route.SessionRequest.path)
                    }
                }
            }
            .launchIn(lifecycleScope)

        web3walletViewModel.walletEvents
            .onEach { event ->
                when (event) {
                    is SignEvent.SessionProposal -> navigateWhenReady { navController.navigate(Route.SessionProposal.path) }
                    is SignEvent.SessionAuthenticate -> navigateWhenReady { navController.navigate(Route.SessionAuthenticate.path) }
                    is SignEvent.ExpiredRequest -> {
                        if (navController.currentDestination?.route != Route.Connections.path) {
                            navController.popBackStack(route = Route.Connections.path, inclusive = false)
                        }
                        Toast.makeText(baseContext, "Request expired", Toast.LENGTH_SHORT).show()
                    }

                    is SignEvent.Disconnect -> {
                        connectionsViewModel.refreshConnections()
                        if (navController.currentDestination?.route != Route.Connections.path) {
                            navController.navigate(Route.Connections.path)
                        }
                    }

                    else -> Unit
                }
            }
            .launchIn(lifecycleScope)
    }

    private suspend fun navigateWhenReady(navigate: () -> Unit) {
        if (!::navController.isInitialized) {
            delay(400)
            navigate()
        } else {
            navigate()
        }
    }

    private fun handleAppLink(intent: Intent?) {
        if (intent?.dataString?.contains("wc_ev") == true) {
            WalletKit.dispatchEnvelope(intent.dataString ?: "") {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@WalletKitActivity, "Error dispatching envelope: ${it.throwable.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            when {
                intent?.dataString?.startsWith("kotlin-web3wallet:/wc") == true -> {
                    val uri = intent.dataString?.replace("kotlin-web3wallet:/wc", "kotlin-web3wallet://wc")
                    intent.setData(uri?.toUri())
                }

                intent?.dataString?.startsWith("wc:") == true && intent.dataString?.contains("requestId") == false -> {
                    val uri = "kotlin-web3wallet://wc?uri=" + URLEncoder.encode(intent.dataString, "UTF-8")
                    intent.setData(uri.toUri())
                }
            }

            if (intent?.dataString?.startsWith("kotlin-web3wallet://request") == true || intent?.dataString?.contains("requestId") == true) {
                lifecycleScope.launch {
                    navigateWhenReady {
                        if (navController.currentDestination?.route != Route.SessionRequest.path) {
                            web3walletViewModel.showRequestLoader(true)
                        }
                    }
                }
            }

            if (intent?.dataString?.startsWith("kotlin-web3wallet://request") == false && intent.dataString?.contains("requestId") == false
            ) {
                lifecycleScope.launch {
                    navigateWhenReady {
                        web3walletViewModel.pair(intent.dataString.toString())
                    }
                }
            }
        }
    }

    private fun setBeagle() {
        Beagle.set(
            HeaderModule(
                title = getString(R.string.app_name),
                subtitle = BuildConfig.APPLICATION_ID,
                text = "${BuildConfig.BUILD_TYPE} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            ),
            ScreenCaptureToolboxModule(),
            DividerModule(),
            TextModule("Logs", TextModule.Type.SECTION_HEADER),
            NetworkLogListModule(),
            LogListModule(),
            DividerModule(),
            TextModule(text = EthAccountDelegate.ethAddress) {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Account", EthAccountDelegate.ethAddress))
            },
            PaddingModule(size = PaddingModule.Size.LARGE),
            TextModule(text = EthAccountDelegate.privateKey) {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Private Key", EthAccountDelegate.privateKey))
            },
            PaddingModule(size = PaddingModule.Size.LARGE),
            TextModule(text = CoreClient.Push.clientId, id = CoreClient.Push.clientId) {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("ClientId", CoreClient.Push.clientId))
            },
            DividerModule(),
            TextInputModule(
                text = "Import Private Key",
                areRealTimeUpdatesEnabled = false,
                validator = { text ->
                    !text.startsWith("0x") && text.length == 64
                },
                onValueChanged = { text ->
                    NotifyClient.unregister(
                        params = Notify.Params.Unregister(
                            EthAccountDelegate.ethAddress,
                        ),
                        onSuccess = {
                            println("Unregister Success")
                            EthAccountDelegate.privateKey = text
                            registerAccount()
                        },
                        onError = { println(it.throwable.stackTraceToString()) }
                    )
                }
            )
        )
    }

    private fun registerAccount() {
        val account = EthAccountDelegate.ethAddress
        val domain = BuildConfig.APPLICATION_ID
        val isRegistered = NotifyClient.isRegistered(params = Notify.Params.IsRegistered(account = account, domain = domain))

        if (!isRegistered) {
            NotifyClient.prepareRegistration(
                params = Notify.Params.PrepareRegistration(account = account, domain = domain),
                onSuccess = { cacaoPayloadWithIdentityPrivateKey, message ->
                    println("PrepareRegistration Success: $cacaoPayloadWithIdentityPrivateKey")

                    val signature = CacaoSigner.sign(message, EthAccountDelegate.privateKey.hexToBytes(), SignatureType.EIP191)

                    NotifyClient.register(
                        params = Notify.Params.Register(cacaoPayloadWithIdentityPrivateKey = cacaoPayloadWithIdentityPrivateKey, signature = signature),
                        onSuccess = { println("Register Success") },
                        onError = { println(it.throwable.stackTraceToString()) }
                    )

                },
                onError = { println(it.throwable.stackTraceToString()) }
            )
        } else {
            println("$account is already registered")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleAppLink(intent)
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
