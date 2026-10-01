package com.studyos.app.core.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.studyos.app.core.blocker.AppBlockerManager
import com.studyos.app.features.blocker.AppBlockerLockActivity

class AppBlockerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip StudyOS itself, system packages, or blank
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui") return

        if (AppBlockerManager.isAppBlocked(packageName)) {
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
