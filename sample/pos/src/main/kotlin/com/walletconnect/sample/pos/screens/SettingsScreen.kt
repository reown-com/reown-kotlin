package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.sample.pos.BuildConfig
import com.walletconnect.sample.pos.POSViewModel
import com.walletconnect.sample.pos.components.CloseButton
import com.walletconnect.sample.pos.components.PosHeader
import com.walletconnect.sample.pos.model.Currency
import com.walletconnect.sample.pos.model.ThemeMode
import kotlinx.coroutines.launch

private enum class ActiveSheet { THEME, CURRENCY }

private val CaretUpDown: ImageVector by lazy {
    ImageVector.Builder(
        name = "CaretUpDown",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // Down caret
            moveTo(14.4137f, 13.0875f)
            curveTo(14.5011f, 13.1746f, 14.5705f, 13.278f, 14.6178f, 13.392f)
            curveTo(14.6651f, 13.506f, 14.6895f, 13.6281f, 14.6895f, 13.7515f)
            curveTo(14.6895f, 13.8749f, 14.6651f, 13.9971f, 14.6178f, 14.111f)
            curveTo(14.5705f, 14.225f, 14.5011f, 14.3285f, 14.4137f, 14.4156f)
            lineTo(10.6637f, 18.1656f)
            curveTo(10.5766f, 18.253f, 10.4731f, 18.3223f, 10.3592f, 18.3696f)
            curveTo(10.2452f, 18.417f, 10.1231f, 18.4413f, 9.9997f, 18.4413f)
            curveTo(9.8763f, 18.4413f, 9.7541f, 18.417f, 9.6402f, 18.3696f)
            curveTo(9.5262f, 18.3223f, 9.4227f, 18.253f, 9.3356f, 18.1656f)
            lineTo(5.5856f, 14.4156f)
            curveTo(5.4095f, 14.2395f, 5.3106f, 14.0006f, 5.3106f, 13.7515f)
            curveTo(5.3106f, 13.5024f, 5.4095f, 13.2636f, 5.5856f, 13.0875f)
            curveTo(5.7617f, 12.9113f, 6.0006f, 12.8124f, 6.2497f, 12.8124f)
            curveTo(6.4987f, 12.8124f, 6.7376f, 12.9113f, 6.9137f, 13.0875f)
            lineTo(10.0005f, 16.1726f)
            lineTo(13.0872f, 13.0851f)
            curveTo(13.1744f, 12.9981f, 13.2779f, 12.9291f, 13.3918f, 12.8822f)
            curveTo(13.5057f, 12.8352f, 13.6277f, 12.8111f, 13.7509f, 12.8113f)
            curveTo(13.8741f, 12.8115f, 13.9961f, 12.836f, 14.1098f, 12.8834f)
            curveTo(14.2235f, 12.9308f, 14.3268f, 13.0001f, 14.4137f, 13.0875f)
            close()
            // Up caret
            moveTo(6.9137f, 6.9156f)
            lineTo(10.0005f, 3.8289f)
            lineTo(13.0872f, 6.9164f)
            curveTo(13.2633f, 7.0925f, 13.5022f, 7.1914f, 13.7512f, 7.1914f)
            curveTo(14.0003f, 7.1914f, 14.2392f, 7.0925f, 14.4153f, 6.9164f)
            curveTo(14.5914f, 6.7402f, 14.6904f, 6.5014f, 14.6904f, 6.2523f)
            curveTo(14.6904f, 6.0032f, 14.5914f, 5.7644f, 14.4153f, 5.5882f)
            lineTo(10.6653f, 1.8382f)
            curveTo(10.5782f, 1.7508f, 10.4747f, 1.6815f, 10.3608f, 1.6342f)
            curveTo(10.2468f, 1.5869f, 10.1246f, 1.5625f, 10.0012f, 1.5625f)
            curveTo(9.8779f, 1.5625f, 9.7557f, 1.5869f, 9.6417f, 1.6342f)
            curveTo(9.5278f, 1.6815f, 9.4243f, 1.7508f, 9.3372f, 1.8382f)
            lineTo(5.5872f, 5.5882f)
            curveTo(5.4111f, 5.7644f, 5.3121f, 6.0032f, 5.3121f, 6.2523f)
            curveTo(5.3121f, 6.5014f, 5.4111f, 6.7402f, 5.5872f, 6.9164f)
            curveTo(5.7633f, 7.0925f, 6.0022f, 7.1914f, 6.2512f, 7.1914f)
            curveTo(6.5003f, 7.1914f, 6.7392f, 7.0925f, 6.9153f, 6.9164f)
            lineTo(6.9137f, 6.9156f)
            close()
        }
    }.build()
}

private val DeviceMobileSpeaker: ImageVector by lazy {
    ImageVector.Builder(
        name = "DeviceMobileSpeaker",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(13.75f, 0.9375f)
            horizontalLineTo(6.25f)
            curveTo(5.66984f, 0.9375f, 5.11344f, 1.16797f, 4.7032f, 1.5782f)
            curveTo(4.29297f, 1.98844f, 4.0625f, 2.54484f, 4.0625f, 3.125f)
            verticalLineTo(16.875f)
            curveTo(4.0625f, 17.4552f, 4.29297f, 18.0116f, 4.7032f, 18.4218f)
            curveTo(5.11344f, 18.832f, 5.66984f, 19.0625f, 6.25f, 19.0625f)
            horizontalLineTo(13.75f)
            curveTo(14.3302f, 19.0625f, 14.8866f, 18.832f, 15.2968f, 18.4218f)
            curveTo(15.707f, 18.0116f, 15.9375f, 17.4552f, 15.9375f, 16.875f)
            verticalLineTo(3.125f)
            curveTo(15.9375f, 2.54484f, 15.707f, 1.98844f, 15.2968f, 1.5782f)
            curveTo(14.8866f, 1.16797f, 14.3302f, 0.9375f, 13.75f, 0.9375f)
            close()
            moveTo(14.0625f, 16.875f)
            curveTo(14.0625f, 16.9579f, 14.0296f, 17.0374f, 13.971f, 17.096f)
            curveTo(13.9124f, 17.1546f, 13.8329f, 17.1875f, 13.75f, 17.1875f)
            horizontalLineTo(6.25f)
            curveTo(6.16712f, 17.1875f, 6.08763f, 17.1546f, 6.02903f, 17.096f)
            curveTo(5.97042f, 17.0374f, 5.9375f, 16.9579f, 5.9375f, 16.875f)
            verticalLineTo(3.125f)
            curveTo(5.9375f, 3.04212f, 5.97042f, 2.96263f, 6.02903f, 2.90403f)
            curveTo(6.08763f, 2.84542f, 6.16712f, 2.8125f, 6.25f, 2.8125f)
            horizontalLineTo(13.75f)
            curveTo(13.8329f, 2.8125f, 13.9124f, 2.84542f, 13.971f, 2.90403f)
            curveTo(14.0296f, 2.96263f, 14.0625f, 3.04212f, 14.0625f, 3.125f)
            verticalLineTo(16.875f)
            close()
            moveTo(12.8125f, 5f)
            curveTo(12.8125f, 5.24864f, 12.7137f, 5.4871f, 12.5379f, 5.66291f)
            curveTo(12.3621f, 5.83873f, 12.1236f, 5.9375f, 11.875f, 5.9375f)
            horizontalLineTo(8.125f)
            curveTo(7.87636f, 5.9375f, 7.6379f, 5.83873f, 7.46209f, 5.66291f)
            curveTo(7.28627f, 5.4871f, 7.1875f, 5.24864f, 7.1875f, 5f)
            curveTo(7.1875f, 4.75136f, 7.28627f, 4.5129f, 7.46209f, 4.33709f)
            curveTo(7.6379f, 4.16127f, 7.87636f, 4.0625f, 8.125f, 4.0625f)
            horizontalLineTo(11.875f)
            curveTo(12.1236f, 4.0625f, 12.3621f, 4.16127f, 12.5379f, 4.33709f)
            curveTo(12.7137f, 4.5129f, 12.8125f, 4.75136f, 12.8125f, 5f)
            close()
        }
    }.build()
}

private val SunIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Sun",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(9.0625f, 2.8125f)
            verticalLineTo(1.5625f)
            curveTo(9.0625f, 1.31386f, 9.16127f, 1.0754f, 9.33709f, 0.899587f)
            curveTo(9.5129f, 0.723772f, 9.75136f, 0.625f, 10f, 0.625f)
            curveTo(10.2486f, 0.625f, 10.4871f, 0.723772f, 10.6629f, 0.899587f)
            curveTo(10.8387f, 1.0754f, 10.9375f, 1.31386f, 10.9375f, 1.5625f)
            verticalLineTo(2.8125f)
            curveTo(10.9375f, 3.06114f, 10.8387f, 3.2996f, 10.6629f, 3.47541f)
            curveTo(10.4871f, 3.65123f, 10.2486f, 3.75f, 10f, 3.75f)
            curveTo(9.75136f, 3.75f, 9.5129f, 3.65123f, 9.33709f, 3.47541f)
            curveTo(9.16127f, 3.2996f, 9.0625f, 3.06114f, 9.0625f, 2.8125f)
            close()
            moveTo(15.3125f, 10f)
            curveTo(15.3125f, 11.0507f, 15.0009f, 12.0778f, 14.4172f, 12.9515f)
            curveTo(13.8334f, 13.8251f, 13.0037f, 14.506f, 12.033f, 14.9081f)
            curveTo(11.0623f, 15.3102f, 9.99411f, 15.4154f, 8.96358f, 15.2104f)
            curveTo(7.93306f, 15.0054f, 6.98646f, 14.4995f, 6.2435f, 13.7565f)
            curveTo(5.50053f, 13.0135f, 4.99456f, 12.0669f, 4.78958f, 11.0364f)
            curveTo(4.58459f, 10.0059f, 4.6898f, 8.93773f, 5.09189f, 7.96699f)
            curveTo(5.49398f, 6.99626f, 6.1749f, 6.16656f, 7.04853f, 5.58282f)
            curveTo(7.92217f, 4.99907f, 8.94929f, 4.6875f, 10f, 4.6875f)
            curveTo(11.4085f, 4.68895f, 12.7589f, 5.24912f, 13.7549f, 6.24509f)
            curveTo(14.7509f, 7.24107f, 15.3111f, 8.59148f, 15.3125f, 10f)
            close()
            moveTo(13.4375f, 10f)
            curveTo(13.4375f, 9.32013f, 13.2359f, 8.65552f, 12.8582f, 8.09023f)
            curveTo(12.4805f, 7.52493f, 11.9436f, 7.08434f, 11.3155f, 6.82416f)
            curveTo(10.6874f, 6.56399f, 9.99619f, 6.49591f, 9.32938f, 6.62855f)
            curveTo(8.66257f, 6.76119f, 8.05006f, 7.08858f, 7.56932f, 7.56932f)
            curveTo(7.08858f, 8.05006f, 6.76119f, 8.66257f, 6.62855f, 9.32938f)
            curveTo(6.49591f, 9.99619f, 6.56399f, 10.6874f, 6.82416f, 11.3155f)
            curveTo(7.08434f, 11.9436f, 7.52493f, 12.4805f, 8.09023f, 12.8582f)
            curveTo(8.65552f, 13.2359f, 9.32013f, 13.4375f, 10f, 13.4375f)
            curveTo(10.9114f, 13.4365f, 11.7851f, 13.074f, 12.4295f, 12.4295f)
            curveTo(13.074f, 11.7851f, 13.4365f, 10.9114f, 13.4375f, 10f)
            close()
            moveTo(4.02422f, 5.35078f)
            curveTo(4.11142f, 5.43799f, 4.21495f, 5.50716f, 4.32889f, 5.55436f)
            curveTo(4.44283f, 5.60155f, 4.56495f, 5.62585f, 4.68828f, 5.62585f)
            curveTo(4.81161f, 5.62585f, 4.93373f, 5.60155f, 5.04767f, 5.55436f)
            curveTo(5.16161f, 5.50716f, 5.26514f, 5.43799f, 5.35234f, 5.35078f)
            curveTo(5.43955f, 5.26358f, 5.50873f, 5.16005f, 5.55592f, 5.04611f)
            curveTo(5.60312f, 4.93217f, 5.62741f, 4.81005f, 5.62741f, 4.68672f)
            curveTo(5.62741f, 4.56339f, 5.60312f, 4.44127f, 5.55592f, 4.32733f)
            curveTo(5.50873f, 4.21339f, 5.43955f, 4.10986f, 5.35234f, 4.02266f)
            lineTo(4.41484f, 3.08516f)
            curveTo(4.23872f, 2.90904f, 3.99985f, 2.81009f, 3.75078f, 2.81009f)
            curveTo(3.50171f, 2.81009f, 3.26284f, 2.90904f, 3.08672f, 3.08516f)
            curveTo(2.9106f, 3.26128f, 2.81166f, 3.50015f, 2.81166f, 3.74922f)
            curveTo(2.81166f, 3.99829f, 2.9106f, 4.23716f, 3.08672f, 4.41328f)
            lineTo(4.02422f, 5.35078f)
            close()
            moveTo(4.02422f, 14.6477f)
            lineTo(3.08672f, 15.5852f)
            curveTo(2.99951f, 15.6724f, 2.93034f, 15.7759f, 2.88314f, 15.8898f)
            curveTo(2.83595f, 16.0038f, 2.81166f, 16.1259f, 2.81166f, 16.2492f)
            curveTo(2.81166f, 16.3725f, 2.83595f, 16.4947f, 2.88314f, 16.6086f)
            curveTo(2.93034f, 16.7225f, 2.99951f, 16.8261f, 3.08672f, 16.9133f)
            curveTo(3.26284f, 17.0894f, 3.50171f, 17.1883f, 3.75078f, 17.1883f)
            curveTo(3.87411f, 17.1883f, 3.99623f, 17.1641f, 4.11017f, 17.1169f)
            curveTo(4.22411f, 17.0697f, 4.32764f, 17.0005f, 4.41484f, 16.9133f)
            lineTo(5.35234f, 15.9758f)
            curveTo(5.52846f, 15.7997f, 5.62741f, 15.5608f, 5.62741f, 15.3117f)
            curveTo(5.62741f, 15.0626f, 5.52846f, 14.8238f, 5.35234f, 14.6477f)
            curveTo(5.17622f, 14.4715f, 4.93735f, 14.3726f, 4.68828f, 14.3726f)
            curveTo(4.43921f, 14.3726f, 4.20034f, 14.4715f, 4.02422f, 14.6477f)
            close()
            moveTo(15.3125f, 5.625f)
            curveTo(15.4357f, 5.6251f, 15.5576f, 5.60093f, 15.6714f, 5.55388f)
            curveTo(15.7852f, 5.50683f, 15.8887f, 5.43781f, 15.9758f, 5.35078f)
            lineTo(16.9133f, 4.41328f)
            curveTo(17.0005f, 4.32608f, 17.0697f, 4.22255f, 17.1169f, 4.10861f)
            curveTo(17.1641f, 3.99467f, 17.1883f, 3.87255f, 17.1883f, 3.74922f)
            curveTo(17.1883f, 3.62589f, 17.1641f, 3.50377f, 17.1169f, 3.38983f)
            curveTo(17.0697f, 3.27589f, 17.0005f, 3.17236f, 16.9133f, 3.08516f)
            curveTo(16.8261f, 2.99795f, 16.7225f, 2.92877f, 16.6086f, 2.88158f)
            curveTo(16.4947f, 2.83438f, 16.3725f, 2.81009f, 16.2492f, 2.81009f)
            curveTo(16.1259f, 2.81009f, 16.0038f, 2.83438f, 15.8898f, 2.88158f)
            curveTo(15.7759f, 2.92877f, 15.6724f, 2.99795f, 15.5852f, 3.08516f)
            lineTo(14.6477f, 4.02266f)
            curveTo(14.5158f, 4.15378f, 14.4258f, 4.32117f, 14.3894f, 4.50354f)
            curveTo(14.3529f, 4.68591f, 14.3714f, 4.87501f, 14.4427f, 5.0468f)
            curveTo(14.514f, 5.21858f, 14.6347f, 5.36528f, 14.7896f, 5.46824f)
            curveTo(14.9445f, 5.5712f, 15.1265f, 5.62577f, 15.3125f, 5.625f)
            close()
            moveTo(15.9758f, 14.6492f)
            curveTo(15.7997f, 14.4731f, 15.5608f, 14.3742f, 15.3117f, 14.3742f)
            curveTo(15.0626f, 14.3742f, 14.8238f, 14.4731f, 14.6477f, 14.6492f)
            curveTo(14.4715f, 14.8253f, 14.3726f, 15.0642f, 14.3726f, 15.3133f)
            curveTo(14.3726f, 15.5624f, 14.4715f, 15.8012f, 14.6477f, 15.9773f)
            lineTo(15.5852f, 16.9148f)
            curveTo(15.7613f, 17.091f, 16.0001f, 17.1899f, 16.2492f, 17.1899f)
            curveTo(16.4983f, 17.1899f, 16.7372f, 17.091f, 16.9133f, 16.9148f)
            curveTo(17.0894f, 16.7387f, 17.1883f, 16.4999f, 17.1883f, 16.2508f)
            curveTo(17.1883f, 16.0017f, 17.0894f, 15.7628f, 16.9133f, 15.5867f)
            lineTo(15.9758f, 14.6492f)
            close()
            moveTo(3.75f, 10f)
            curveTo(3.75f, 9.75136f, 3.65123f, 9.5129f, 3.47541f, 9.33709f)
            curveTo(3.2996f, 9.16127f, 3.06114f, 9.0625f, 2.8125f, 9.0625f)
            horizontalLineTo(1.5625f)
            curveTo(1.31386f, 9.0625f, 1.0754f, 9.16127f, 0.899587f, 9.33709f)
            curveTo(0.723772f, 9.5129f, 0.625f, 9.75136f, 0.625f, 10f)
            curveTo(0.625f, 10.2486f, 0.723772f, 10.4871f, 0.899587f, 10.6629f)
            curveTo(1.0754f, 10.8387f, 1.31386f, 10.9375f, 1.5625f, 10.9375f)
            horizontalLineTo(2.8125f)
            curveTo(3.06114f, 10.9375f, 3.2996f, 10.8387f, 3.47541f, 10.6629f)
            curveTo(3.65123f, 10.4871f, 3.75f, 10.2486f, 3.75f, 10f)
            close()
            moveTo(10f, 16.25f)
            curveTo(9.75136f, 16.25f, 9.5129f, 16.3488f, 9.33709f, 16.5246f)
            curveTo(9.16127f, 16.7004f, 9.0625f, 16.9389f, 9.0625f, 17.1875f)
            verticalLineTo(18.4375f)
            curveTo(9.0625f, 18.6861f, 9.16127f, 18.9246f, 9.33709f, 19.1004f)
            curveTo(9.5129f, 19.2762f, 9.75136f, 19.375f, 10f, 19.375f)
            curveTo(10.2486f, 19.375f, 10.4871f, 19.2762f, 10.6629f, 19.1004f)
            curveTo(10.8387f, 18.9246f, 10.9375f, 18.6861f, 10.9375f, 18.4375f)
            verticalLineTo(17.1875f)
            curveTo(10.9375f, 16.9389f, 10.8387f, 16.7004f, 10.6629f, 16.5246f)
            curveTo(10.4871f, 16.3488f, 10.2486f, 16.25f, 10f, 16.25f)
            close()
            moveTo(18.4375f, 9.0625f)
            horizontalLineTo(17.1875f)
            curveTo(16.9389f, 9.0625f, 16.7004f, 9.16127f, 16.5246f, 9.33709f)
            curveTo(16.3488f, 9.5129f, 16.25f, 9.75136f, 16.25f, 10f)
            curveTo(16.25f, 10.2486f, 16.3488f, 10.4871f, 16.5246f, 10.6629f)
            curveTo(16.7004f, 10.8387f, 16.9389f, 10.9375f, 17.1875f, 10.9375f)
            horizontalLineTo(18.4375f)
            curveTo(18.6861f, 10.9375f, 18.9246f, 10.8387f, 19.1004f, 10.6629f)
            curveTo(19.2762f, 10.4871f, 19.375f, 10.2486f, 19.375f, 10f)
            curveTo(19.375f, 9.75136f, 19.2762f, 9.5129f, 19.1004f, 9.33709f)
            curveTo(18.9246f, 9.16127f, 18.6861f, 9.0625f, 18.4375f, 9.0625f)
            close()
        }
    }.build()
}

private val MoonIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Moon",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(18.467f, 10.89f)
            curveTo(18.3468f, 10.7694f, 18.1959f, 10.684f, 18.0308f, 10.6427f)
            curveTo(17.8656f, 10.6014f, 17.6923f, 10.6058f, 17.5295f, 10.6556f)
            curveTo(16.3921f, 10.9989f, 15.1829f, 11.0274f, 14.0306f, 10.7378f)
            curveTo(12.8783f, 10.4482f, 11.8261f, 9.85153f, 10.9862f, 9.01125f)
            curveTo(10.1462f, 8.17097f, 9.54992f, 7.11859f, 9.2608f, 5.9662f)
            curveTo(8.97167f, 4.81381f, 9.00055f, 3.60457f, 9.34435f, 2.46729f)
            curveTo(9.39451f, 2.30441f, 9.39931f, 2.13093f, 9.35824f, 1.96551f)
            curveTo(9.31718f, 1.8001f, 9.23179f, 1.64901f, 9.11128f, 1.52849f)
            curveTo(8.99076f, 1.40798f, 8.83967f, 1.32259f, 8.67426f, 1.28153f)
            curveTo(8.50884f, 1.24046f, 8.33537f, 1.24526f, 8.17248f, 1.29542f)
            curveTo(6.46185f, 1.8212f, 4.96067f, 2.8732f, 3.88263f, 4.30167f)
            curveTo(2.9399f, 5.55471f, 2.365f, 7.04567f, 2.22244f, 8.60725f)
            curveTo(2.07989f, 10.1688f, 2.37531f, 11.7392f, 3.07557f, 13.1423f)
            curveTo(3.77583f, 14.5453f, 4.85321f, 15.7254f, 6.18683f, 16.5503f)
            curveTo(7.52045f, 17.3751f, 9.05753f, 17.8119f, 10.6256f, 17.8118f)
            curveTo(12.4563f, 17.8172f, 14.2383f, 17.2219f, 15.6983f, 16.1173f)
            curveTo(17.126f, 15.0382f, 18.1769f, 13.5362f, 18.7014f, 11.8251f)
            curveTo(18.7507f, 11.6626f, 18.7549f, 11.4898f, 18.7136f, 11.3251f)
            curveTo(18.6723f, 11.1604f, 18.5871f, 11.01f, 18.467f, 10.89f)
            close()
            moveTo(14.5701f, 14.6196f)
            curveTo(13.3065f, 15.5709f, 11.7417f, 16.0338f, 10.1639f, 15.9231f)
            curveTo(8.58603f, 15.8124f, 7.10126f, 15.1356f, 5.98282f, 14.0171f)
            curveTo(4.86437f, 12.8987f, 4.18751f, 11.4139f, 4.0768f, 9.83605f)
            curveTo(3.96609f, 8.25821f, 4.42898f, 6.69347f, 5.38029f, 5.4298f)
            curveTo(5.88086f, 4.76766f, 6.50123f, 4.20532f, 7.2092f, 3.77198f)
            curveTo(7.19513f, 3.97198f, 7.1881f, 4.17276f, 7.1881f, 4.37433f)
            curveTo(7.19058f, 6.61133f, 8.08033f, 8.756f, 9.66213f, 10.3378f)
            curveTo(11.2439f, 11.9196f, 13.3886f, 12.8093f, 15.6256f, 12.8118f)
            curveTo(15.8272f, 12.8118f, 16.0279f, 12.8048f, 16.2279f, 12.7907f)
            curveTo(15.7948f, 13.4989f, 15.2324f, 14.1193f, 14.5701f, 14.6196f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsScreen(
    viewModel: POSViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val selectedThemeMode by viewModel.selectedThemeMode.collectAsState()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    var activeSheet by remember { mutableStateOf(ActiveSheet.CURRENCY) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = WCTheme.spacing.spacing8, topEnd = WCTheme.spacing.spacing8),
        sheetBackgroundColor = WCTheme.colors.bgPrimary,
        sheetElevation = 4.dp,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        sheetContent = {
            when (activeSheet) {
                ActiveSheet.THEME -> ThemeBottomSheet(
                    selectedThemeMode = selectedThemeMode,
                    onSelect = { mode ->
                        viewModel.setThemeMode(mode)
                        scope.launch { sheetState.hide() }
                    },
                    onDismiss = { scope.launch { sheetState.hide() } }
                )
                ActiveSheet.CURRENCY -> CurrencyBottomSheet(
                    selectedCurrency = selectedCurrency,
                    onSelect = { currency ->
                        viewModel.setCurrency(currency)
                        scope.launch { sheetState.hide() }
                    },
                    onDismiss = { scope.launch { sheetState.hide() } }
                )
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(WCTheme.colors.bgPrimary)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PosHeader(onBack = onClose)

            Spacer(Modifier.height(WCTheme.spacing.spacing3))

            // Theme setting
            SettingsItem(
                label = "Theme",
                value = selectedThemeMode.displayName,
                showCaret = true,
                onClick = {
                    activeSheet = ActiveSheet.THEME
                    scope.launch { sheetState.show() }
                },
                modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing5)
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing2))

            // Currency setting
            SettingsItem(
                label = "Currency",
                value = "${selectedCurrency.displayName} (${selectedCurrency.symbol})",
                showCaret = true,
                onClick = {
                    activeSheet = ActiveSheet.CURRENCY
                    scope.launch { sheetState.show() }
                },
                modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing5)
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing2))

            // SDK Version
            SettingsItem(
                label = "SDK Version",
                value = BuildConfig.BOM_VERSION,
                modifier = Modifier.padding(horizontal = WCTheme.spacing.spacing5)
            )

            Spacer(Modifier.weight(1f))

            CloseButton(onClick = onClose)

            Spacer(Modifier.height(WCTheme.spacing.spacing5))
        }
    }
}

@Composable
private fun ThemeBottomSheet(
    selectedThemeMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing5)
    ) {
        // Header: title centered, X button on right
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Theme",
                style = WCTheme.typography.h6Regular,
                color = WCTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(WCTheme.spacing.spacing3))
                    .border(
                        width = 1.dp,
                        color = WCTheme.colors.borderSecondary,
                        shape = RoundedCornerShape(WCTheme.spacing.spacing3)
                    )
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = WCTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing7))

        // Theme options
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selectedThemeMode
            val icon = when (mode) {
                ThemeMode.SYSTEM -> DeviceMobileSpeaker
                ThemeMode.LIGHT -> SunIcon
                ThemeMode.DARK -> MoonIcon
            }
            ThemeOptionItem(
                icon = icon,
                label = mode.displayName,
                isSelected = isSelected,
                onClick = { onSelect(mode) }
            )
            if (mode != ThemeMode.entries.last()) {
                Spacer(Modifier.height(WCTheme.spacing.spacing2))
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun ThemeOptionItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(WCTheme.borderRadius.radius4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .border(1.dp, WCTheme.colors.borderAccentPrimary, shape)
                        .background(WCTheme.colors.foregroundAccentPrimary10, shape)
                } else {
                    Modifier.background(WCTheme.colors.foregroundPrimary, shape)
                }
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) WCTheme.colors.iconAccentPrimary else WCTheme.colors.iconDefault,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(WCTheme.spacing.spacing2))
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(1.dp, WCTheme.colors.iconAccentPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(WCTheme.colors.iconAccentPrimary, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun CurrencyBottomSheet(
    selectedCurrency: Currency,
    onSelect: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WCTheme.spacing.spacing5)
    ) {
        // Header: title centered, X button on right
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Currency",
                style = WCTheme.typography.h6Regular,
                color = WCTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(WCTheme.spacing.spacing3))
                    .border(
                        width = 1.dp,
                        color = WCTheme.colors.borderSecondary,
                        shape = RoundedCornerShape(WCTheme.spacing.spacing3)
                    )
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = WCTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing7))

        // Currency options
        Currency.entries.forEach { currency ->
            val isSelected = currency == selectedCurrency
            CurrencyOptionItem(
                label = "${currency.displayName} (${currency.symbol})",
                isSelected = isSelected,
                onClick = { onSelect(currency) }
            )
            if (currency != Currency.entries.last()) {
                Spacer(Modifier.height(WCTheme.spacing.spacing2))
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun CurrencyOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(WCTheme.borderRadius.radius4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .border(1.dp, WCTheme.colors.borderAccentPrimary, shape)
                        .background(WCTheme.colors.foregroundAccentPrimary10, shape)
                } else {
                    Modifier.background(WCTheme.colors.foregroundPrimary, shape)
                }
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            // Radio button: blue circle border with filled inner circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(1.dp, WCTheme.colors.iconAccentPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(WCTheme.colors.iconAccentPrimary, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    label: String,
    value: String,
    showCaret: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(WCTheme.borderRadius.shapeMedium)
            .background(WCTheme.colors.foregroundPrimary)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = WCTheme.spacing.spacing5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textPrimary
        )
        Spacer(Modifier.width(WCTheme.spacing.spacing2))
        Text(
            text = value,
            style = WCTheme.typography.bodyLgRegular,
            color = WCTheme.colors.textTertiary,
            modifier = Modifier.weight(1f)
        )
        if (showCaret) {
            Icon(
                imageVector = CaretUpDown,
                contentDescription = null,
                tint = WCTheme.colors.iconInvert,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
