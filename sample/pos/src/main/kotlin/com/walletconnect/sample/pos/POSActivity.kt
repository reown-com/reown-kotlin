package com.walletconnect.sample.pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.walletconnect.sample.pos.nfc.NfcManager
import com.walletconnect.sample.pos.nfc.UsdkServiceHelper

class POSActivity : AppCompatActivity() {
    private val viewModel: POSViewModel = POSViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UsdkServiceHelper.bind(this)
        enableEdgeToEdge()
        setContent { POSSampleHost(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        NfcManager.enable()
    }

    override fun onPause() {
        super.onPause()
        NfcManager.disable()
    }

    override fun onDestroy() {
        super.onDestroy()
        UsdkServiceHelper.unbind(this)
    }
}
