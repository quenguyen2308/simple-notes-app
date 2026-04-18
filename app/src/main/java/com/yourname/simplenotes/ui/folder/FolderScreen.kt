package com.yourname.simplenotes.ui.folder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/** Full-screen folder management: tree browser + create/rename/delete dialogs. */
@Composable
fun FolderScreen(
    onFolderSelected: (String?) -> Unit,
    viewModel: FolderViewModel = koinViewModel()
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createParentId by remember { mutableStateOf<String?>(null) }
    var contextFolderId by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        FolderBrowser(
            folders = folders,
            selectedFolderId = selectedFolderId,
            onFolderSelect = { id ->
                viewModel.selectedFolderId.value = id
                onFolderSelected(id)
            },
            onFolderLongPress = { id -> contextFolderId = id },
            modifier = Modifier.fillMaxSize().padding(bottom = 72.dp)
        )
        FloatingActionButton(
            onClick = { createParentId = null; showCreateDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Color(0xFFFFC107),
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "New folder")
        }
    }

    // Long-press context menu
    contextFolderId?.let { folderId ->
        val folder = flattenHierarchy(folders).find { it.id == folderId }
        AlertDialog(
            onDismissRequest = { contextFolderId = null },
            title = { Text(folder?.name ?: "Folder") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            renameTarget = folderId to (folder?.name ?: "")
                            contextFolderId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rename") }
                    TextButton(
                        onClick = {
                            createParentId = folderId
                            showCreateDialog = true
                            contextFolderId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add Subfolder") }
                    TextButton(
                        onClick = { deleteConfirmId = folderId; contextFolderId = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { contextFolderId = null }) { Text("Cancel") } }
        )
    }

    deleteConfirmId?.let { folderId ->
        val folder = flattenHierarchy(folders).find { it.id == folderId }
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text("Xóa thư mục") },
            text = { Text("Bạn có chắc muốn xóa \"${folder?.name ?: "thư mục"}\" không? Các ghi chú bên trong sẽ không bị xóa.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteFolder(folderId); deleteConfirmId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmId = null }) { Text("Hủy") } }
        )
    }

    if (showCreateDialog) {
        FolderNameDialog(
            title = if (createParentId == null) "New Folder" else "New Subfolder",
            initialName = "",
            onConfirm = { name, color ->
                viewModel.createFolder(name, color, createParentId)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    renameTarget?.let { (id, currentName) ->
        FolderNameDialog(
            title = "Rename Folder",
            initialName = currentName,
            onConfirm = { newName, _ -> viewModel.renameFolder(id, newName); renameTarget = null },
            onDismiss = { renameTarget = null }
        )
    }
}

/** Dialog for entering or editing a folder name and picking a color. */
@Composable
private fun FolderNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(0xFF1976D2.toInt()) }
    val colorOptions = listOf(
        0xFF1976D2.toInt(), 0xFF388E3C.toInt(), 0xFFD32F2F.toInt(),
        0xFF7B1FA2.toInt(), 0xFFF57C00.toInt(), 0xFF0097A7.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (name.isNotBlank()) onConfirm(name, selectedColor) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { colorInt ->
                        Surface(
                            color = Color(colorInt),
                            shape = CircleShape,
                            onClick = { selectedColor = colorInt },
                            modifier = Modifier.size(32.dp),
                            border = if (selectedColor == colorInt)
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                            else null
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
