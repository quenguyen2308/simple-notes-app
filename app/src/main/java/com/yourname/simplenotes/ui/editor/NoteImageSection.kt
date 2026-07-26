package com.yourname.simplenotes.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yourname.simplenotes.data.local.entities.ContentBlock

/**
 * Displays attached image blocks, each with a delete overlay in the top-right corner.
 * Inserting a new image is done from the editor's bottom toolbar (Image button), not here.
 */
@Composable
fun NoteImageSection(
    imageBlocks: List<ContentBlock.Image>,
    onRemoveImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        imageBlocks.forEach { block ->
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                AsyncImage(
                    model = block.uri,
                    contentDescription = block.caption.ifEmpty { "Attached image" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                // Delete button overlay — top-right corner
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { onRemoveImage(block.uri) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
