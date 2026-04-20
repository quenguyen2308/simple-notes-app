package com.yourname.simplenotes.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.simplenotes.domain.model.Note

@Composable
fun RecycleBinScreen(
    notes: List<Note>,
    onRestore: (String) -> Unit,
    onPermanentDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    var confirmDeleteNote by remember { mutableStateOf<Note?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                "Recycle Bin",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            if (notes.isNotEmpty()) {
                TextButton(onClick = { showClearAllConfirm = true }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Recycle bin is empty",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    RecycleBinNoteItem(
                        note = note,
                        onRestore = { onRestore(note.id) },
                        onDelete = { confirmDeleteNote = note }
                    )
                }
            }
        }
    }

    confirmDeleteNote?.let { note ->
        AlertDialog(
            onDismissRequest = { confirmDeleteNote = null },
            title = { Text("Delete permanently") },
            text = { Text("\"${note.title.ifBlank { "Note" }}\" will be deleted forever and cannot be recovered.") },
            confirmButton = {
                TextButton(onClick = {
                    onPermanentDelete(note.id)
                    confirmDeleteNote = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteNote = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear Recycle Bin") },
            text = { Text("All ${notes.size} notes will be permanently deleted and cannot be recovered.") },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearAllConfirm = false }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RecycleBinNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val daysLeft = remember(note.updatedAt) {
        val elapsed = System.currentTimeMillis() - note.updatedAt
        maxOf(0L, 30L - elapsed / (24L * 60 * 60 * 1000))
    }
    val expiryText = if (daysLeft == 0L) "Xóa hôm nay" else "Xóa sau $daysLeft ngày"
    val expiryColor = if (daysLeft <= 3) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = expiryText,
                    fontSize = 12.sp,
                    color = expiryColor
                )
            }
            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Default.RestoreFromTrash,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = "Delete permanently",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
