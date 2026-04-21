package com.walletconnect.sample.pos.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.walletconnect.sample.pos.components.CloseButton
import com.walletconnect.sample.pos.components.PosHeader
import com.walletconnect.sample.pos.log.LogEntry
import com.walletconnect.sample.pos.log.LogLevel
import com.walletconnect.sample.pos.log.PosLogStore
import com.walletconnect.sample.pos.ui.theme.WCTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logs by PosLogStore.logs.collectAsState()

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

        // Clear Logs button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WCTheme.spacing.spacing5)
                .height(50.dp)
                .clip(WCTheme.borderRadius.shapeLarge)
                .background(WCTheme.colors.foregroundPrimary)
                .clickable { PosLogStore.clear() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Clear Logs",
                style = WCTheme.typography.bodyLgMedium,
                color = WCTheme.colors.textPrimary
            )
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing3))

        if (logs.isEmpty()) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "No logs yet",
                style = WCTheme.typography.bodyLgRegular,
                color = WCTheme.colors.textTertiary
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = WCTheme.spacing.spacing5),
                verticalArrangement = Arrangement.spacedBy(WCTheme.spacing.spacing2)
            ) {
                items(logs, key = { it.id }) { entry ->
                    LogEntryCard(entry)
                }
            }
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing3))

        CloseButton(onClick = onClose)

        Spacer(Modifier.height(WCTheme.spacing.spacing5))
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(WCTheme.borderRadius.shapeMedium)
            .background(WCTheme.colors.foregroundPrimary)
            .padding(WCTheme.spacing.spacing4)
    ) {
        // Header row: level badge + timestamp
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(entry.level)
            Spacer(Modifier.width(WCTheme.spacing.spacing2))
            Text(
                text = formatTimestamp(entry.timestamp),
                style = WCTheme.typography.bodySmRegular,
                color = WCTheme.colors.textTertiary
            )
        }

        // Source
        if (entry.source != null) {
            Spacer(Modifier.height(WCTheme.spacing.spacing1))
            Text(
                text = entry.source,
                style = WCTheme.typography.bodySmRegular,
                color = WCTheme.colors.textTertiary
            )
        }

        Spacer(Modifier.height(WCTheme.spacing.spacing1))

        // Message
        Text(
            text = entry.message,
            style = WCTheme.typography.bodyMdMedium,
            color = WCTheme.colors.textPrimary
        )

        // Data
        if (entry.data != null) {
            Spacer(Modifier.height(WCTheme.spacing.spacing1))
            Text(
                text = entry.data,
                style = WCTheme.typography.bodySmRegular.copy(fontFamily = FontFamily.Monospace),
                color = WCTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun LevelBadge(level: LogLevel) {
    val bgColor = when (level) {
        LogLevel.INFO -> WCTheme.colors.bgAccentPrimary
        LogLevel.ERROR -> WCTheme.colors.bgError
    }
    Box(
        modifier = Modifier
            .clip(WCTheme.borderRadius.shapeXSmall)
            .background(bgColor)
            .padding(horizontal = WCTheme.spacing.spacing2, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level.name,
            style = WCTheme.typography.bodySmMedium,
            color = WCTheme.colors.textInvert
        )
    }
}

private val timestampFormat = SimpleDateFormat("MM/dd, hh:mm:ss a", Locale.US)

private fun formatTimestamp(millis: Long): String = timestampFormat.format(Date(millis))
