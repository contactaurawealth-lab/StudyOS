package com.studyos.app.features.blocker

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.GlassDialog
import com.studyos.app.core.ui.component.GlassTopBar
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSIconButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.theme.StudyOSTheme

@Composable
fun AppBlockerScreen(
    viewModel: AppBlockerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val context = LocalContext.current

    var showPasscodeDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.checkAccessibilityPermission()
        onDispose { }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlassTopBar(
            title = "Focus App Blocker",
            subtitle = if (state.isBlockerEnabled) "${state.blockedCount} apps restricted" else "Inactive",
            navigationIcon = {
                StudyOSIconButton(
                    onClick = onNavigateBack,
                    contentDescription = "Back"
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = colors.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master Blocker Switch
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Blocker Active",
                                style = typography.subsectionTitle,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Block distracting apps to maintain deep focus sessions",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }

                        Switch(
                            checked = state.isBlockerEnabled,
                            onCheckedChange = { viewModel.toggleBlockerEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.background,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.mutedText,
                                uncheckedTrackColor = colors.surface
                            )
                        )
                    }
                }
            }

            // Accessibility Permission Card
            if (!state.isAccessibilityEnabled) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.accent.copy(alpha = 0.08f)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Accessibility Service Required",
                                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = "StudyOS needs accessibility permission to detect when blocked apps are opened.",
                                        style = typography.caption,
                                        color = colors.secondaryText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            StudyOSButton(
                                text = "Enable in System Settings",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Passcode Setup Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emergency Unlock Passcode",
                                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (state.passcode.isNotBlank()) "Passcode is active (unlocks for 5 mins)" else "No passcode set (anyone can unlock for 5 mins)",
                                style = typography.caption,
                                color = colors.secondaryText
                            )
                        }

                        StudyOSOutlinedButton(
                            text = if (state.passcode.isNotBlank()) "Change" else "Set Code",
                            onClick = { showPasscodeDialog = true }
                        )
                    }
                }
            }

            // Search Bar & Apps Header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Installed Apps (${state.blockedCount} blocked)",
                        style = typography.sectionTitle.copy(fontSize = 17.sp),
                        color = colors.primaryText
                    )

                    if (state.blockedCount > 0) {
                        Text(
                            text = "Unblock All",
                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { viewModel.clearAllBlocked() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search apps...", color = colors.mutedText) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, null, tint = colors.mutedText, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.primaryText,
                        unfocusedTextColor = colors.primaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Apps List
            if (state.isLoadingApps) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            } else if (state.filteredApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotBlank()) "No apps found matching '${state.searchQuery}'" else "No launchable apps found.",
                            style = typography.body,
                            color = colors.secondaryText
                        )
                    }
                }
            } else {
                items(state.filteredApps, key = { it.packageName }) { app ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.togglePackageBlocked(app.packageName) },
                        padding = 10.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (app.isBlocked) colors.accent.copy(alpha = 0.2f) else colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (app.isBlocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    tint = if (app.isBlocked) colors.accent else colors.mutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = typography.body.copy(fontWeight = FontWeight.Medium),
                                    color = colors.primaryText
                                )
                                Text(
                                    text = app.packageName,
                                    style = typography.caption.copy(fontSize = 11.sp),
                                    color = colors.mutedText
                                )
                            }

                            Checkbox(
                                checked = app.isBlocked,
                                onCheckedChange = { viewModel.togglePackageBlocked(app.packageName) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.accent,
                                    uncheckedColor = colors.border,
                                    checkmarkColor = colors.background
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPasscodeDialog) {
        var newCode by remember { mutableStateOf(state.passcode) }
        GlassDialog(
            onDismissRequest = { showPasscodeDialog = false },
            title = "Set Emergency Passcode",
            message = "Enter a passcode required to temporarily unlock blocked apps for 5 minutes during a study session:",
            content = {
                OutlinedTextField(
                    value = newCode,
                    onValueChange = { newCode = it },
                    placeholder = { Text("Leave blank to remove passcode", color = colors.mutedText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButtonText = "Save",
            onConfirm = {
                viewModel.setPasscode(newCode)
                showPasscodeDialog = false
            },
            dismissButtonText = "Cancel"
        )
    }
}
