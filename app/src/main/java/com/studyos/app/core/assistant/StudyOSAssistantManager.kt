package com.studyos.app.core.assistant

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

data class AssistScreenContext(
    val textContent: String = "",
    val sourcePackage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object StudyOSAssistantManager {

    private val _assistContext = MutableStateFlow<AssistScreenContext?>(null)
    val assistContext: StateFlow<AssistScreenContext?> = _assistContext.asStateFlow()

    private var sessionFinisher: WeakReference<() -> Unit>? = null

    fun updateAssistContext(context: AssistScreenContext?) {
        _assistContext.value = context
    }

    fun clearAssistContext() {
        _assistContext.value = null
    }

    fun registerSessionFinisher(finisher: () -> Unit) {
        sessionFinisher = WeakReference(finisher)
    }

    fun unregisterSessionFinisher() {
        sessionFinisher = null
    }

    fun notifyOverlayClosed() {
        sessionFinisher?.get()?.invoke()
        sessionFinisher = null
        clearAssistContext()
    }

    /**
     * Checks if StudyOS is selected as the default digital assistant in Android System Settings.
     */
    fun isDefaultAssistant(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                        return true
                    }
                }
            }

            val defaultAssistant = Settings.Secure.getString(
                context.contentResolver,
                "assistant"
            )
            val myPackage = context.packageName
            defaultAssistant != null && defaultAssistant.contains(myPackage)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens Android System Default Apps / Digital Assistant Settings screen.
     */
    fun openAssistantSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (fallbackEx: Exception) {
                try {
                    val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    true
                } catch (lastEx: Exception) {
                    false
                }
            }
        }
    }

    /**
     * Opens Gesture / Power button settings if available on the device.
     */
    fun openPowerButtonGestureSettings(context: Context): Boolean {
        val gestureIntents = listOf(
            Intent("android.settings.GESTURE_SETTINGS"),
            Intent("android.settings.SYSTEM_GESTURES_SETTINGS"),
            Intent("com.android.settings.GESTURE_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in gestureIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (ignored: Exception) {
            }
        }
        return false
    }
}
