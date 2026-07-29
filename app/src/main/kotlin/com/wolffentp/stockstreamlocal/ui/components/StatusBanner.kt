package com.wolffentp.stockstreamlocal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolffentp.stockstreamlocal.market.provider.RefreshState
import com.wolffentp.stockstreamlocal.util.Formatters
import java.time.Instant

/**
 * Banner shown at the top of quote screens indicating connection / refresh status.
 * Never shows fake data — only provider-reported states.
 */
@Composable
fun StatusBanner(
    refreshState: RefreshState,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    val (icon, message, containerColor) = when {
        !isOnline ->
            Triple(Icons.Default.WifiOff, "Offline — live quotes unavailable", MaterialTheme.colorScheme.errorContainer)
        refreshState is RefreshState.Offline ->
            Triple(Icons.Default.WifiOff, "Offline", MaterialTheme.colorScheme.errorContainer)
        refreshState is RefreshState.Streaming ->
            Triple(Icons.Default.Wifi, "● Live — ${Formatters.timestamp(refreshState.at)}", MaterialTheme.colorScheme.primaryContainer)
        refreshState is RefreshState.Refreshing ->
            Triple(Icons.Default.Sync, "Refreshing…", MaterialTheme.colorScheme.surfaceVariant)
        refreshState is RefreshState.Success ->
            Triple(Icons.Default.CheckCircle, "Updated ${Formatters.timestamp(refreshState.at)}", MaterialTheme.colorScheme.secondaryContainer)
        refreshState is RefreshState.Error ->
            Triple(Icons.Default.Warning, refreshState.message, MaterialTheme.colorScheme.errorContainer)
        refreshState is RefreshState.Paused ->
            Triple(Icons.Default.Pause, "Refresh paused", MaterialTheme.colorScheme.surfaceVariant)
        else -> return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, style = MaterialTheme.typography.labelSmall)
    }
}
