package com.yourname.simplenotes.ui.folder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourname.simplenotes.domain.model.Category

/** Folder tree browser: expandable hierarchy with breadcrumb navigation. */
@Composable
fun FolderBrowser(
    folders: List<Category>,
    selectedFolderId: String?,
    onFolderSelect: (String?) -> Unit,
    onFolderLongPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = modifier) {
        FolderBreadcrumb(
            currentFolderId = selectedFolderId,
            folders = folders,
            onNavigate = onFolderSelect
        )
        HorizontalDivider()
        if (folders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No folders yet. Tap + to create one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(folders, key = { it.id }) { folder ->
                    FolderTreeItem(
                        folder = folder,
                        level = 0,
                        isSelected = selectedFolderId == folder.id,
                        expandedIds = expandedIds,
                        onToggleExpand = { id ->
                            expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
                        },
                        onSelect = onFolderSelect,
                        onLongPress = onFolderLongPress
                    )
                }
            }
        }
    }
}

/** Single row in the folder tree; recursively renders [folder.subFolders] when expanded. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderTreeItem(
    folder: Category,
    level: Int,
    isSelected: Boolean,
    expandedIds: Set<String>,
    onToggleExpand: (String) -> Unit,
    onSelect: (String?) -> Unit,
    onLongPress: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(start = (level * 16).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (folder.subFolders.isNotEmpty()) {
                IconButton(onClick = { onToggleExpand(folder.id) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (folder.id in expandedIds) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle expand",
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(36.dp))
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = { onSelect(folder.id) },
                        onLongClick = { onLongPress(folder.id) }
                    )
                    .padding(top = 10.dp, bottom = 10.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(folder.colorArgb),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    folder.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (folder.notesCount > 0) {
                    Text(
                        "${folder.notesCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (folder.id in expandedIds) {
            folder.subFolders.forEach { sub ->
                FolderTreeItem(sub, level + 1, isSelected, expandedIds, onToggleExpand, onSelect, onLongPress)
            }
        }
    }
}

/** Horizontal breadcrumb showing path from root to [currentFolderId]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderBreadcrumb(
    currentFolderId: String?,
    folders: List<Category>,
    onNavigate: (String?) -> Unit
) {
    val path = remember(currentFolderId, folders) {
        buildBreadcrumbPath(currentFolderId, flattenHierarchy(folders))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val allColor = if (currentFolderId == null) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            "All",
            style = MaterialTheme.typography.labelMedium,
            color = allColor,
            modifier = Modifier.combinedClickable(onClick = { onNavigate(null) }, onLongClick = {})
        )
        path.forEach { folder ->
            Icon(Icons.AutoMirrored.Filled.NavigateNext, null, modifier = Modifier.size(14.dp))
            val color = if (currentFolderId == folder.id) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                folder.name,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                modifier = Modifier.combinedClickable(onClick = { onNavigate(folder.id) }, onLongClick = {})
            )
        }
    }
}

private fun buildBreadcrumbPath(folderId: String?, flat: List<Category>): List<Category> {
    if (folderId == null) return emptyList()
    val map = flat.associateBy { it.id }
    val path = mutableListOf<Category>()
    var currentId: String? = folderId
    while (currentId != null) {
        val folder = map[currentId] ?: break
        path.add(0, folder)
        currentId = folder.parentId
    }
    return path
}

/** Flattens a hierarchy tree into a plain list (preserves parentId on each item). */
internal fun flattenHierarchy(folders: List<Category>): List<Category> {
    val result = mutableListOf<Category>()
    fun flatten(cats: List<Category>) { cats.forEach { result.add(it); flatten(it.subFolders) } }
    flatten(folders)
    return result
}
