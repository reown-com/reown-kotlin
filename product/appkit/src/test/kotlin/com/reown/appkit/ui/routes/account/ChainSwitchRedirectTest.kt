package com.reown.appkit.ui.routes.account

import com.android.resources.NightMode
import com.reown.appkit.presets.AppKitChainsPresets
import com.reown.appkit.ui.navigation.Route
import com.reown.appkit.ui.routes.account.chain_redirect.ChainRedirectState
import com.reown.appkit.ui.routes.account.chain_redirect.ChainSwitchRedirectScreen
import com.reown.appkit.utils.ScreenShotTest
import org.junit.Ignore
import org.junit.Test

@Ignore("This test is not working on CI for Sonar only")
internal class ChainSwitchRedirectTest : ScreenShotTest("account/" + Route.CHAIN_SWITCH_REDIRECT.path) {

    private val chain = AppKitChainsPresets.ethChains["1"]!!

    @Test
    fun `test ChainSwitchRedirect with Loading in LightMode`() = runRouteScreenShotTest(
        title = chain.chainName
    ) {
        ChainSwitchRedirectScreen(chain, ChainRedirectState.Loading, {})
    }

    @Test
    fun `test ChainSwitchRedirect with Loading in DarkMode`() = runRouteScreenShotTest(
        title = chain.chainName,
        nightMode = NightMode.NIGHT
    ) {
        ChainSwitchRedirectScreen(chain, ChainRedirectState.Loading, {})
    }

    @Test
    fun `test ChainSwitchRedirect with Decline in LightMode`() = runRouteScreenShotTest(
        title = chain.chainName
    ) {
        ChainSwitchRedirectScreen(chain, ChainRedirectState.Declined, {})
    }

    @Test
    fun `test ChainSwitchRedirect with Decline in DarkMode`() = runRouteScreenShotTest(
        title = chain.chainName,
        nightMode = NightMode.NIGHT
    ) {
        ChainSwitchRedirectScreen(chain, ChainRedirectState.Declined, {})
    }
}