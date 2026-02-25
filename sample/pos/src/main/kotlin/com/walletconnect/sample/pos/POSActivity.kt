package com.walletconnect.sample.pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.walletconnect.sample.pos.nfc.NfcManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class POSActivity : AppCompatActivity() {
    private val viewModel: POSViewModel = POSViewModel()
    private var hceToggleJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { POSSampleHost(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        // Re-apply NFC mode whenever the HCE toggle changes.
        // StateFlow replays the current value immediately, so no separate enable() call needed.
        hceToggleJob = lifecycleScope.launch {
            viewModel.hceOnlyMode.collect {
                NfcManager.enable(this@POSActivity)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        hceToggleJob?.cancel()
        NfcManager.disable(this)
    }
}
