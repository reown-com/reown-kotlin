package com.walletconnect.sample.pos.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.theme.WCTheme
import androidx.core.content.ContextCompat
import androidx.core.graphics.minus
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.style.Neighbors
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import com.walletconnect.sample.pos.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StyledQrCode(
    data: String,
    size: Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }

    val qrBitmap by produceState<ImageBitmap?>(null, data, sizePx) {
        if (sizePx <= 0) return@produceState
        value = withContext(Dispatchers.Default) {
            val logo = ContextCompat.getDrawable(context, R.drawable.ic_wc_qr_logo)
            val drawable = QrCodeDrawable(
                data = QrData.Url(data),
                options = createQrVectorOptions {
                    padding = .0f
                    logo {
                        this.drawable = logo
                        this.size = .25f
                        this.padding = QrVectorLogoPadding.Natural(.2f)
                    }
                    colors {
                        dark = QrVectorColor.Solid(Color.Black.toArgb())
                    }
                    shapes {
                        frame = PosQrFrameShape
                        ball = QrVectorBallShape.RoundCorners(.4f)
                        darkPixel = QrVectorPixelShape.RoundCornersVertical(.95f)
                    }
                }
            )
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    }

    val bitmap = qrBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "QR Code",
            modifier = modifier
                .size(size)
                .clip(WCTheme.borderRadius.shapeLarge)
                .background(Color.White)
                .padding(WCTheme.spacing.spacing3)
                .aspectRatio(1f)
        )
    } else {
        QrShimmerPlaceholder(size = size, modifier = modifier)
    }
}

@Composable
private fun QrShimmerPlaceholder(size: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "qr_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "qr_shimmer_translate"
    )
    val shimmerBase = WCTheme.colors.foregroundSecondary
    val shimmerHighlight = WCTheme.colors.foregroundPrimary
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(shimmerBase, shimmerHighlight, shimmerBase),
        start = Offset(translateAnim - 500f, translateAnim - 500f),
        end = Offset(translateAnim, translateAnim)
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(WCTheme.borderRadius.shapeLarge)
            .background(shimmerBrush)
            .aspectRatio(1f)
    )
}

private object PosQrFrameShape : QrVectorFrameShape {
    private const val CORNER: Float = 0.45f

    override fun createPath(size: Float, neighbors: Neighbors): Path {
        val strokeWidth = size / 7f
        val outerCorner = CORNER * size
        val innerCorner = CORNER * (size - 2 * strokeWidth)
        val outerRadii = FloatArray(8) { outerCorner }
        val innerRadii = FloatArray(8) { innerCorner }

        return Path().apply {
            addRoundRect(RectF(0f, 0f, size, size), outerRadii, Path.Direction.CW)
        } - Path().apply {
            addRoundRect(
                RectF(strokeWidth, strokeWidth, size - strokeWidth, size - strokeWidth),
                innerRadii,
                Path.Direction.CCW
            )
        }
    }
}
