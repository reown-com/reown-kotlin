package com.reown.appkit.ui.routes.connect.all_wallets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reown.android.internal.common.modal.data.model.Wallet
import com.reown.modal.utils.isLandscape
import com.reown.util.Empty
import com.reown.appkit.R
import com.reown.appkit.ui.components.internal.commons.ContentDescription
import com.reown.appkit.ui.components.internal.commons.HorizontalSpacer
import com.reown.appkit.ui.components.internal.commons.LoadingSpinner
import com.reown.appkit.ui.components.internal.commons.ScanQRIcon
import com.reown.appkit.ui.components.internal.commons.TransparentSurface
import com.reown.appkit.ui.components.internal.commons.VerticalSpacer
import com.reown.appkit.ui.components.internal.commons.inputs.SearchInput
import com.reown.appkit.ui.components.internal.commons.inputs.SearchState
import com.reown.appkit.ui.components.internal.commons.inputs.SearchStatePreviewProvider
import com.reown.appkit.ui.components.internal.commons.walletsGridItems
import com.reown.modal.ui.model.LoadingState
import com.reown.appkit.ui.previews.ComponentPreview
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.previews.AppKitPreview
import com.reown.appkit.ui.previews.testWallets
import com.reown.appkit.ui.routes.connect.ConnectViewModel
import com.reown.appkit.ui.routes.connect.WalletsData
import com.reown.appkit.ui.theme.AppKitTheme
import com.reown.appkit.ui.utils.conditionalModifier
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
internal fun AllWalletsRoute(
    connectViewModel: ConnectViewModel
) {
    val walletsState by connectViewModel.walletsState.collectAsState()

    AllWalletsContent(
        walletsData = walletsState,
        searchPhrase = connectViewModel.searchPhrase,
        onSearch = { connectViewModel.search(it) },
        onSearchClear = { connectViewModel.clearSearch() },
        onFetchNextPage = { connectViewModel.fetchMoreWallets() },
        onWalletItemClick = { wallet -> connectViewModel.navigateToRedirectRoute(wallet) },
        onScanQRClick = { connectViewModel.navigateToScanQRCode() }
    )
}

@Composable
private fun AllWalletsContent(
    walletsData: WalletsData,
    searchPhrase: String,
    onSearch: (String) -> Unit,
    onSearchClear: () -> Unit,
    onFetchNextPage: () -> Unit,
    onWalletItemClick: (Wallet) -> Unit,
    onScanQRClick: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val scrollToFirstItem = { coroutineScope.launch { gridState.scrollToItem(0) } }
    val searchState = remember {
        SearchState(
            searchPhrase = searchPhrase,
            onSearchSubmit = { onSearch(it).also { scrollToFirstItem() } },
            onClearInput = { onSearchClear().also { scrollToFirstItem() } }
        )
    }
    val gridFraction = if (isLandscape) 1f else .95f

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex != 0 && !gridState.canScrollForward }
            .distinctUntilChanged()
            .filter { it }
            .collect { onFetchNextPage() }
    }

    Column(modifier = Modifier.fillMaxHeight(gridFraction)) {
        SearchInputRow(searchState, onScanQRClick)
        if (walletsData.loadingState == LoadingState.REFRESH) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingSpinner()
            }
        } else if (walletsData.wallets.isEmpty()) {
            NoWalletsFoundItem()
        } else {
            WalletsGrid(gridState, walletsData, onWalletItemClick)
        }
    }
}

@Composable
private fun WalletsGrid(
    gridState: LazyGridState,
    walletsData: WalletsData,
    onWalletItemClick: (Wallet) -> Unit
) {
    val color = AppKitTheme.colors.background.color275
    Box {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.FixedSize(82.dp),
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .graphicsLayer { alpha = 0.99f }
                .drawWithContent {
                    val colors = listOf(Color.Transparent, color)
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(colors, startY = 0f, endY = 40f),
                        blendMode = BlendMode.DstIn,
                    )
                },
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            walletsGridItems(walletsData.wallets, onWalletItemClick)
            if (walletsData.loadingState == LoadingState.APPEND) {
                loadingWalletsItems()
            }
        }
    }
}

private fun LazyGridScope.loadingWalletsItems() {
    items(10) {
        TransparentSurface(
            modifier = Modifier.padding(4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .height(96.dp)
                    .background(AppKitTheme.colors.grayGlass02),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.wallet_placeholder),
                    contentDescription = "Wallet loader",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(width = 1.dp, color = AppKitTheme.colors.grayGlass10, shape = RoundedCornerShape(16.dp))

                )
                VerticalSpacer(height = 8.dp)
                Text(
                    text = String.Empty,
                    style = AppKitTheme.typo.tiny500,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }

    }
}

@Composable
private fun SearchInputRow(
    searchState: SearchState,
    onScanQRClick: () -> Unit
) {
    val defaultSpacing: Dp = 12.dp
    val focusBorderWidth: Dp = 4.dp
    val focusedSpacing: Dp = defaultSpacing - focusBorderWidth
    val focusBorderColor = AppKitTheme.colors.accent20
    val state by searchState.state.collectAsState()

    val paddingValues: PaddingValues
    val spacerValue: Dp
    if (state.isFocused) {
        spacerValue = focusedSpacing
        paddingValues = PaddingValues(start = focusedSpacing, top = focusedSpacing, bottom = focusedSpacing, end = defaultSpacing)
    } else {
        spacerValue = 12.dp
        paddingValues = PaddingValues(12.dp)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(paddingValues)
    ) {
        Box(modifier = Modifier
            .weight(1f)
            .conditionalModifier(state.isFocused) {
                border(width = focusBorderWidth, color = focusBorderColor, RoundedCornerShape(16.dp)).padding(focusBorderWidth)
            }) {
            SearchInput(searchState)
        }
        HorizontalSpacer(width = spacerValue)
        ScanQRIcon(onClick = onScanQRClick)
    }
}


@Composable
private fun ColumnScope.NoWalletsFoundItem() {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_wallet),
            contentDescription = ContentDescription.WALLET.description,
            modifier = Modifier
                .size(40.dp)
                .background(AppKitTheme.colors.grayGlass05, RoundedCornerShape(12.dp))
                .padding(7.dp)
        )
        VerticalSpacer(height = 20.dp)
        Text(
            text = "No Wallet found",
            style = TextStyle(color = AppKitTheme.colors.foreground.color125, fontSize = 16.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

}

@UiModePreview
@Composable
private fun SearchRowPreview(
    @PreviewParameter(SearchStatePreviewProvider::class) state: SearchState
) {
    ComponentPreview { SearchInputRow(searchState = state, {}) }
}

@UiModePreview
@Composable
private fun AllWalletsEmptyPreview() {
    AppKitPreview {
        AllWalletsContent(WalletsData.empty(), "", {}, {}, {}, {}, {})
    }
}

@UiModePreview
@Composable
private fun AllWalletsPreview() {
    AppKitPreview {
        AllWalletsContent(WalletsData.submit(testWallets),"", {}, {}, {}, {}, {})
    }
}

@UiModePreview
@Composable
private fun AllWalletsLoadingRefreshPreview() {
    AppKitPreview {
        AllWalletsContent(WalletsData.refresh(),"", {}, {}, {}, {}, {})
    }
}

@UiModePreview
@Composable
private fun AllWalletsLoadingAppendPreview() {
    AppKitPreview {
        AllWalletsContent(WalletsData.append(testWallets),"", {}, {}, {}, {}, {})
    }
}
