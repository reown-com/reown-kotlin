package com.reown.appkit.ui.components.internal.commons.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.reown.appkit.ui.previews.ComponentPreview
import com.reown.appkit.ui.previews.UiModePreview
import com.reown.appkit.ui.theme.AppKitTheme
import kotlin.math.roundToInt

@Composable
internal fun AccountImage(address: String, avatarUrl: String?) {
    if (avatarUrl != null) {
        AccountAvatar(avatarUrl)
    } else {
        Box(
            modifier = Modifier
                .border(width = 8.dp, color = AppKitTheme.colors.grayGlass05, shape = CircleShape)
                .padding(8.dp)
                .size(64.dp)
                .background(brush = Brush.linearGradient(generateAvatarColors(address)), shape = CircleShape)
        )
    }
}

@Composable
private fun AccountAvatar(url: String) {
    Box(
        modifier = Modifier
            .border(width = 8.dp, color = AppKitTheme.colors.grayGlass05, shape = CircleShape)
            .padding(8.dp)
            .size(64.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
        )
    }
}

internal fun generateAvatarColors(address: String): List<Color> {
    // Default color in case of invalid input (a neutral blue shade)
    val defaultBaseColor = "4287f5"

    try {
        val hash = address.takeIf { it.isNotEmpty() }
            ?.lowercase()
            ?.replace("^0x".toRegex(), "")
            ?: defaultBaseColor

        // Ensure we have at least 6 characters for the color
        val baseColor = when {
            hash.length >= 6 -> hash.substring(0, 6)
            hash.isNotEmpty() -> hash.padEnd(6, hash.last())
            else -> defaultBaseColor
        }

        val rgbColor = hexToRgb(baseColor)
        val colors: MutableList<Color> = mutableListOf()

        for (i in 0 until 5) {
            val tintedColor = tintColor(rgbColor, 0.15 * i)
            colors.add(Color(tintedColor.first, tintedColor.second, tintedColor.third))
        }

        return colors
    } catch (e: Exception) {
        // If anything goes wrong, return colors based on the default color
        val rgbColor = hexToRgb(defaultBaseColor)
        return List(5) { i ->
            val tintedColor = tintColor(rgbColor, 0.15 * i)
            Color(tintedColor.first, tintedColor.second, tintedColor.third)
        }
    }
}

internal fun hexToRgb(hex: String): Triple<Int, Int, Int> {
    val bigint = hex.toLong(16)
    val r = (bigint shr 16 and 255).toInt()
    val g = (bigint shr 8 and 255).toInt()
    val b = (bigint and 255).toInt()
    return Triple(r, g, b)
}

internal fun tintColor(rgb: Triple<Int, Int, Int>, tint: Double): Triple<Int, Int, Int> {
    val (r, g, b) = rgb
    val tintedR = (r + (255 - r) * tint).roundToInt()
    val tintedG = (g + (255 - g) * tint).roundToInt()
    val tintedB = (b + (255 - b) * tint).roundToInt()
    return Triple(tintedR, tintedG, tintedB)
}

@UiModePreview
@Composable
private fun AddressImagePreview() {
    ComponentPreview {
        AccountImage("0x59eAF7DD5a2f5e433083D8BbC8de3439542579cb", null)
    }
}
