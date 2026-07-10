package com.yourname.simplenotes.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.simplenotes.domain.model.Category

@Composable
fun FolderScreen(
    folders: List<Category> = sampleFolders(),
    onFolderSelected: (String?) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    val totalNotes = folders.sumOf { it.notesCount }

    Scaffold(
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Folders",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${folders.size} Folders, $totalNotes notes",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            // Top actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
                Row {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            }

            // Breadcrumb
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(22.sp.value.dp)
                )
                Icon(
                    Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(22.sp.value.dp)
                )
                Text(
                    "Folders",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Grid of folders
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .weight(1f),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders) { folder ->
                    FolderCard(
                        folder = folder,
                        onClick = { onFolderSelected(folder.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderCard(folder: Category, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(70.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth(0.65f)
                    .height(10.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 12.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .background(Color(folder.colorArgb))
            )
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(
                    text = folder.notesCount.toString(),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Text(
                    lineHeight = 16.sp,
                    text = folder.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun FolderScreenPreview() {
    MaterialTheme {
        FolderScreen()
    }
}

private fun sampleFolders(): List<Category> = listOf(
    Category(id = "1", name = "Trip", notesCount = 125, colorArgb = 0xFFFFC107.toInt()),
    Category(id = "2", name = "Screen off memo", notesCount = 17, colorArgb = 0xFF5C7CFA.toInt()),
    Category(id = "3", name = "Shopping", notesCount = 36, colorArgb = 0xFFBDBDBD.toInt()),
    Category(id = "4", name = "Recipe", notesCount = 5, colorArgb = 0xFFFF6B6B.toInt()),
)
