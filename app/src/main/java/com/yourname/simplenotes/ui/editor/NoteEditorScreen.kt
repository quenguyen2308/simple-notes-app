package com.yourname.simplenotes.ui.editor

import android.content.Intent
import android.provider.Settings
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourname.simplenotes.util.BiometricHelper
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    onBack: () -> Unit,
    viewModel: NoteEditorViewModel = koinViewModel()
) {
    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var categoryExpanded by remember { mutableStateOf(false) }
    var showNoPasscodeDialog by remember { mutableStateOf(false) }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(backDispatcher) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { viewModel.save(); onBack() }
        }
        backDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    val dateText = remember(viewModel.createdAtMs) {
        val ms = if (viewModel.createdAtMs > 0L) viewModel.createdAtMs else System.currentTimeMillis()
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(ms))
    }

    // Gallery image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addImage(it.toString()) }
    }

    // Transparent colors for borderless TextField appearance
    val borderlessColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.save(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text(if (noteId == "new") "New note" else "Edit note") },
                actions = {
                    // Share note as plain text
                    IconButton(onClick = {
                        val text = buildString {
                            append(viewModel.title)
                            appendLine()
                            if (viewModel.isChecklistMode) {
                                viewModel.checklistItems.forEach { item ->
                                    appendLine("${if (item.isCompleted) "☑" else "☐"} ${item.text}")
                                }
                            } else {
                                append(viewModel.richTextState.annotatedString.text)
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, viewModel.title)
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share note"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    // Checklist mode toggle
                    IconButton(onClick = viewModel::toggleChecklistMode) {
                        Icon(
                            if (viewModel.isChecklistMode) Icons.Default.CheckBox
                            else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Toggle checklist"
                        )
                    }
                    // Pin toggle
                    IconButton(onClick = viewModel::onPinToggle) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = if (viewModel.isPinned) "Unpin" else "Pin note",
                            tint = if (viewModel.isPinned) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Lock toggle — uses device credential as the passcode
                    IconButton(onClick = {
                        if (viewModel.isLocked) {
                            // Unlock using device credential (biometric + device PIN/pattern/password)
                            BiometricHelper.authenticateWithDeviceCredential(
                                activity = context as FragmentActivity,
                                title = "Unlock Note",
                                onSuccess = { viewModel.setLock(locked = false, newPinHash = null) },
                                onError = {}
                            )
                        } else {
                            // Lock: require device to have a passcode; otherwise direct user to set one up
                            if (BiometricHelper.isDeviceSecure(context)) {
                                viewModel.setLock(locked = true, newPinHash = null)
                            } else {
                                showNoPasscodeDialog = true
                            }
                        }
                    }) {
                        Icon(
                            if (viewModel.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            if (viewModel.isLocked) "Locked" else "Lock note"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(viewModel.backgroundColor))
        ) {
            // Creation date
            Text(
                dateText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Color picker row
            NoteColorPicker(
                selectedColor = viewModel.backgroundColor,
                onColorSelected = viewModel::onBackgroundColorChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Category picker
            val selectedCategory = categories.find { it.id == viewModel.selectedCategoryId }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "No category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("No category") },
                        onClick = { viewModel.onCategoryChange(null); categoryExpanded = false }
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = { viewModel.onCategoryChange(cat.id); categoryExpanded = false }
                        )
                    }
                }
            }

            // Label section
            NoteLabelSection(
                labels = viewModel.labels,
                onAddLabel = viewModel::addLabel,
                onRemoveLabel = viewModel::removeLabel
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            // Title — borderless, large
            TextField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineSmall
                            .copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall
                    .copy(fontWeight = FontWeight.SemiBold),
                singleLine = true,
                colors = borderlessColors,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Image section — insert button always visible; displays attached images above it
            NoteImageSection(
                imageBlocks = viewModel.imageBlocks,
                onInsertImage = { galleryLauncher.launch("image/*") },
                onRemoveImage = viewModel::removeImage,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Content area — checklist or rich text editor
            if (viewModel.isChecklistMode) {
                NoteChecklistEditor(
                    items = viewModel.checklistItems,
                    onAddItem = viewModel::addChecklistItem,
                    onRemoveItem = viewModel::removeChecklistItem,
                    onToggleItem = viewModel::toggleChecklistItem,
                    onUpdateItemText = viewModel::updateChecklistItemText,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                RichTextEditor(
                    state = viewModel.richTextState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Shown when user tries to lock a note but device has no screen lock set up
    if (showNoPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showNoPasscodeDialog = false },
            title = { Text("No Device Passcode") },
            text = { Text("Your device has no screen lock set up. Please enable a PIN, pattern, or password in device settings to use note locking.") },
            confirmButton = {
                TextButton(onClick = {
                    showNoPasscodeDialog = false
                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }) { Text("Go to Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showNoPasscodeDialog = false }) { Text("Cancel") }
            }
        )
    }
}
