package com.walletconnect.sample.pos.components

import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.walletconnect.sample.pos.R

@Composable
fun StyledQrCode(
    data: String,
    size: Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val qrDrawable = remember(data) {
        val logo = ContextCompat.getDrawable(context, R.drawable.ic_wc_qr_logo)
        QrCodeDrawable(
            data = QrData.Url(data),
            options = createQrVectorOptions {
                padding = .0f
                logo {
                    drawable = logo
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
    }

    Image(
        painter = rememberDrawablePainter(drawable = qrDrawable),
        contentDescription = "QR Code",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(12.dp)
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
