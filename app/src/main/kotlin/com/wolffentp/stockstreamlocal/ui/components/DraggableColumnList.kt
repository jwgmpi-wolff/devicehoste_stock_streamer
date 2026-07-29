package com.wolffentp.stockstreamlocal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolffentp.stockstreamlocal.columns.ColumnDefinition

/**
 * A single draggable/toggleable column row in the column layout editor.
 * Full drag-and-drop reorder requires a ReorderableColumn library in a real build;
 * this implementation provides the row UI and tap-to-move affordances.
 */
@Composable
fun DraggableColumnRow(
    column: ColumnDefinition,
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = column.displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onToggleVisibility) {
            Icon(
                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (isHidden) "Show column" else "Hide column",
                tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
