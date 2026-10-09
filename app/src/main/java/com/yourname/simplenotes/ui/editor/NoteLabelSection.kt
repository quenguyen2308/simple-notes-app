package com.yourname.simplenotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.simplenotes.ui.theme.FrostedGlassBgDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBgLight
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderDark
import com.yourname.simplenotes.ui.theme.FrostedGlassBorderLight
import com.yourname.simplenotes.ui.theme.SakuraPink
import com.yourname.simplenotes.ui.theme.isAppInDarkTheme

/**
 * Horizontally scrollable row of label chips with remove buttons,
 * plus a "+ Thêm nhãn" chip that opens a Bento Sakura dialog to type a new label.
 *
 * Shows: [#Label1 x] [#Label2 x] [+ Thêm nhãn]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteLabelSection(
    labels: List<String>,
    onAddLabel: (String) -> Unit,
    onRemoveLabel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEach { label ->
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("#$label") },
                trailingIcon = {
                    IconButton(
                        onClick = { onRemoveLabel(label) },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xóa nhãn $label"
                        )
                    }
                }
            )
        }
        SuggestionChip(
            onClick = { inputText = ""; showDialog = true },
            label = { Text("+ Thêm nhãn") }
        )
    }

    if (showDialog) {
        val isDark = isAppInDarkTheme()
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = if (isDark) FrostedGlassBgDark else FrostedGlassBgLight,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(
                BorderStroke(1.2.dp, if (isDark) FrostedGlassBorderDark else FrostedGlassBorderLight),
                RoundedCornerShape(28.dp)
            ),
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .background(SakuraPink.copy(alpha = 0.14f), CircleShape)
                        .border(1.dp, SakuraPink.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        tint = SakuraPink,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Thêm nhãn mới",
                    fontWeight = FontWeight.Bold,
                    color = SakuraPink
                )
            },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Tên nhãn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { onAddLabel(inputText.trim()); showDialog = false },
                    enabled = inputText.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SakuraPink)
                ) {
                    Text("Thêm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}
