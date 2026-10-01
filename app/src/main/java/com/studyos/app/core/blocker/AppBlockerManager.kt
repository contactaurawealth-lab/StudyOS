package com.studyos.app.core.blocker

import java.util.concurrent.ConcurrentHashMap

object AppBlockerManager {
    // Stores packageName -> epoch millis timestamp until which the app is temporarily unlocked (5 min grace period)
    private val unlockedUntilMap = ConcurrentHashMap<String, Long>()

    @Volatile var isBlockerEnabled: Boolean = false
        private set

    @Volatile var blockedPackages: Set<String> = emptySet()
        private set

    @Volatile var passcode: String = ""
        private set

    const val UNLOCK_DURATION_MILLIS = 5 * 60 * 1000L // 5 minutes

    fun syncFromPreferences(enabled: Boolean, packages: Set<String>, code: String) {
        isBlockerEnabled = enabled
        blockedPackages = packages
        passcode = code
    }

    fun isAppBlocked(packageName: String): Boolean {
        if (!isBlockerEnabled) return false
        if (packageName.isBlank()) return false
        if (!blockedPackages.contains(packageName)) return false

        val unlockExpiry = unlockedUntilMap[packageName] ?: 0L
        val isGracePeriodActive = System.currentTimeMillis() < unlockExpiry
        return !isGracePeriodActive
    }

    fun unlockForFiveMinutes(packageName: String) {
        val expiryTime = System.currentTimeMillis() + UNLOCK_DURATION_MILLIS
        unlockedUntilMap[packageName] = expiryTime
    }

    fun getRemainingUnlockSeconds(packageName: String): Long {
        val expiryTime = unlockedUntilMap[packageName] ?: return 0L
        val diff = (expiryTime - System.currentTimeMillis()) / 1000L
        return diff.coerceAtLeast(0L)
    }

    fun isPasscodeSet(): Boolean = passcode.isNotBlank()

    fun verifyPasscode(input: String): Boolean {
        if (passcode.isBlank()) return true
        return passcode == input.trim()
    }

    fun clearUnlock(packageName: String) {
        unlockedUntilMap.remove(packageName)
    }

    fun resetAllUnlocks() {
        unlockedUntilMap.clear()
    }
}
