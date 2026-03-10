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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
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
    val colors = WCTheme.colors
    val spacing = WCTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.spacing3)) {
        PERMISSIONS.forEach { permission ->
            val statusColor = if (permission.allowed) colors.textSuccess else colors.textError
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(spacing.spacing5)
                        .background(statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            id = if (permission.allowed) R.drawable.ic_check else R.drawable.ic_close
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(spacing.spacing3),
                        tint = colors.textInvert
                    )
                }
                Spacer(modifier = Modifier.width(spacing.spacing2))
                Text(
                    text = permission.text,
                    style = WCTheme.typography.bodyMdRegular.copy(
                        color = colors.textPrimary
                    )
                )
            }
        }
    }
}
