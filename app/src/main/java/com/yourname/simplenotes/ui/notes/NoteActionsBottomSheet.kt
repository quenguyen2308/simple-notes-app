package com.yourname.simplenotes.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourname.simplenotes.domain.model.Note

/**
 * Samsung Notes-style bottom sheet showing contextual actions for a single note.
 * All action callbacks invoke [onDismiss] after execution to auto-close the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteActionsBottomSheet(
    note: Note,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onLock: () -> Unit = {},
    onMoveToFolder: () -> Unit = {},
    onAddLabel: () -> Unit = {},
    onChangeColor: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            // Sheet title
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider()

            // Move to folder
            ListItem(
                headlineContent = { Text("Move to folder") },
                leadingContent = {
                    Icon(Icons.Default.FolderOpen, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth().clickable { onMoveToFolder(); onDismiss() }
            )

            // Add label
            ListItem(
                headlineContent = { Text("Add label") },
                leadingContent = {
                    Icon(Icons.Default.Label, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth().clickable { onAddLabel(); onDismiss() }
            )

            // Change color
            ListItem(
                headlineContent = { Text("Change color") },
                leadingContent = {
                    Icon(Icons.Default.Palette, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth().clickable { onChangeColor(); onDismiss() }
            )

            // Pin / Unpin — text depends on current pin state
            ListItem(
                headlineContent = { Text(if (note.isPinned) "Unpin" else "Pin") },
                leadingContent = {
                    Icon(Icons.Default.PushPin, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth().clickable { onPin(); onDismiss() }
            )

            // Lock / Unlock
            ListItem(
                headlineContent = { Text(if (note.isLocked) "Unlock" else "Lock") },
                leadingContent = {
                    Icon(
                        if (note.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth().clickable { onLock(); onDismiss() }
            )

            HorizontalDivider()

            // Delete — destructive, shown in error color
            ListItem(
                headlineContent = {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.fillMaxWidth().clickable { onDelete(); onDismiss() }
            )
        }
    }
}
