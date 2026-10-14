package com.studyos.app.core.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.studyos.app.StudyOSApplication
import com.studyos.app.core.blocker.AppBlockerManager
import com.studyos.app.features.blocker.AppBlockerLockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = applicationContext as? StudyOSApplication
        app?.let {
            CoroutineScope(Dispatchers.IO).launch {
                it.container.preferencesDataSource.syncAppBlockerManager()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip StudyOS itself, system packages, or blank
        if (packageName.isBlank()) return
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui") return

        if (AppBlockerManager.isAppBlocked(packageName)) {
            val now = SystemClock.uptimeMillis()
            if (lastBlockedPackage == packageName && (now - lastBlockTimestamp) < 1000L) {
                // Debounce rapid window state changes from the same blocked app
                return
            }
            lastBlockedPackage = packageName
            lastBlockTimestamp = now

            // 1. Immediately drop the distracting app to background via HOME action
            // This guarantees the user is pulled out of the distracting app even on Android 10+
            // when background activity launches may be deferred or blocked by OEM restrictions.
            performGlobalAction(GLOBAL_ACTION_HOME)

            // 2. Launch the lock overlay activity
            val intent = Intent(this, AppBlockerLockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                putExtra(AppBlockerLockActivity.EXTRA_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
