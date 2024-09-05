package com.reown.appkit.ui.routes.connect

import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import com.reown.modal.ui.model.LoadingState
import com.reown.util.Empty
import com.reown.appkit.ui.navigation.Route
import com.reown.appkit.ui.previews.testWallets
import com.reown.appkit.ui.routes.connect.all_wallets.AllWalletsRoute
import com.reown.appkit.utils.MainDispatcherRule
import com.reown.appkit.utils.ScreenShotTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore("This test is not working on CI for Sonar only")
internal class AllWalletsRouteTest  : ScreenShotTest("connect/${Route.ALL_WALLETS.path}") {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel: ConnectViewModel = mockk()
    private val walletsState: StateFlow<WalletsData> = mockk()

    @Before
    fun setup() {
        every { viewModel.walletsState } returns walletsState
        every { viewModel.searchPhrase } returns String.Empty
    }

    @Test
    fun `test AllWalletsRoute with LoadingRefresh in LightMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title
    ) {
        every { viewModel.walletsState.value } returns WalletsData(loadingState = LoadingState.REFRESH)
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with LoadingRefresh in DarkMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        nightMode = NightMode.NIGHT
    ) {
        every { viewModel.walletsState.value } returns WalletsData(loadingState = LoadingState.REFRESH)
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with LoadingRefresh in Landscape`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        orientation = ScreenOrientation.LANDSCAPE
    ) {
        every { viewModel.walletsState.value } returns WalletsData(loadingState = LoadingState.REFRESH)
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with EmptyList in LightMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title
    ) {
        every { viewModel.walletsState.value } returns WalletsData()
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with EmptyList in DarkMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        nightMode = NightMode.NIGHT
    ) {
        every { viewModel.walletsState.value } returns WalletsData()
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with EmptyList in Landscape`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        orientation = ScreenOrientation.LANDSCAPE
    ) {
        every { viewModel.walletsState.value } returns WalletsData()
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with Loaded Wallets in LightMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title
    ) {
        every { viewModel.walletsState.value } returns WalletsData(wallets = testWallets)
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with Loaded Wallets in DarkMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        nightMode = NightMode.NIGHT
    ) {
        every { viewModel.walletsState.value } returns WalletsData(wallets = testWallets)
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with Loaded Wallets in Landscape`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        orientation = ScreenOrientation.LANDSCAPE
    ) {
        every { viewModel.walletsState.value } returns WalletsData(wallets = testWallets)
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with Loading Append in LightMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title
    ) {
        every { viewModel.walletsState.value } returns WalletsData(wallets = testWallets, LoadingState.APPEND)
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with Loading Append in DarkMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        nightMode = NightMode.NIGHT
    ) {
        every { viewModel.walletsState.value } returns WalletsData(wallets = testWallets, LoadingState.APPEND)
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with Loading Append in Landscape`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        orientation = ScreenOrientation.LANDSCAPE
    ) {
        every { viewModel.walletsState.value } returns WalletsData(wallets = testWallets, LoadingState.APPEND)
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with Search in LightMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title
    ) {
        every { viewModel.searchPhrase } returns "Meta"
        every { viewModel.walletsState.value } returns WalletsData(wallets = testWallets.filter { it.name.contains("Meta") })
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with Search in DarkMode`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        nightMode = NightMode.NIGHT
    ) {
        every { viewModel.searchPhrase } returns "Meta"
        every { viewModel.walletsState.value } returns WalletsData(wallets = testWallets.filter { it.name.contains("Meta") })
        AllWalletsRoute(connectViewModel = viewModel)
    }

    @Test
    fun `test AllWalletsRoute with Search in Landscape`() = runRouteScreenShotTest(
        title = Route.ALL_WALLETS.title,
        orientation = ScreenOrientation.LANDSCAPE
    ) {
        every { viewModel.searchPhrase } returns "Meta"
        every { viewModel.walletsState.value } returns WalletsData(wallets = testWallets.filter { it.name.contains("Meta") })
        AllWalletsRoute(connectViewModel = viewModel)
    }
}