package com.studyos.app.features.settings.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.studyos.app.core.model.AppTheme
import com.studyos.app.core.notification.StudyOSNotificationManager
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSDialog
import com.studyos.app.core.ui.component.StudyOSDivider
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSListItem
import com.studyos.app.core.ui.component.StudyOSSectionHeader
import com.studyos.app.features.settings.viewmodel.SettingsViewModel
import com.studyos.app.theme.StudyOSTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    pendingRestoreJson = json
                    showRestoreConfirmDialog = true
                }
            } catch (e: Exception) {
                // Handled gracefully
            }
        }
    }

    // Check system notification status
    LaunchedEffect(Unit) {
        val enabled = StudyOSNotificationManager.areNotificationsEnabled(context)
        viewModel.setSystemNotificationsEnabled(enabled)
    }

    // Permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setSystemNotificationsEnabled(true)
            viewModel.toggleStudyReminders(true)
        } else {
            viewModel.setSystemNotificationsEnabled(false)
        }
    }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotificationMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .widthIn(max = 680.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    StudyOSIconButton(
                        onClick = onBack,
                        contentDescription = "Back"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Settings",
                    style = typography.screenTitle,
                    color = colors.primaryText
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Academic Configuration Section
            StudyOSSectionHeader(
                title = "Academic setup"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
            ) {
                Column {
                    val studentName = uiState.student?.name ?: "Configure student"
                    val studentGrade = uiState.student?.classLevel ?: "Not set"
                    StudyOSListItem(
                        title = "Profile",
                        subtitle = "$studentName • $studentGrade",
                        onClick = onNavigateToProfile,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Subjects",
                        subtitle = "${uiState.subjects.size} subjects organized",
                        onClick = onNavigateToSubjects,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    StudyOSDivider()

                    val dailyGoal = uiState.preferences.dailyStudyGoalMinutes
                    val sessionLen = uiState.preferences.defaultSessionMinutes
                    StudyOSListItem(
                        title = "Study preferences",
                        subtitle = "$dailyGoal min daily goal • $sessionLen min sessions",
                        onClick = onNavigateToPreferences,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Notifications Section
            StudyOSSectionHeader(
                title = "Notifications",
                description = "Free local reminders that run entirely on your device."
            )

            // System notifications warning if disabled in Android settings
            if (!uiState.areSystemNotificationsEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.surface)
                        .background(colors.surface)
                        .border(1.dp, colors.accent.copy(alpha = 0.5f), shapes.surface)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Notifications are disabled",
                                style = typography.bodyMedium,
                                color = colors.primaryText
                            )
                        }
                        Text(
                            text = "Enable them in Android settings to receive study and revision reminders.",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                        StudyOSButton(
                            text = "Open settings",
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
            ) {
                Column {
                    // 1. Study reminders
                    NotificationToggleRow(
                        title = "Study reminders",
                        subtitle = "Alerts for planned study sessions",
                        checked = uiState.studyRemindersEnabled,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPermission) {
                                    viewModel.setShowPermissionRationaleDialog(true)
                                    return@NotificationToggleRow
                                }
                            }
                            viewModel.toggleStudyReminders(enable)
                        }
                    )

                    StudyOSDivider()

                    // 2. Daily reminder
                    NotificationToggleRow(
                        title = "Daily reminder",
                        subtitle = "Daily check-in to plan and review your day",
                        checked = uiState.dailyReminderEnabled,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPermission) {
                                    viewModel.setShowPermissionRationaleDialog(true)
                                    return@NotificationToggleRow
                                }
                            }
                            viewModel.toggleDailyReminder(enable)
                        }
                    )

                    StudyOSDivider()

                    // 3. Reminder time
                    StudyOSListItem(
                        title = "Reminder time",
                        subtitle = formatTimeString(uiState.dailyReminderTime),
                        onClick = { showTimePickerDialog = true },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    StudyOSDivider()

                    // 4. Revision reminders
                    NotificationToggleRow(
                        title = "Revision reminders",
                        subtitle = "Spaced repetition and active recall prompts",
                        checked = uiState.revisionRemindersEnabled,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPermission) {
                                    viewModel.setShowPermissionRationaleDialog(true)
                                    return@NotificationToggleRow
                                }
                            }
                            viewModel.toggleRevisionReminders(enable)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Section
            StudyOSSectionHeader(
                title = "Appearance",
                description = "Choose your reading preference."
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
            ) {
                Column {
                    ThemeOptionRow(
                        title = "Light",
                        isSelected = uiState.currentTheme == AppTheme.LIGHT,
                        onClick = { viewModel.setTheme(AppTheme.LIGHT) }
                    )
                    StudyOSDivider()
                    ThemeOptionRow(
                        title = "Dark",
                        isSelected = uiState.currentTheme == AppTheme.DARK,
                        onClick = { viewModel.setTheme(AppTheme.DARK) }
                    )
                    StudyOSDivider()
                    ThemeOptionRow(
                        title = "System",
                        isSelected = uiState.currentTheme == AppTheme.SYSTEM,
                        onClick = { viewModel.setTheme(AppTheme.SYSTEM) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Data & Backup Section
            StudyOSSectionHeader(
                title = "Data & Backup",
                description = "Export and restore your offline data safely across devices."
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
            ) {
                Column {
                    StudyOSListItem(
                        title = "Export backup (JSON)",
                        subtitle = "Save all subjects, chapters, flashcards, and mistakes",
                        onClick = {
                            viewModel.exportBackup(context) { file ->
                                if (file != null) {
                                    try {
                                        val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, fileUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Save or Share StudyOS Backup"))
                                    } catch (e: Exception) {
                                        // Ignore or fallback
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    StudyOSDivider()

                    StudyOSListItem(
                        title = "Restore backup (JSON)",
                        subtitle = "Import and merge data from a previously saved JSON file",
                        onClick = {
                            filePickerLauncher.launch("application/json")
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = colors.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // About Section
            StudyOSSectionHeader(
                title = "About"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.surface)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shapes.surface)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "StudyOS",
                        style = typography.bodyMedium,
                        color = colors.primaryText
                    )
                    Text(
                        text = "Android Edition • 100% Offline-First",
                        style = typography.secondary,
                        color = colors.secondaryText
                    )
                    Text(
                        text = "Local-first study operating system • No internet, cloud or account required",
                        style = typography.caption,
                        color = colors.mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // Educational Pre-Permission Dialog (Android 13+)
    if (uiState.showPermissionRationaleDialog) {
        StudyOSDialog(
            onDismissRequest = { viewModel.setShowPermissionRationaleDialog(false) },
            title = "Stay on track",
            text = "StudyOS can remind you when it's time to study or revise. All reminders are 100% offline and stored locally on your phone.",
            confirmButtonText = "Enable reminders",
            onConfirm = {
                viewModel.setShowPermissionRationaleDialog(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            dismissButtonText = "Not now",
            onDismiss = { viewModel.setShowPermissionRationaleDialog(false) }
        )
    }

    // Reminder Time Picker Dialog
    if (showTimePickerDialog) {
        val timePresets = listOf(
            "07:00" to "7:00 AM",
            "08:00" to "8:00 AM",
            "09:00" to "9:00 AM",
            "12:00" to "12:00 PM",
            "16:00" to "4:00 PM",
            "17:00" to "5:00 PM",
            "18:00" to "6:00 PM",
            "19:00" to "7:00 PM (Default)",
            "20:00" to "8:00 PM",
            "21:00" to "9:00 PM",
            "22:00" to "10:00 PM"
        )

        StudyOSDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = "Choose reminder time",
            confirmButtonText = "Close",
            onConfirm = { showTimePickerDialog = false },
            dismissButtonText = null,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    timePresets.forEach { (timeValue, label) ->
                        val isSelected = uiState.dailyReminderTime == timeValue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDailyReminderTime(timeValue)
                                    showTimePickerDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = if (isSelected) typography.bodyMedium else typography.body,
                                color = if (isSelected) colors.accent else colors.primaryText,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    if (showRestoreConfirmDialog && pendingRestoreJson != null) {
        StudyOSDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreJson = null
            },
            title = "Restore Backup",
            text = "Restoring will import and merge subjects, chapters, notes, flashcards, quizzes, and mistakes from your backup file into your local database. Do you want to proceed?",
            confirmButtonText = "Restore Data",
            onConfirm = {
                val json = pendingRestoreJson
                showRestoreConfirmDialog = false
                pendingRestoreJson = null
                if (json != null) {
                    viewModel.restoreBackup(json)
                }
            },
            dismissButtonText = "Cancel",
            onDismiss = {
                showRestoreConfirmDialog = false
                pendingRestoreJson = null
            }
        )
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodyMedium,
                color = colors.primaryText
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = typography.caption,
                    color = colors.mutedText
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.surface,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.mutedText,
                uncheckedTrackColor = colors.surface,
                uncheckedBorderColor = colors.border
            )
        )
    }
}

private fun formatTimeString(timeStr: String): String {
    val parts = timeStr.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 19
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val period = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", displayHour, minute, period)
}

@Composable
private fun ThemeOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                this.role = Role.RadioButton
                this.selected = isSelected
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = if (isSelected) typography.bodyMedium else typography.body,
            color = colors.primaryText,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
