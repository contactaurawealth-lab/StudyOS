# StudyOS Comprehensive Codebase Audit Report

> **Auditor**: Senior Software Architect & Security Systems Analyst  
> **Audited Workspace**: `/root/StudyOS`  
> **Commit Inspected**: `1364b94` (Targeting Release v1.6.0)  
> **Mode**: MODE B (Audit and Propose)  
> **Date**: October 2026

---

## 1. Executive Summary

A comprehensive architectural, security, data integrity, and quality assurance audit of the StudyOS Android application was conducted across its entire codebase (119 Kotlin source files, 34 Room entities, 14 viewmodels, 28 UI screens, and 28 test suites).

StudyOS exhibits exceptional software engineering rigor in its core domain: 100% offline data sovereignty, zero cloud tracking, deterministic quiz generation, and a suite of 129 passing unit tests. However, deep forensic inspection identified critical security exposures, data integrity loopholes, and test-versus-reality discrepancies that present tangible risks to user data safety and system resilience.

---

## 2. Codebase Inventory & Metrics

| Metric | Measured Value | Notes |
| :--- | :--- | :--- |
| **Primary Language** | Kotlin 1.9.22 | JVM Target 17 |
| **UI Toolkit** | Jetpack Compose (Compiler 1.5.8) | Material3 Design Tokens |
| **Local Storage** | Room SQLite 2.6.1 | Schema Version 11, 34 Entities |
| **Preferences Storage** | Jetpack DataStore Preferences 1.0.0 | Protobuf-free Key-Value store |
| **Total Production Kotlin Files** | 91 files | `app/src/main/java` |
| **Total Test Files** | 28 files | `app/src/test/java` |
| **Total Unit Tests Executed** | 129 tests | 100% Passed (0 failures, 0 ignored) |
| **Target / Min SDK** | Android 15 (API 34) / Android 8.0 (API 26) | Full backward compatibility |

---

## 3. Prioritized Finding Registry

| ID | Title | Category | Severity | Confidence | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SEC-01** | Permissive Network Security Config & Cleartext Exposure | Security | **CRITICAL** | High (Confirmed) | Confirmed Defect |
| **DAT-01** | Catastrophic Room Data Loss Risk via `fallbackToDestructiveMigration` | Data Integrity | **CRITICAL** | High (Confirmed) | Confirmed Defect |
| **LOG-01** | Agent Empty/Substring Match Permitting Arbitrary Deletion | Business Logic | **HIGH** | High (Confirmed) | Confirmed Defect |
| **SEC-02** | Unencrypted AI API Key Storage & `allowBackup=true` Leakage | Security | **HIGH** | High (Confirmed) | Confirmed Defect |
| **TST-01** | Backup Encryption Test Discrepancy vs Plaintext Implementation | Testing & QA | **HIGH** | High (Confirmed) | Confirmed Defect |
| **DAT-02** | Foreign Key Violation & Cascade Risk during Backup Restore | Data Integrity | **MEDIUM** | High (Confirmed) | Confirmed Defect |
| **LOG-02** | Feynman Oral Walk Heuristic Length Fallback False Praise | Business Logic | **MEDIUM** | High (Confirmed) | Confirmed Defect |
| **SYS-01** | Background Activity Launch Restrictions in AppBlocker on Android 10+ | OS Integration | **MEDIUM** | Medium (Suspected) | Architecture Risk |

---

## 4. Deep Forensic Finding Cards

---

### Finding SEC-01: Permissive Network Security Config & Cleartext Exposure
- **Severity**: **CRITICAL**
- **Category**: Security / Transport Layer
- **Location**: [`app/src/main/res/xml/network_security_config.xml#L1-L9`](file:///root/StudyOS/app/src/main/res/xml/network_security_config.xml#L1-L9) and [`AndroidManifest.xml#L38-L40`](file:///root/StudyOS/app/src/main/AndroidManifest.xml#L38-L40)
- **Root Cause**:
  ```xml
  <!-- network_security_config.xml -->
  <network-security-config>
      <base-config cleartextTrafficPermitted="true">
          <trust-anchors>
              <certificates src="system" />
              <certificates src="user" />
          </trust-anchors>
      </base-config>
  </network-security-config>
  ```
  The `<base-config>` element applies globally to all network calls in all build types (including release). Setting `cleartextTrafficPermitted="true"` permits unencrypted HTTP traffic, while `<certificates src="user" />` trusts user-installed Certificate Authority (CA) root certificates in production.
- **Real-World Impact**:
  1. An attacker on an open Wi-Fi network (or a malicious proxy) can intercept any unencrypted HTTP traffic.
  2. Any user certificate installed on a compromised device can decrypt, intercept, or tamper with the user's private OpenAI/Gemini requests, exposing API keys and sensitive academic notes.
- **Remediation**:
  Enforce `cleartextTrafficPermitted="false"` by default in `base-config`, limit user certificates strictly to `<debug-overrides>`, and disable `android:usesCleartextTraffic` in `AndroidManifest.xml`.

---

### Finding DAT-01: Catastrophic Room Data Loss Risk via `fallbackToDestructiveMigration`
- **Severity**: **CRITICAL**
- **Category**: Data Integrity / Database
- **Location**: [`app/src/main/java/com/studyos/app/core/database/StudyOSDatabase.kt#L756`](file:///root/StudyOS/app/src/main/java/com/studyos/app/core/database/StudyOSDatabase.kt#L756)
- **Root Cause**:
  ```kotlin
  val instance = Room.databaseBuilder(...)
      .addMigrations(MIGRATION_1_2, MIGRATION_2_3, ..., MIGRATION_10_11)
      .fallbackToDestructiveMigration()
      .build()
  ```
  If a student upgrades the application and any schema divergence, missing migration step, or unhandled column mismatch occurs (e.g., from an older version or third-party fork), Room will silently drop and recreate all 34 database tables without warning, wiping out months of student notes, flashcards, and exam records.
- **Real-World Impact**: Irreversible total data wipe for users experiencing an edge-case migration failure.
- **Remediation**:
  1. Remove `.fallbackToDestructiveMigration()`.
  2. Implement an automated pre-migration SQLite backup routine that duplicates `studyos_database.db` to a `.db.bak` file before Room migration executes.
  3. If migration fails, catch the migration exception, restore the backup, and prompt the user to export their data safely rather than dropping tables.

---

### Finding LOG-01: Agent Empty/Substring Match Permitting Arbitrary Deletion
- **Severity**: **HIGH**
- **Category**: Business Logic / Data Safety
- **Location**: [`app/src/main/java/com/studyos/app/core/agent/StudyOSAgentActionExecutor.kt#L250-L308`](file:///root/StudyOS/app/src/main/java/com/studyos/app/core/agent/StudyOSAgentActionExecutor.kt#L250-L308)
- **Root Cause**:
  ```kotlin
  private suspend fun deleteNote(titleQuery: String): AgentExecutionResult {
      val allNotes = noteDao.observeAllNotes().firstOrNull() ?: emptyList()
      val target = allNotes.firstOrNull { it.title.contains(titleQuery, ignoreCase = true) || it.id == titleQuery }
          ?: return AgentExecutionResult(false, "DELETE_NOTE", "Could not find note \"$titleQuery\" to delete.")
      noteDao.deleteById(target.id)
      ...
  }
  ```
  If an LLM or prompt generates a deletion action with an empty string or whitespace (e.g. `{"action": "DELETE_NOTE", "title": ""}`), `title.contains("", ignoreCase = true)` evaluates to `true` for every note. Consequently, `firstOrNull` matches the first note in the user's database and deletes it permanently. The identical flaw exists in `deleteFlashcard`, `deleteTask`, and `deleteExam`.
- **Real-World Impact**: Accidental deletion of arbitrary user records whenever an LLM returns a malformed action or short substring.
- **Remediation**:
  Strictly require `titleQuery.trim().length >= 3` before executing search matching, disallow pure substring matching for deletion (require exact match or user confirmation dialog), and reject blank queries immediately.

---

### Finding SEC-02: Unencrypted AI API Key Storage & `allowBackup=true` Leakage
- **Severity**: **HIGH**
- **Category**: Security / Insecure Storage
- **Location**: [`app/src/main/java/com/studyos/app/core/datastore/StudyOSPreferencesDataSource.kt#L203`](file:///root/StudyOS/app/src/main/java/com/studyos/app/core/datastore/StudyOSPreferencesDataSource.kt#L203) and [`AndroidManifest.xml#L32`](file:///root/StudyOS/app/src/main/AndroidManifest.xml#L32)
- **Root Cause**:
  AI API keys (OpenAI, Gemini, OpenRouter) are saved directly in standard Jetpack DataStore files (`PreferencesKeys.AI_API_KEY`) as plaintext strings. Furthermore, `android:allowBackup="true"` is enabled without a custom `<full-backup-content>` XML rule.
- **Real-World Impact**: Anyone with ADB access (or physical access with USB debugging) can run `adb backup` or inspect the local XML/datastore files to extract user API keys without root privileges.
- **Remediation**:
  1. Introduce `EncryptedSharedPreferences` backed by the Android Keystore (`MasterKey`) for sensitive credentials such as AI API keys and blocker passcodes.
  2. Configure `<full-backup-content>` to exclude sensitive preferences and encryption keys from adb backup.

---

### Finding TST-01: Backup Encryption Test Discrepancy vs Plaintext Implementation
- **Severity**: **HIGH**
- **Category**: Testing & QA / Specification Mismatch
- **Location**: [`app/src/test/java/com/studyos/app/core/backup/BackupEncryptionTest.kt`](file:///root/StudyOS/app/src/test/java/com/studyos/app/core/backup/BackupEncryptionTest.kt) vs [`app/src/main/java/com/studyos/app/core/backup/BackupManager.kt#L103-L110`](file:///root/StudyOS/app/src/main/java/com/studyos/app/core/backup/BackupManager.kt#L103-L110)
- **Root Cause**:
  `BackupEncryptionTest.kt` passes unit tests verifying AES-256-GCM cipher round-trips and tampering detection. However, `BackupManager.kt` actually exports the database as unencrypted plaintext JSON (`StudyOS_Backup_$timestamp.json`) directly to disk:
  ```kotlin
  FileOutputStream(backupFile).use { fos ->
      fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
  }
  ```
  The documentation and tests claim air-gapped AES-256-GCM encrypted backups, while the production code writes raw unencrypted JSON.
- **Real-World Impact**: False sense of security for end-users who expect password-protected encrypted backups. Any exported file is exposed in plaintext.
- **Remediation**:
  Integrate the AES-256-GCM encryption routine from the test directly into `BackupManager.kt` with an optional passphrase prompt.

---

### Finding DAT-02: Foreign Key Violation & Cascade Risk during Backup Restore
- **Severity**: **MEDIUM**
- **Category**: Data Integrity
- **Location**: [`app/src/main/java/com/studyos/app/core/backup/BackupManager.kt#L140-L176`](file:///root/StudyOS/app/src/main/java/com/studyos/app/core/backup/BackupManager.kt#L140-L176)
- **Root Cause**:
  `BackupManager.restoreBackup` performs inserts using `CONFLICT_REPLACE` inside a transaction while `PRAGMA foreign_keys = ON` is active. In SQLite, `REPLACE` acts as a `DELETE` followed by an `INSERT`. If child records reference a replaced parent row before the child rows are processed, SQLite throws a foreign key constraint violation. Furthermore, `PRAGMA foreign_keys = OFF` is never temporarily set during batch restore.
- **Real-World Impact**: Backup restoration can abort mid-process on complex interdependent tables (e.g. `papers` <-> `paper_questions` <-> `question_bank`), leaving the database partially restored.
- **Remediation**:
  Execute `sdb.execSQL("PRAGMA foreign_keys = OFF;")` immediately prior to batch restore, and re-enable it with `PRAGMA foreign_keys = ON;` followed by `DatabaseIntegrityManager.checkIntegrity(sdb)` after transaction completion.

---

### Finding LOG-02: Feynman Oral Walk Heuristic Length Fallback False Praise
- **Severity**: **MEDIUM**
- **Category**: Business Logic / Pedagogical Accuracy
- **Location**: [`app/src/main/java/com/studyos/app/features/practice/audiowalk/FeynmanAudioWalkViewModel.kt#L400-L405`](file:///root/StudyOS/app/src/main/java/com/studyos/app/features/practice/audiowalk/FeynmanAudioWalkViewModel.kt#L400-L405)
- **Root Cause**:
  When AI is unavailable or offline, answer feedback relies purely on string length:
  ```kotlin
  feedback = if (studentAnswer.length > 20) {
      "Spot on! Great articulation of the core principles. That's a solid explanation."
  } else {
      "Good start! Remember to connect this directly back to the underlying mechanism..."
  }
  ```
- **Real-World Impact**: A student who says "I have absolutely no idea what the answer is" (> 20 chars) receives enthusiastic praise ("Spot on! Great articulation..."), reinforcing incorrect conceptual understanding.
- **Remediation**:
  Perform keyword intersection matching against `question.expectedConcept` before giving affirmative feedback, or provide neutral Socratic encouragement when offline rather than false confirmation.

---

### Finding SYS-01: Background Activity Launch Restrictions in AppBlocker on Android 10+
- **Severity**: **MEDIUM**
- **Category**: OS Integration & Reliability
- **Location**: [`app/src/main/java/com/studyos/app/core/service/AppBlockerAccessibilityService.kt#L22-L29`](file:///root/StudyOS/app/src/main/java/com/studyos/app/core/service/AppBlockerAccessibilityService.kt#L22-L29)
- **Root Cause**:
  The accessibility service calls `startActivity(intent)` when a blocked package is focused. Starting an activity from the background on Android 10+ (API 29+) is restricted unless the app has the `SYSTEM_ALERT_WINDOW` permission or uses a high-priority notification with a full-screen intent.
- **Real-World Impact**: On certain Android 10+ OEM devices (e.g. Xiaomi MIUI, Samsung OneUI), the blocker screen may fail to pop up immediately when the user launches a blocked app.
- **Remediation**:
  Request the `SYSTEM_ALERT_WINDOW` permission or provide a fallback notification overlay when full-screen intent launches are blocked by the OS.
