package com.walletconnect.sample.pos.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import com.walletconnect.pos.Pos
import com.walletconnect.sample.pos.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.sqrt

// Dark mode foreground-primary color, always used for the button regardless of theme
private val ButtonBgColor = Color(0xFF252525)
private const val INITIAL_CIRCLE_SIZE = 20

@Composable
fun PaymentSuccessScreen(
    displayAmount: String,
    paymentInfo: Pos.PaymentInfo?,
    onNewPayment: () -> Unit,
    onPrintReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val diagonalLength = sqrt(screenWidth * screenWidth + screenHeight * screenHeight)
    val finalScale = ceil(diagonalLength / INITIAL_CIRCLE_SIZE).toFloat() + 2f

    val circleScale = remember { Animatable(1f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Launch both animations concurrently, matching RN timing
        coroutineScope {
            launch {
                circleScale.animateTo(finalScale, tween(durationMillis = 400))
            }
            launch {
                delay(150)
                contentAlpha.animateTo(1f, tween(durationMillis = 200))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WCTheme.colors.bgPrimary)
    ) {
        // Expanding blue circle from center - fixed 20dp size that scales up
        Box(
            modifier = Modifier
                .size(INITIAL_CIRCLE_SIZE.dp)
                .align(Alignment.Center)
                .scale(circleScale.value)
                .background(WCTheme.colors.bgAccentPrimary, CircleShape)
        )

        // Content fades in after circle expands
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = WCTheme.spacing.spacing5)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top logos - always white
            Spacer(Modifier.height(WCTheme.spacing.spacing4))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_wcpay_logo),
                    contentDescription = "WCPay",
                    modifier = Modifier
                        .width(60.dp)
                        .height(18.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color.White)
                )
                Spacer(Modifier.width(WCTheme.spacing.spacing2))
                Image(
                    painter = painterResource(R.drawable.ic_plus_header),
                    contentDescription = "+",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
                Spacer(Modifier.width(WCTheme.spacing.spacing2))
                Image(
                    painter = painterResource(R.drawable.ic_ingenico_logo),
                    contentDescription = "Ingenico",
                    modifier = Modifier
                        .width(78.dp)
                        .height(22.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            // Center content
            Spacer(Modifier.weight(1f))

            Text(
                text = "Payment successful",
                style = WCTheme.typography.bodyXlRegular,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(WCTheme.spacing.spacing3))

            if (displayAmount.isNotBlank()) {
                Text(
                    text = displayAmount.uppercase(),
                    style = WCTheme.typography.h3Regular,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            // Print receipt button - dark bg always
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WCTheme.spacing.spacing12)
                    .clip(WCTheme.borderRadius.shapeLarge)
                    .background(ButtonBgColor)
                    .clickable(onClick = onPrintReceipt),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Print receipt",
                        style = WCTheme.typography.bodyLgMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.width(WCTheme.spacing.spacing2))
                    Icon(
                        painter = painterResource(R.drawable.ic_receipt),
                        contentDescription = null,
                        tint = WCTheme.colors.iconDefault,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(WCTheme.spacing.spacing3))

            // New payment button - dark bg always
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WCTheme.spacing.spacing12)
                    .clip(WCTheme.borderRadius.shapeLarge)
                    .background(ButtonBgColor)
                    .clickable(onClick = onNewPayment),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "New payment",
                        style = WCTheme.typography.bodyLgMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.width(WCTheme.spacing.spacing2))
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = WCTheme.colors.iconDefault,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(WCTheme.spacing.spacing4))
        }
    }
}
