package com.yourname.simplenotes.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.simplenotes.data.local.entities.ChecklistItem
import com.yourname.simplenotes.data.local.entities.ContentBlock
import com.yourname.simplenotes.domain.model.Category
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.ui.theme.FOLDER_COLOR_PALETTE
import com.yourname.simplenotes.ui.theme.HeaderStyle
import com.yourname.simplenotes.ui.theme.SakuraBlushBg
import com.yourname.simplenotes.ui.theme.SakuraBlushBgDark
import com.yourname.simplenotes.ui.theme.SakuraBorderSoft
import com.yourname.simplenotes.ui.theme.SakuraBorderSoftDark
import com.yourname.simplenotes.ui.theme.SakuraPink
import com.yourname.simplenotes.ui.theme.SakuraPinkContainer
import com.yourname.simplenotes.ui.theme.SakuraPinkContainerDark
import com.yourname.simplenotes.ui.theme.SakuraPinkLight
import com.yourname.simplenotes.ui.theme.SakuraSurface
import com.yourname.simplenotes.ui.theme.SakuraSurfaceDark
import com.yourname.simplenotes.ui.theme.SakuraTextPrimary
import com.yourname.simplenotes.ui.theme.SakuraTextPrimaryDark
import com.yourname.simplenotes.ui.theme.SakuraTextSecondary
import com.yourname.simplenotes.ui.theme.SakuraTextSecondaryDark

/**
 * PhotoEvents layout adaptation for Simple Notes.
 *
 * Features:
 * 1. Top bar matching PhotoEvents (Sakura icon box + brand title + category manage + pill sort).
 * 2. Pinned horizontal Category Card carousel with customizable category colors.
 * 3. Floating selection bar (PhotoEvents bg_floating_date_badge style).
 * 4. Empty state floating badge.
 * 5. Sakura squircle FAB.
 * 6. Full Dark Mode support without text contrast issues.
 *
 * NOTE: The decor of [NoteCard] (tilt angle, ambient halo glow, luminous glowing border,
 * 📌 pushpin emoji, checklists, tags, relative dates) is preserved 100%.
 */

@Composable
fun PhotoEventsHeader(
    sortLabel: String = "Mới nhất",
    onMenuClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onSortClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) SakuraSurfaceDark else SakuraSurface
    val borderCol = if (isDark) SakuraBorderSoftDark else SakuraBorderSoft
    val textPrimary = if (isDark) SakuraTextPrimaryDark else SakuraTextPrimary
    val textSecondary = if (isDark) SakuraTextSecondaryDark else SakuraTextSecondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (onMenuClick != null) 8.dp else 20.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bên trái: Menu Button (nếu có) + Sakura Icon Box + App Name + Subtitle
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onMenuClick != null) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            // Sakura icon box (38dp)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SakuraPink, SakuraPinkLight)
                        )
                    )
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🌸", fontSize = 18.sp)
            }

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    text = "Simple Notes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "Sổ tay & Ghi chú cá nhân",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            }
        }

        // Actions: Tìm kiếm + Nút sắp xếp dạng Sakura Pill Button
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onSearchClick != null) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = SakuraPink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            Surface(
                onClick = onSortClick,
                shape = RoundedCornerShape(100.dp),
                color = surfaceColor,
                border = BorderStroke(1.2.dp, borderCol),
                shadowElevation = 1.dp,
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = SakuraPink,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = sortLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }
        }
    }
}

/** Floating selection mode pill bar matching PhotoEvents headerSelection. */
@Composable
fun PhotoEventsSelectionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit = {},
    onMove: () -> Unit = {},
    onLock: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerCol = if (isDark) SakuraPinkContainerDark else SakuraPinkContainer
    val borderCol = if (isDark) SakuraBorderSoftDark else SakuraBorderSoft

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(100.dp),
        color = containerCol,
        border = BorderStroke(1.2.dp, borderCol),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Huỷ chọn", tint = SakuraPink, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "🌸 $selectedCount đã chọn",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SakuraPink
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onSelectAll, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SelectAll, "Chọn tất cả", tint = SakuraPink, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = onMove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, "Chuyển thư mục", tint = SakuraPink, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = onLock, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Lock, "Khóa", tint = SakuraPink, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Xoá", tint = SakuraPink, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

/**
 * Pinned category card item matching PhotoEvents item_category_card.xml:
 * - 60dp height, 18dp corner radius.
 * - Circular 38dp icon frame with emoji tinted with the category's customizable color.
 * - Bold title + subtitle ("X ghi chú").
 * - Active state: takes the category's custom color, white text, translucent white icon frame, edit icon.
 * - Inactive state: clean surface, category color dot indicator, softly tinted icon frame.
 */
@Composable
fun PhotoEventsCategoryCardItem(
    name: String,
    icon: String,
    countText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    categoryColor: Color = SakuraPink,
    onEditClick: (() -> Unit)? = null,
    isAddAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isSelected) {
        categoryColor
    } else {
        if (isDark) SakuraSurfaceDark else SakuraSurface
    }
    val strokeColor = if (isSelected) {
        categoryColor
    } else if (isAddAction) {
        if (isDark) SakuraPinkLight else SakuraPink
    } else {
        if (isDark) SakuraBorderSoftDark else SakuraBorderSoft
    }
    val elevation = if (isSelected) 3.dp else 0.8.dp
    val titleColor = if (isSelected) {
        Color.White
    } else if (isAddAction) {
        if (isDark) SakuraPinkLight else SakuraPink
    } else {
        if (isDark) SakuraTextPrimaryDark else SakuraTextPrimary
    }
    val countColor = if (isSelected) {
        Color.White.copy(alpha = 0.88f)
    } else {
        if (isDark) SakuraTextSecondaryDark else SakuraTextSecondary
    }

    Card(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            width = if (isAddAction) 1.2.dp else 1.dp,
            color = strokeColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon frame / emoji
                Text(
                    text = icon,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 2.dp)
                )

                // Title & Count
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = countText,
                        fontSize = 8.sp,
                        lineHeight = 9.5.sp,
                        color = countColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Edit icon in top-end corner when selected and editable
            if (isSelected && onEditClick != null && !isAddAction) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 3.dp)
                        .size(14.dp)
                        .clickable(onClick = onEditClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Sửa danh mục",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }
    }
}

private data class StripCardItem(
    val key: String,
    val name: String,
    val icon: String,
    val countText: String,
    val color: Color,
    val isSelected: Boolean,
    val isAddAction: Boolean = false,
    val onClick: () -> Unit,
    val onEditClick: (() -> Unit)? = null,
    val categoryIndex: Int? = null
)

/**
 * Pinned category 2-row strip:
 * - Unassigned folder ("Khác") permanently pinned at index 0 (fixed, cannot be moved).
 * - User categories with custom colors, edit/delete, and drag reordering.
 * - Add new category card ("+ Tạo mới") at the end.
 * - Shrunk height + width to display cleanly in 2 rows without scrolling.
 */
@Composable
fun PhotoEventsCategoryStrip(
    categories: List<Category>,
    categoryCounts: Map<String, Int>,
    unassignedNotesCount: Int = 0,
    totalNotesCount: Int = unassignedNotesCount,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    onAddCategoryClick: () -> Unit,
    onEditCategoryClick: (Category) -> Unit,
    onReorderCategories: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragAccumulatedX by remember { mutableStateOf(0f) }

    val items = remember(categories, categoryCounts, unassignedNotesCount, selectedCategoryId) {
        buildList {
            // 1. "Khác" card - fixed at index 0, cannot change position
            add(
                StripCardItem(
                    key = "__unassigned__",
                    name = "Khác",
                    icon = "📁",
                    countText = "$unassignedNotesCount ghi chú",
                    color = SakuraPink,
                    isSelected = selectedCategoryId == null,
                    onClick = { onSelectCategory(null) }
                )
            )

            // 2..N User Category Cards
            categories.forEachIndexed { index, cat ->
                val count = categoryCounts[cat.id] ?: cat.notesCount
                val emoji = extractEmojiOrFolder(cat.name)
                val cleanName = cat.name.removePrefix(emoji).trim().ifBlank { cat.name }
                add(
                    StripCardItem(
                        key = cat.id,
                        name = cleanName,
                        icon = emoji,
                        countText = "$count ghi chú",
                        color = Color(cat.colorArgb),
                        isSelected = selectedCategoryId == cat.id,
                        onClick = { onSelectCategory(cat.id) },
                        onEditClick = { onEditCategoryClick(cat) },
                        categoryIndex = index
                    )
                )
            }

            // N+1 "+ Tạo mới" card
            add(
                StripCardItem(
                    key = "__add_new__",
                    name = "Tạo mới",
                    icon = "➕",
                    countText = "Thêm mục",
                    color = SakuraPink,
                    isSelected = false,
                    isAddAction = true,
                    onClick = onAddCategoryClick
                )
            )
        }
    }

    val total = items.size
    val row1Count = if (total <= 2) 1 else (total + 1) / 2
    val row2Count = total - row1Count
    val numColumns = maxOf(row1Count, row2Count)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (col in 0 until numColumns) {
                if (col < row1Count) {
                    val item = items[col]
                    val dragModifier = if (item.categoryIndex != null && onReorderCategories != null) {
                        val catIdx = item.categoryIndex
                        val isDragging = draggingIndex == catIdx
                        Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .pointerInput(item.key) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = catIdx
                                        dragAccumulatedX = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAccumulatedX += dragAmount.x
                                        val thresholdPx = 50.dp.toPx()
                                        if (dragAccumulatedX > thresholdPx && catIdx < categories.lastIndex) {
                                            val currentList = categories.map { it.id }.toMutableList()
                                            val targetIndex = catIdx + 1
                                            val moved = currentList.removeAt(catIdx)
                                            currentList.add(targetIndex, moved)
                                            draggingIndex = targetIndex
                                            dragAccumulatedX = 0f
                                            onReorderCategories(currentList)
                                        } else if (dragAccumulatedX < -thresholdPx && catIdx > 0) {
                                            val currentList = categories.map { it.id }.toMutableList()
                                            val targetIndex = catIdx - 1
                                            val moved = currentList.removeAt(catIdx)
                                            currentList.add(targetIndex, moved)
                                            draggingIndex = targetIndex
                                            dragAccumulatedX = 0f
                                            onReorderCategories(currentList)
                                        }
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragAccumulatedX = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragAccumulatedX = 0f
                                    }
                                )
                            }
                    } else Modifier

                    PhotoEventsCategoryCardItem(
                        name = item.name,
                        icon = item.icon,
                        countText = item.countText,
                        categoryColor = item.color,
                        isSelected = item.isSelected,
                        onClick = item.onClick,
                        onEditClick = item.onEditClick,
                        isAddAction = item.isAddAction,
                        modifier = Modifier
                            .weight(1f)
                            .then(dragModifier)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Row 2
        if (row2Count > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until numColumns) {
                    val itemIdx = row1Count + col
                    if (itemIdx < total) {
                        val item = items[itemIdx]
                        val dragModifier = if (item.categoryIndex != null && onReorderCategories != null) {
                            val catIdx = item.categoryIndex
                            val isDragging = draggingIndex == catIdx
                            Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .pointerInput(item.key) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = catIdx
                                            dragAccumulatedX = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragAccumulatedX += dragAmount.x
                                            val thresholdPx = 50.dp.toPx()
                                            if (dragAccumulatedX > thresholdPx && catIdx < categories.lastIndex) {
                                                val currentList = categories.map { it.id }.toMutableList()
                                                val targetIndex = catIdx + 1
                                                val moved = currentList.removeAt(catIdx)
                                                currentList.add(targetIndex, moved)
                                                draggingIndex = targetIndex
                                                dragAccumulatedX = 0f
                                                onReorderCategories(currentList)
                                            } else if (dragAccumulatedX < -thresholdPx && catIdx > 0) {
                                                val currentList = categories.map { it.id }.toMutableList()
                                                val targetIndex = catIdx - 1
                                                val moved = currentList.removeAt(catIdx)
                                                currentList.add(targetIndex, moved)
                                                draggingIndex = targetIndex
                                                dragAccumulatedX = 0f
                                                onReorderCategories(currentList)
                                            }
                                        },
                                        onDragEnd = {
                                            draggingIndex = null
                                            dragAccumulatedX = 0f
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragAccumulatedX = 0f
                                        }
                                    )
                                }
                        } else Modifier

                        PhotoEventsCategoryCardItem(
                            name = item.name,
                            icon = item.icon,
                            countText = item.countText,
                            categoryColor = item.color,
                            isSelected = item.isSelected,
                            onClick = item.onClick,
                            onEditClick = item.onEditClick,
                            isAddAction = item.isAddAction,
                            modifier = Modifier
                                .weight(1f)
                                .then(dragModifier)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Edit dialog allowing users to customize category name, select color from 24-color palette, or delete. */
@Composable
fun PhotoEventsEditCategoryDialog(
    category: Category,
    onSave: (Category) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var selectedColorArgb by remember { mutableStateOf(category.colorArgb) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tùy chỉnh danh mục",
                fontWeight = FontWeight.Bold,
                color = SakuraPink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên danh mục") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live preview of category card with chosen color
                Surface(
                    color = Color(selectedColorArgb),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🌸", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = name.ifBlank { "Xem trước danh mục" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "Màu đang chọn",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Text(
                    text = "🎨 Bảng màu danh mục (24 màu phong phú):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SakuraPink
                )

                // 2-row color palette
                val paletteRows = remember { FOLDER_COLOR_PALETTE.take(24).chunked(12) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    paletteRows.forEach { rowColors ->
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(rowColors) { colorInt ->
                                val isPicked = selectedColorArgb == colorInt
                                Surface(
                                    color = Color(colorInt),
                                    shape = CircleShape,
                                    onClick = { selectedColorArgb = colorInt },
                                    modifier = Modifier.size(34.dp),
                                    border = if (isPicked) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else null,
                                    shadowElevation = if (isPicked) 3.dp else 0.dp
                                ) {
                                    if (isPicked) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("Xóa danh mục", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(category.copy(name = name.trim(), colorArgb = selectedColorArgb))
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Lưu", fontWeight = FontWeight.Bold, color = SakuraPink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

/** Create dialog allowing users to create a category with name and 24-color palette. */
@Composable
fun PhotoEventsCreateCategoryDialog(
    onSave: (name: String, colorArgb: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColorArgb by remember { mutableStateOf(FOLDER_COLOR_PALETTE.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tạo danh mục mới",
                fontWeight = FontWeight.Bold,
                color = SakuraPink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên danh mục") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live preview of category card with chosen color
                Surface(
                    color = Color(selectedColorArgb),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🌸", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = name.ifBlank { "Xem trước danh mục" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "Màu đang chọn",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Text(
                    text = "🎨 Bảng màu danh mục (24 màu phong phú):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SakuraPink
                )

                // 2-row color palette
                val paletteRows = remember { FOLDER_COLOR_PALETTE.take(24).chunked(12) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    paletteRows.forEach { rowColors ->
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(rowColors) { colorInt ->
                                val isPicked = selectedColorArgb == colorInt
                                Surface(
                                    color = Color(colorInt),
                                    shape = CircleShape,
                                    onClick = { selectedColorArgb = colorInt },
                                    modifier = Modifier.size(34.dp),
                                    border = if (isPicked) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else null,
                                    shadowElevation = if (isPicked) 3.dp else 0.dp
                                ) {
                                    if (isPicked) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedColorArgb)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Tạo", fontWeight = FontWeight.Bold, color = SakuraPink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

/** Bento Sort & Display Bottom Sheet matching sheet_sort_events.xml. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEventsSortBottomSheet(
    currentSortField: SortField,
    currentSortAscending: Boolean,
    currentViewType: NoteViewType,
    onSelectSort: (SortField, Boolean) -> Unit,
    onToggleViewType: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState()
    val surfaceColor = if (isDark) SakuraSurfaceDark else SakuraSurface
    val textPrimary = if (isDark) SakuraTextPrimaryDark else SakuraTextPrimary
    val textSecondary = if (isDark) SakuraTextSecondaryDark else SakuraTextSecondary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) SakuraPinkContainerDark else SakuraPinkContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌸", fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sắp xếp & Hiển thị",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Tùy chỉnh thứ tự và cách hiển thị ghi chú",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            // View mode toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) SakuraBlushBgDark else SakuraBlushBg)
                    .clickable(onClick = onToggleViewType)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (currentViewType == NoteViewType.GRID) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = null,
                        tint = SakuraPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (currentViewType == NoteViewType.GRID) "Chế độ: Lưới so le (Masonry Grid)" else "Chế độ: Danh sách (Timeline List)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                }
                Text(
                    text = "Đổi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SakuraPink
                )
            }

            Text(
                text = "Thứ tự sắp xếp",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SakuraPink
            )

            val sortOptions = listOf(
                Triple("Mới nhất (Cập nhật gần đây)", SortField.DATE_MODIFIED, false),
                Triple("Cũ nhất", SortField.DATE_MODIFIED, true),
                Triple("Ngày tạo (Mới nhất)", SortField.DATE_CREATED, false),
                Triple("Ngày tạo (Cũ nhất)", SortField.DATE_CREATED, true),
                Triple("Tiêu đề (A → Z)", SortField.TITLE, true),
                Triple("Tiêu đề (Z → A)", SortField.TITLE, false)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sortOptions.forEach { (label, field, ascending) ->
                    val isSelected = currentSortField == field && currentSortAscending == ascending
                    val itemBg = if (isSelected) {
                        if (isDark) SakuraPinkContainerDark else SakuraPinkContainer
                    } else {
                        if (isDark) SakuraBlushBgDark else SakuraBlushBg
                    }
                    val itemBorder = if (isSelected) SakuraPink else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(itemBg)
                            .border(1.2.dp, itemBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                onSelectSort(field, ascending)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) SakuraPink else textPrimary
                        )
                        if (isSelected) {
                            Text(
                                text = "✓",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SakuraPink
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Empty state matching PhotoEvents txtEmpty floating badge style. */
@Composable
fun PhotoEventsEmptyState(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerCol = if (isDark) SakuraPinkContainerDark else SakuraPinkContainer
    val borderCol = if (isDark) SakuraBorderSoftDark else SakuraBorderSoft
    val textPrimary = if (isDark) SakuraTextPrimaryDark else SakuraTextPrimary
    val textSecondary = if (isDark) SakuraTextSecondaryDark else SakuraTextSecondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(containerCol)
                .border(2.dp, borderCol, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🌸", fontSize = 38.sp)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Chưa có ghi chú nào",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Bấm '+' bên dưới để bắt đầu lưu lại những ý tưởng đáng nhớ.",
            fontSize = 13.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

/** Sakura Squircle FAB matching PhotoEvents fabAdd. */
@Composable
fun PhotoEventsFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = SakuraPink,
        shadowElevation = 6.dp,
        modifier = modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Thêm ghi chú",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Helper to extract emoji if present in category name, else returns default folder. */
private fun extractEmojiOrFolder(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return "📁"
    val firstCodePoint = trimmed.codePointAt(0)
    val charCount = Character.charCount(firstCodePoint)
    val firstChar = trimmed.substring(0, charCount)
    if (firstCodePoint in 0x1F300..0x1FAFF || firstCodePoint in 0x2600..0x27BF) {
        return firstChar
    }
    return when {
        text.contains("công việc", ignoreCase = true) || text.contains("work", ignoreCase = true) -> "💼"
        text.contains("cá nhân", ignoreCase = true) || text.contains("personal", ignoreCase = true) -> "🎀"
        text.contains("học", ignoreCase = true) || text.contains("study", ignoreCase = true) -> "📚"
        text.contains("ý tưởng", ignoreCase = true) || text.contains("idea", ignoreCase = true) -> "💡"
        text.contains("du lịch", ignoreCase = true) || text.contains("trip", ignoreCase = true) -> "✈️"
        text.contains("mua sắm", ignoreCase = true) || text.contains("shop", ignoreCase = true) -> "🛒"
        else -> "📁"
    }
}

// ── Sample Data for Previews ──────────────────────────────────────────────────

private fun sampleCategories(): List<Category> = listOf(
    Category(id = "1", name = "💼 Công việc", notesCount = 3, colorArgb = 0xFF6366F1.toInt()),
    Category(id = "2", name = "🎀 Cá nhân", notesCount = 2, colorArgb = 0xFFEC4899.toInt()),
    Category(id = "3", name = "📚 Học tập", notesCount = 1, colorArgb = 0xFF8B5CF6.toInt()),
    Category(id = "4", name = "💡 Ý tưởng", notesCount = 1, colorArgb = 0xFF10B981.toInt()),
)

private fun sampleNotes(): List<Note> = listOf(
    Note(
        id = "note-1",
        title = "Kế hoạch sprint Q4",
        contentBlocks = listOf(
            ContentBlock.Text(
                text = "Đồng bộ layout sang chuẩn Bento Sakura Blossom của PhotoEvents, test kỹ tương thích Room DB và sync."
            )
        ),
        createdAt = System.currentTimeMillis() - 100_000,
        updatedAt = System.currentTimeMillis() - 60_000,
        folderId = "1",
        isPinned = true,
        labels = listOf("Sprint", "Priority")
    ),
    Note(
        id = "note-2",
        title = "Chuyến đi Sa Pa 🌸",
        contentBlocks = listOf(
            ContentBlock.Text(
                text = "Khách sạn view mây Fansipan tuyệt đẹp, mang theo áo ấm và máy ảnh."
            )
        ),
        createdAt = System.currentTimeMillis() - 7_200_000,
        updatedAt = System.currentTimeMillis() - 7_200_000,
        folderId = "2",
        labels = listOf("Dulịch")
    ),
    Note(
        id = "note-3",
        title = "Việc cần chuẩn bị",
        contentBlocks = listOf(
            ContentBlock.Checklist(
                items = listOf(
                    ChecklistItem(
                        id = "1",
                        text = "Hoàn tất mockup UI",
                        isCompleted = true,
                        order = 0
                    ),
                    ChecklistItem(
                        id = "2",
                        text = "Xuất file preview",
                        isCompleted = true,
                        order = 1
                    ),
                    ChecklistItem(
                        id = "3",
                        text = "Review cùng user",
                        isCompleted = false,
                        order = 2
                    )
                )
            )
        ),
        createdAt = System.currentTimeMillis() - 15_000_000,
        updatedAt = System.currentTimeMillis() - 15_000_000,
        folderId = "1",
        isPinned = true
    ),
    Note(
        id = "note-4",
        title = "Danh sách sách hay",
        contentBlocks = listOf(
            ContentBlock.Text(
                text = "1. Atomic Habits\n2. Clean Code & Kotlin in Action\n3. Designing Data-Intensive Apps"
            )
        ),
        createdAt = System.currentTimeMillis() - 86_400_000,
        updatedAt = System.currentTimeMillis() - 86_400_000,
        folderId = "2",
        backgroundColor = 0xFFFFFBEB.toInt(),
        labels = listOf("Books")
    )
)

/** Full Preview Screen showing the PhotoEvents layout with NoteCard decor preserved. */
@Composable
fun PhotoEventsNotesPreviewScreen(
    initialSelectionMode: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    var categories by remember { mutableStateOf(sampleCategories()) }
    val notes = remember { sampleNotes() }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var isSelectionMode by remember { mutableStateOf(initialSelectionMode) }
    var selectedNotes by remember { mutableStateOf(if (initialSelectionMode) setOf("note-1", "note-2") else emptySet()) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    // Category edit dialog
    categoryToEdit?.let { cat ->
        PhotoEventsEditCategoryDialog(
            category = cat,
            onSave = { updated ->
                categories = categories.map { if (it.id == updated.id) updated else it }
                categoryToEdit = null
            },
            onDismiss = { categoryToEdit = null }
        )
    }

    val unassignedNotesCount = remember(notes) {
        notes.count { it.folderId == null }
    }

    val filteredNotes = remember(selectedCategoryId, notes) {
        if (selectedCategoryId == null) notes.filter { it.folderId == null }
        else notes.filter { it.folderId == selectedCategoryId }
    }

    val categoryCounts: Map<String, Int> = remember(notes) {
        notes.mapNotNull { it.folderId }.groupingBy { it }.eachCount()
    }

    Scaffold(
        containerColor = if (isDark) SakuraBlushBgDark else SakuraBlushBg,
        floatingActionButton = {
            if (!isSelectionMode) {
                PhotoEventsFab(onClick = {})
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Header or Selection Bar
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                PhotoEventsHeader(
                    sortLabel = "Mới nhất",
                    onSortClick = {}
                )
            }

            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                PhotoEventsSelectionBar(
                    selectedCount = selectedNotes.size,
                    onCancel = { isSelectionMode = false; selectedNotes = emptySet() }
                )
            }

            // 2. Pinned Categories 2-Row Strip
            PhotoEventsCategoryStrip(
                categories = categories,
                categoryCounts = categoryCounts,
                unassignedNotesCount = unassignedNotesCount,
                totalNotesCount = notes.size,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = { selectedCategoryId = it },
                onAddCategoryClick = {},
                onEditCategoryClick = { categoryToEdit = it },
                onReorderCategories = { newIds ->
                    categories = newIds.mapNotNull { id -> categories.find { it.id == id } }
                }
            )

            Spacer(Modifier.height(8.dp))

            // 3. Notes Grid (Preserving 100% of NoteCard's decor: tilt, glow, pin, checklist, etc.)
            if (filteredNotes.isEmpty()) {
                PhotoEventsEmptyState()
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Column 0: Xếp so le nối đuôi nhau
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredNotes.filterIndexed { i, _ -> i % 2 == 0 }.forEach { note ->
                            NoteCard(
                                note = note,
                                isSelected = selectedNotes.contains(note.id),
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedNotes = if (selectedNotes.contains(note.id)) selectedNotes - note.id else selectedNotes + note.id
                                    }
                                },
                                onLongPress = {
                                    isSelectionMode = true
                                    selectedNotes = setOf(note.id)
                                },
                                onShowActions = {},
                                headerStyle = HeaderStyle.DEFAULT,
                                tilted = true
                            )
                        }
                    }

                    // Column 1: Xếp so le nối đuôi nhau
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredNotes.filterIndexed { i, _ -> i % 2 == 1 }.forEach { note ->
                            NoteCard(
                                note = note,
                                isSelected = selectedNotes.contains(note.id),
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedNotes = if (selectedNotes.contains(note.id)) selectedNotes - note.id else selectedNotes + note.id
                                    }
                                },
                                onLongPress = {
                                    isSelectionMode = true
                                    selectedNotes = setOf(note.id)
                                },
                                onShowActions = {},
                                headerStyle = HeaderStyle.DEFAULT,
                                tilted = true
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Composable Previews ───────────────────────────────────────────────────────

@Preview(name = "PhotoEvents Layout - Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PhotoEventsNotesPreviewLight() {
    MaterialTheme {
        PhotoEventsNotesPreviewScreen()
    }
}

@Preview(name = "PhotoEvents Layout - Dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PhotoEventsNotesPreviewDark() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = SakuraBlushBgDark,
            surface = SakuraSurfaceDark,
            onSurface = SakuraTextPrimaryDark,
            onSurfaceVariant = SakuraTextSecondaryDark
        )
    ) {
        PhotoEventsNotesPreviewScreen()
    }
}

@Preview(name = "PhotoEvents Layout - Selection Mode", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PhotoEventsNotesPreviewSelection() {
    MaterialTheme {
        PhotoEventsNotesPreviewScreen(initialSelectionMode = true)
    }
}
