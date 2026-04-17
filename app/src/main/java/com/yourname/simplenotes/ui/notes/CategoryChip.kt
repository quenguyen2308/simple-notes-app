package com.yourname.simplenotes.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.simplenotes.domain.model.Category

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onColorChange: ((Int) -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .background(
                color = Color(category.colorArgb),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = category.name,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
