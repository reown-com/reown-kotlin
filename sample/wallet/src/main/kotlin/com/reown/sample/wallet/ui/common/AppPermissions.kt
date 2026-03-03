@file:JvmSynthetic

package com.reown.sample.wallet.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.reown.sample.common.ui.themedColor
import com.reown.sample.common.ui.theme.WCTheme
import com.reown.sample.wallet.R

private data class Permission(
    val text: String,
    val allowed: Boolean,
)

private val PERMISSIONS = listOf(
    Permission("View your balance & activity", allowed = true),
    Permission("Request transaction approvals", allowed = true),
    Permission("Move funds without permission", allowed = false),
)

@Composable
fun AppPermissions() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PERMISSIONS.forEach { permission ->
            val bgColor = if (permission.allowed) Color(0xFF30A46B) else Color(0xFFDF4A34)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(bgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            id = if (permission.allowed) R.drawable.ic_check else R.drawable.ic_close
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = permission.text,
                    style = WCTheme.typography.bodyMdRegular.copy(
                        color = themedColor(darkColor = 0xFFe3e7e7, lightColor = 0xFF202020)
                    )
                )
            }
        }
    }
}
