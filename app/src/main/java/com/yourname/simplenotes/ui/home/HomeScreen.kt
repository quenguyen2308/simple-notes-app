package com.yourname.simplenotes.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.simplenotes.domain.model.Note
import com.yourname.simplenotes.ui.notes.NoteCard
import com.yourname.simplenotes.ui.notes.NoteListViewModel
import com.yourname.simplenotes.ui.notes.StyledFab
import com.yourname.simplenotes.ui.notes.StyledIconButton
import com.yourname.simplenotes.ui.settings.SettingsPrefs
import com.yourname.simplenotes.ui.theme.Baloo2Family
import com.yourname.simplenotes.ui.theme.HeaderStyle
import com.yourname.simplenotes.ui.theme.NotezInk
import com.yourname.simplenotes.ui.theme.NotezMintLight
import com.yourname.simplenotes.ui.theme.NotezPinkLight
import com.yourname.simplenotes.ui.theme.NotezPurpleLight
import com.yourname.simplenotes.ui.theme.appBackground
import com.yourname.simplenotes.util.BiometricHelper
import org.koin.androidx.compose.koinViewModel

/**
 * "Trang chủ" dashboard tab — greeting header, quick-action shortcuts, a pinned-notes preview,
 * and a recent-notes preview. Reuses [NoteListViewModel] directly (no dedicated ViewModel): this
 * screen's own Koin-scoped instance observes the same Room flows as the Ghi chú tab's instance,
 * so writes from either tab stay consistent without any explicit state sharing.
 */
@Composable
fun HomeScreen(
    onNoteClick: (String) -> Unit,
    onNewBlankNote: () -> Unit,
    onNewImageNote: () -> Unit,
    onNewChecklistNote: () -> Unit,
    onSearchClick: () -> Unit,
    onSeeAllPinned: () -> Unit,
    onSeeAllRecent: () -> Unit,
    viewModel: NoteListViewModel = koinViewModel()
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val recentNotes by viewModel.recentNotes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsPrefs = remember { SettingsPrefs(context) }
    var headerStyle by remember { mutableStateOf(HeaderStyle.fromStorageKey(settingsPrefs.headerStyle)) }

    // Settings now lives on its own tab (not an in-place overlay), so re-read the picked
    // header style whenever this tab resumes rather than on a dismiss callback.
    LifecycleResumeEffect(viewModel) {
        headerStyle = HeaderStyle.fromStorageKey(settingsPrefs.headerStyle)
        viewModel.onResume()
        onPauseOrDispose { }
    }

    val pinnedNotes = remember(notes) {
        notes.filter { it.isPinned }.sortedByDescending { it.contentUpdatedAt }.take(4)
    }
    val recentList = remember(recentNotes) { recentNotes.take(5) }
    val isNotez = headerStyle == HeaderStyle.NOTEZ

    fun handleNoteClick(note: Note) {
        if (!note.isLocked) { onNoteClick(note.id); return }
        BiometricHelper.authenticateWithDeviceCredential(
            activity = context as FragmentActivity,
            title    = "Mở khóa ghi chú",
            onSuccess = { onNoteClick(note.id) },
            onError   = {}
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { StyledFab(style = headerStyle, onClick = onNewBlankNote) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .appBackground()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Greeting + search icon ───────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (isNotez) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Xin chào, bạn 👋",
                                style = TextStyle(
                                    fontFamily = Baloo2Family,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = NotezInk
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            HomeSparkleAccent()
                        }
                    } else {
                        Text(
                            "Xin chào, bạn 👋",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                StyledIconButton(
                    style = headerStyle,
                    onClick = onSearchClick,
                    icon = Icons.Default.Search,
                    contentDescription = "Tìm kiếm"
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Tappable "search bar" pill ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon2(Icons.Default.Search, MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Tìm kiếm ghi chú...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Quick-action pills ────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionPill(
                    icon = Icons.Default.NoteAdd,
                    label = "Ghi chú nhanh",
                    background = if (isNotez) NotezPurpleLight.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer,
                    tint = if (isNotez) NotezInk else MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onNewBlankNote,
                    modifier = Modifier.weight(1f)
                )
                QuickActionPill(
                    icon = Icons.Default.Image,
                    label = "Hình ảnh",
                    background = if (isNotez) NotezPinkLight.copy(alpha = 0.22f) else MaterialTheme.colorScheme.secondaryContainer,
                    tint = if (isNotez) NotezInk else MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onNewImageNote,
                    modifier = Modifier.weight(1f)
                )
                QuickActionPill(
                    icon = Icons.Default.CheckBox,
                    label = "Checklist",
                    background = if (isNotez) NotezMintLight.copy(alpha = 0.22f) else MaterialTheme.colorScheme.tertiaryContainer,
                    tint = if (isNotez) NotezInk else MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onNewChecklistNote,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Ghim (pinned) preview ─────────────────────────────────
            if (pinnedNotes.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                SectionHeader(
                    icon = Icons.Default.PushPin,
                    title = "Ghim",
                    isNotez = isNotez,
                    onSeeAll = onSeeAllPinned
                )
                Spacer(Modifier.height(8.dp))
                pinnedNotes.chunked(2).forEach { rowNotes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowNotes.forEach { note ->
                            NoteCard(
                                note = note,
                                onClick = { handleNoteClick(note) },
                                tilted = true,
                                headerStyle = headerStyle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowNotes.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── Ghi chú gần đây (recent) preview ─────────────────────
            Spacer(Modifier.height(12.dp))
            SectionHeader(
                icon = null,
                title = "Ghi chú gần đây",
                isNotez = isNotez,
                onSeeAll = onSeeAllRecent
            )
            Spacer(Modifier.height(8.dp))
            if (recentList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Chưa có ghi chú\nNhấn + để tạo mới",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                recentList.forEach { note ->
                    NoteCard(
                        note = note,
                        onClick = { handleNoteClick(note) },
                        headerStyle = headerStyle,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun Icon2(icon: ImageVector, tint: Color) {
    androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
}

@Composable
private fun SectionHeader(icon: ImageVector?, title: String, isNotez: Boolean, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    icon, contentDescription = null,
                    tint = if (isNotez) NotezPinkLight else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            "Xem tất cả",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onSeeAll)
        )
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = tint, textAlign = TextAlign.Center)
    }
}
