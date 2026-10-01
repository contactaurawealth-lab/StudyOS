package com.studyos.app.features.blocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyos.app.core.datastore.PreferencesDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BlockableAppItem(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean
)

data class AppBlockerUiState(
    val isBlockerEnabled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val passcode: String = "",
    val blockedCount: Int = 0,
    val allApps: List<BlockableAppItem> = emptyList(),
    val filteredApps: List<BlockableAppItem> = emptyList(),
    val searchQuery: String = "",
    val isLoadingApps: Boolean = true
)

class AppBlockerViewModel(
    private val context: Context,
    private val preferencesDataSource: PreferencesDataSource
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _installedApps = MutableStateFlow<List<BlockableAppItem>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _isAccessibilityEnabled = MutableStateFlow(false)

    private data class BlockerPrefs(
        val enabled: Boolean,
        val blockedPackages: Set<String>,
        val passcode: String
    )

    private val _prefsFlow = combine(
        preferencesDataSource.appBlockerEnabled,
        preferencesDataSource.blockedPackages,
        preferencesDataSource.appBlockerPasscode
    ) { enabled, blockedSet, passcode ->
        BlockerPrefs(enabled, blockedSet, passcode)
    }

    val uiState: StateFlow<AppBlockerUiState> = combine(
        _prefsFlow,
        _installedApps,
        _searchQuery,
        _isLoadingApps,
        _isAccessibilityEnabled
    ) { prefs, installed, query, isLoading, a11yEnabled ->
        val appsWithBlockedStatus = installed.map { app ->
            app.copy(isBlocked = prefs.blockedPackages.contains(app.packageName))
        }

        val filtered = if (query.isBlank()) {
            appsWithBlockedStatus
        } else {
            appsWithBlockedStatus.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }

        AppBlockerUiState(
            isBlockerEnabled = prefs.enabled,
            isAccessibilityEnabled = a11yEnabled,
            passcode = prefs.passcode,
            blockedCount = prefs.blockedPackages.size,
            allApps = appsWithBlockedStatus,
            filteredApps = filtered,
            searchQuery = query,
            isLoadingApps = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppBlockerUiState()
    )

    init {
        loadInstalledApps()
        checkAccessibilityPermission()
    }

    fun checkAccessibilityPermission() {
        _isAccessibilityEnabled.value = isAccessibilityServiceEnabled(context)
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val myPackage = context.packageName

                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                resolveInfos
                    .filter { it.activityInfo.packageName != myPackage }
                    .distinctBy { it.activityInfo.packageName }
                    .map { ri ->
                        BlockableAppItem(
                            packageName = ri.activityInfo.packageName,
                            appName = ri.loadLabel(pm).toString(),
                            isBlocked = false
                        )
                    }
                    .sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleBlockerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setAppBlockerEnabled(enabled)
        }
    }

    fun togglePackageBlocked(packageName: String) {
        viewModelScope.launch {
            val current = uiState.value.allApps
                .filter { it.isBlocked }
                .map { it.packageName }
                .toMutableSet()

            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            preferencesDataSource.setBlockedPackages(current)
        }
    }

    fun setPasscode(passcode: String) {
        viewModelScope.launch {
            preferencesDataSource.setAppBlockerPasscode(passcode.trim())
        }
    }

    fun clearAllBlocked() {
        viewModelScope.launch {
            preferencesDataSource.setBlockedPackages(emptySet())
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/com.studyos.app.core.service.AppBlockerAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any {
            it.equals(expectedServiceName, ignoreCase = true) ||
                    it.contains("AppBlockerAccessibilityService", ignoreCase = true)
        }
    }
}
