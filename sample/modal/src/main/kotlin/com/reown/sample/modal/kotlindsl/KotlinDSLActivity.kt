package com.reown.sample.modal.kotlindsl

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import com.reown.sample.modal.common.Route
import com.reown.sample.modal.R
import com.reown.appkit.ui.appKit

class KotlinDSLActivity : AppCompatActivity(R.layout.activity_kotlin_dsl) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.graph = navController.createGraph(
            startDestination = Route.Home.path
        ) {
            fragment<HomeFragment>(Route.Home.path)
            appKit()
        }
    }
}