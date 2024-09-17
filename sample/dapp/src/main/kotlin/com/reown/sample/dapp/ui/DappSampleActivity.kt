@file:OptIn(ExperimentalMaterialApi::class)

package com.reown.sample.dapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.reown.appkit.client.AppKit
import com.reown.sample.common.ui.theme.WCSampleAppTheme
import com.reown.sample.dapp.ui.routes.host.DappSampleHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DappSampleActivity : ComponentActivity() {
    @ExperimentalMaterialNavigationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WCSampleAppTheme {
                DappSampleHost()
            }
        }

        if (intent?.dataString?.contains("wc_ev") == true) {
            AppKit.handleDeepLink(intent.dataString ?: "") {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@DappSampleActivity, "Error dispatching envelope: ${it.throwable.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.dataString?.contains("wc_ev") == true) {
            AppKit.handleDeepLink(intent.dataString ?: "") {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@DappSampleActivity, "Error dispatching envelope: ${it.throwable.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
