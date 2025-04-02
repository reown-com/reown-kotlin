package com.reown.sample.wallet.ui.routes.composable_routes.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.reown.sample.common.ui.WCTopAppBar
import com.reown.sample.common.ui.theme.PreviewTheme
import com.reown.sample.common.ui.theme.UiModePreview
import com.reown.sample.wallet.BuildConfig
import com.reown.sample.wallet.R
import com.reown.sample.wallet.domain.SmartAccountEnabler
import com.reown.sample.wallet.ui.routes.Route

@Composable
fun SettingsRoute(navController: NavHostController) {
    val viewModel: SettingsViewModel = viewModel()
    val deviceToken = viewModel.deviceToken.collectAsState().value

    val sections = listOf(
        Section.SettingsSection(
            "EIP155 Account", listOf(
                Item.SettingCopyableItem("CAIP-10", viewModel.caip10),
//                Item.SettingCopyableItem("Safe Smart Account Address", viewModel.getSmartAccount()),
                Item.SettingCopyableItem("Private key", viewModel.privateKey),
            )
        ),
        Section.SettingsSection(
            "Solana Account", listOf(
                Item.SettingCopyableItem("Address", viewModel.solanaKeys.third),
                Item.SettingCopyableItem("Private key", viewModel.solanaKeys.first),
            )
        ),
        Section.SettingsSection(
            "Device", listOf(
                Item.SettingCopyableItem("Client ID", viewModel.clientId),
                Item.SettingCopyableItem("Device token", deviceToken),
                Item.SettingCopyableItem("App Version", BuildConfig.VERSION_NAME)
            )
        ),
    )

    val context: Context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    SettingsScreen(
        sections,
        onLogoutClicked = { navController.popBackStack() },
        onSettingClicked = {
            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
            clipboardManager.setText(AnnotatedString(it))
        },
        onTransactionClick = { navController.navigate(Route.TransactionDialog.path) }
    )
}

@Composable
private fun SettingsScreen(
    sections: List<Section>,
    onLogoutClicked: () -> Unit,
    onSettingClicked: (String) -> Unit,
    onTransactionClick: () -> Unit,
) {
    Divider()
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        WCTopAppBar(titleText = "Settings")
        Divider()
        FeaturesSection()
        Divider()
        LazyColumn {
            itemsIndexed(sections) { index, section ->
                when (section) {
                    is Section.SettingsSection -> SettingsSection(section.title, section.items, onSettingClicked)
                    is Section.LogoutSection -> LogoutSection(onLogoutClicked)
                }
                if (index != sections.lastIndex) Divider()
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .clickable { onTransactionClick() }
                .padding(vertical = 5.dp),
            text = "Send Transaction",
            style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color.Blue))
    }
}

@Composable
private fun FeaturesSection() {
    val isSafeEnabled by SmartAccountEnabler.isSmartAccountEnabled.collectAsState()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = "Features", style = TextStyle(fontSize = 15.sp), fontWeight = FontWeight(700))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Safe Smart Account", style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight(400),
                    color = MaterialTheme.colors.onBackground.copy(0.75f)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Switch(
                checked = isSafeEnabled,
                onCheckedChange = { isChecked -> SmartAccountEnabler.enableSmartAccount(isChecked) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Green)
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, items: List<Item>, onSettingClicked: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = title, style = TextStyle(fontSize = 15.sp), fontWeight = FontWeight(700))
        Spacer(modifier = Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.filterIsInstance<Item.SettingCopyableItem>().forEach {
                SettingCopyableItem(it.key, it.value, onSettingClicked)
            }
        }
    }
}

@Composable
fun SettingCopyableItem(key: String, value: String, onSettingClicked: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colors.surface, shape = shape)
            .clip(shape)
            .clickable { onSettingClicked(value) }
            .padding(horizontal = 12.dp, vertical = 16.dp)

    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = key, style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight(500),
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(painter = painterResource(id = R.drawable.ic_copy_small), contentDescription = "Copy", tint = MaterialTheme.colors.onBackground)
        }
        Text(
            text = value, style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight(400),
                color = MaterialTheme.colors.onBackground.copy(0.75f)
            )
        )
    }
}


@Composable
fun LogoutSection(onLogoutClicked: () -> Unit) {
    val color = Color(0xFFC05C5C)
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .border(width = 1.dp, color = color, shape = CircleShape)
            .clip(shape = CircleShape)
            .clickable { onLogoutClicked() }
            .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 12.dp),
        text = "Log out",
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight(600)
    )
}

@Composable
@UiModePreview
fun SettingScreenPreview() {
    val sections = listOf(
        Section.SettingsSection(
            "Account", listOf(
                Item.SettingCopyableItem("CAIP-10", "eip155:1:0xC3F909f02cF8D9a023d7eb76437E06D312E5f0Cb"),
                Item.SettingCopyableItem("Private key", "b01d06bb4a18636c12245d9e8b4078917822e461b557b3bf5e4060fee5621b01")
            )
        ),
        Section.SettingsSection(
            "Device", listOf(
                Item.SettingCopyableItem("Client ID", "did:key:z6Mko1GgpY4uTw9VL245is45kPYi9UnYKTXYJEhWhgrxsDXz"),
                Item.SettingCopyableItem("Device token", "314875502ff3a6e23f8a6e8dc3259fe4ee0b429d0ab874efa837e8677b2d215d"),
                Item.SettingCopyableItem("App Version", BuildConfig.VERSION_NAME)
            )
        ),
    )

    PreviewTheme {
        SettingsScreen(sections = sections, onLogoutClicked = { }, onSettingClicked = { }, onTransactionClick = {})
    }
}
