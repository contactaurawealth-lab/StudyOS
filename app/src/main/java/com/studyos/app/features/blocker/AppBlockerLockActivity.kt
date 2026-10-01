package com.studyos.app.features.blocker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.MainActivity
import com.studyos.app.StudyOSApplication
import com.studyos.app.core.blocker.AppBlockerManager
import com.studyos.app.core.model.AppState
import com.studyos.app.core.ui.component.GlassCard
import com.studyos.app.core.ui.component.StudyOSButton
import com.studyos.app.core.ui.component.StudyOSOutlinedButton
import com.studyos.app.theme.StudyOSTheme

class AppBlockerLockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""

        // Prevent bypassing lock by pressing system back button — redirect to home launcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        })

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(targetPackage, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            targetPackage.ifBlank { "Distracting App" }
        }

        val container = (application as StudyOSApplication).container

        setContent {
            val appState by container.preferencesDataSource.appState.collectAsState(
                initial = AppState(isLoading = false)
            )

            StudyOSTheme(appTheme = appState.theme) {
                LockScreenContent(
                    appName = appName,
                    packageName = targetPackage,
                    onUnlockSuccess = {
                        AppBlockerManager.unlockForFiveMinutes(targetPackage)
                        Toast.makeText(
                            this,
                            "$appName unblocked for 5 minutes",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    },
                    onExitToStudy = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun LockScreenContent(
    appName: String,
    packageName: String,
    onUnlockSuccess: () -> Unit,
    onExitToStudy: () -> Unit
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isPasscodeRequired = AppBlockerManager.isPasscodeSet()

    fun attemptUnlock() {
        if (!isPasscodeRequired) {
            onUnlockSuccess()
        } else if (AppBlockerManager.verifyPasscode(passwordInput)) {
            errorMessage = null
            onUnlockSuccess()
        } else {
            errorMessage = "Incorrect passcode. Try again."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock icon badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.15f))
                    .border(1.dp, colors.accent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Focus Mode Active",
                style = typography.screenTitle.copy(fontSize = 24.sp),
                color = colors.primaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$appName is blocked while StudyOS is active.",
                style = typography.body,
                color = colors.secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 20.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isPasscodeRequired) {
                        Text(
                            text = "Emergency 5-Minute Access",
                            style = typography.subsectionTitle,
                            color = colors.primaryText
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Enter your passcode to unlock for 5 minutes:",
                            style = typography.caption,
                            color = colors.secondaryText
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                errorMessage = null
                            },
                            placeholder = { Text("Passcode", color = colors.mutedText) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { attemptUnlock() }),
                            isError = errorMessage != null,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                errorBorderColor = colors.accent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage!!,
                                style = typography.caption,
                                color = colors.accent
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        StudyOSButton(
                            text = "Unlock for 5 Minutes",
                            onClick = { attemptUnlock() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Take a 5-Minute Break?",
                            style = typography.subsectionTitle,
                            color = colors.primaryText
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Unlock $appName temporarily. It will automatically lock again in 5 minutes.",
                            style = typography.caption,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        StudyOSButton(
                            text = "Unlock for 5 Minutes",
                            onClick = { onUnlockSuccess() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    StudyOSOutlinedButton(
                        text = "Return to StudyOS",
                        onClick = onExitToStudy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
