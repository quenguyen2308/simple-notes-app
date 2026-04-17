package com.yourname.simplenotes.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourname.simplenotes.data.local.entities.ChecklistItem

/**
 * Interactive checklist editor for note content.
 * Shows progress bar, per-item checkbox + text field + delete,
 * and an "Add item" button at the bottom.
 */
@Composable
fun NoteChecklistEditor(
    items: List<ChecklistItem>,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onToggleItem: (String) -> Unit,
    onUpdateItemText: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Transparent field colors — removes underline from checklist text fields
    val transparentColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    Column(modifier = modifier) {
        // Progress bar + completion count
        if (items.isNotEmpty()) {
            val completedCount = items.count { it.isCompleted }
            LinearProgressIndicator(
                progress = { completedCount.toFloat() / items.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Text(
                text = "$completedCount / ${items.size} completed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // Checklist items
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ChecklistItemRow(
                    item = item,
                    onToggle = { onToggleItem(item.id) },
                    onTextChange = { text -> onUpdateItemText(item.id, text) },
                    onDelete = { onRemoveItem(item.id) },
                    transparentColors = transparentColors
                )
            }
        }

        // Add item button
        TextButton(
            onClick = onAddItem,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("Add item")
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    transparentColors: androidx.compose.material3.TextFieldColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = { onToggle() }
        )
        TextField(
            value = item.text,
            onValueChange = onTextChange,
            placeholder = { Text("Item", style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            colors = transparentColors,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove item",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
