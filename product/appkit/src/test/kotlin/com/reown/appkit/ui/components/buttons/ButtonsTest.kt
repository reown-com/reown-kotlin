package com.reown.appkit.ui.components.buttons

import com.android.resources.NightMode
import com.reown.appkit.client.Modal
import com.reown.appkit.presets.AppKitChainsPresets
import com.reown.appkit.ui.components.button.AccountButtonState
import com.reown.appkit.ui.components.button.ConnectButton
import com.reown.appkit.ui.components.button.ConnectButtonSize
import com.reown.appkit.utils.ScreenShotTest
import org.junit.Test

internal class ButtonsTest : ScreenShotTest("component/button") {

    @Test
    fun `test ConnectButton in LightMode`() = runComponentScreenShotTest {
        ConnectButton(size = ConnectButtonSize.NORMAL) {}
        ConnectButton(size = ConnectButtonSize.SMALL) {}
        ConnectButton(size = ConnectButtonSize.NORMAL, isEnabled = false) {}
        ConnectButton(size = ConnectButtonSize.SMALL, isEnabled = false) {}
        ConnectButton(size = ConnectButtonSize.NORMAL, isLoading = true) {}
        ConnectButton(size = ConnectButtonSize.SMALL, isLoading = true) {}
    }

    @Test
    fun `test ConnectButton in DarkMode`() = runComponentScreenShotTest(nightMode = NightMode.NIGHT) {
        ConnectButton(size = ConnectButtonSize.NORMAL) {}
        ConnectButton(size = ConnectButtonSize.SMALL) {}
        ConnectButton(size = ConnectButtonSize.NORMAL, isEnabled = false) {}
        ConnectButton(size = ConnectButtonSize.SMALL, isEnabled = false) {}
        ConnectButton(size = ConnectButtonSize.NORMAL, isLoading = true) {}
        ConnectButton(size = ConnectButtonSize.SMALL, isLoading = true) {}
    }

    @Test
    fun `test AccountButton in LightMode`() = runComponentScreenShotTest {
        val chain = AppKitChainsPresets.ethChains["1"]!!
        AccountButtonState(AccountButtonState.Invalid) {}
        AccountButtonState(AccountButtonState.Loading) {}
        AccountButtonState(AccountButtonState.Normal("0x2765d421FB91182490D602E671a")) {}
        AccountButtonState(AccountButtonState.Mixed("0x2765d421FB91182490D602E671a", Modal.Model.ChainImage.Network(""), chain.chainName, "0 ETH")) {}
    }

    @Test
    fun `test AccountButton in DarkMode`() = runComponentScreenShotTest(nightMode = NightMode.NIGHT) {
        val chain = AppKitChainsPresets.ethChains["1"]!!
        AccountButtonState(AccountButtonState.Invalid) {}
        AccountButtonState(AccountButtonState.Loading) {}
        AccountButtonState(AccountButtonState.Normal("0x2765d421FB91182490D602E671a")) {}
        AccountButtonState(AccountButtonState.Mixed("0x2765d421FB91182490D602E671a", Modal.Model.ChainImage.Network(""), chain.chainName, "0 ETH")) {}
    }
}
