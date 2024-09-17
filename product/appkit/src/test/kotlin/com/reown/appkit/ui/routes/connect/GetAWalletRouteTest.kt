package com.reown.appkit.ui.routes.connect

import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import com.reown.appkit.ui.navigation.Route
import com.reown.appkit.ui.previews.testWallets
import com.reown.appkit.ui.routes.connect.get_wallet.GetAWalletRoute
import com.reown.appkit.utils.MainDispatcherRule
import com.reown.appkit.utils.ScreenShotTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore("This test is not working on CI for Sonar only")
internal class GetAWalletRouteTest: ScreenShotTest("connect/${Route.GET_A_WALLET.path}") {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `test GetAWalletRoute in LightMode`() = runRouteScreenShotTest(
        title = Route.GET_A_WALLET.title
    ) {
        GetAWalletRoute(testWallets)
    }

    @Test
    fun `test GetAWalletRoute in DarkMode`() = runRouteScreenShotTest(
        title = Route.GET_A_WALLET.title,
        nightMode = NightMode.NIGHT
    ) {
        GetAWalletRoute(testWallets)
    }

    @Test
    fun `test GetAWalletRoute in Landscape`() = runRouteScreenShotTest(
        title = Route.GET_A_WALLET.title,
        orientation = ScreenOrientation.LANDSCAPE
    ) {
        GetAWalletRoute(testWallets)
    }
}
