package com.walletconnect.sample.pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.reown.sample.common.ui.theme.WCSampleAppTheme
import com.walletconnect.pos.PosClient

class POSActivity : AppCompatActivity() {
    private val viewModel: POSViewModel = POSViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WCSampleAppTheme {
                POSSampleHost(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        PosClient.resume()
    }

    override fun onStop() {
        super.onStop()
        PosClient.pause()
    }
}