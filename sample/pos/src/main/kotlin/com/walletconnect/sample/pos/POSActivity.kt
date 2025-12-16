package com.walletconnect.sample.pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class POSActivity : AppCompatActivity() {
    private val viewModel: POSViewModel = POSViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { POSSampleHost(viewModel) }
    }
}