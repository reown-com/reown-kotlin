package com.walletconnect.sample.modal.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.walletconnect.sample.modal.R
import com.reown.appkit.ui.AppKitView

class ViewActivity: AppCompatActivity(R.layout.activity_view) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = findViewById<AppKitView>(R.id.web3Modal)

        view.setOnCloseModal { finish() }
    }

}