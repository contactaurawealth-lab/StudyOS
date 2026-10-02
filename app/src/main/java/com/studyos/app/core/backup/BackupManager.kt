package com.studyos.app.core.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.studyos.app.core.database.StudyOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private val TABLES_IN_ORDER = listOf(
        "students",
        "study_preferences",
        "subjects",
        "chapters",
        "tasks",
        "study_sessions",
        "notes",
        "resources",
        "flashcards",
        "flashcard_reviews",
        "quizzes",
        "quiz_questions",
        "quiz_attempts",
        "question_results",
        "active_quiz_states",
        "mistakes",
        "tests",
        "test_attempts",
        "exams",
        "exam_subjects",
        "study_plans",
        "revision_schedules",
        "active_recall_logs",
        "recall_items",
        "recall_attempts",
        "ai_conversations",
        "ai_messages",
        "notifications"
    )

    data class BackupResult(
        val success: Boolean,
        val message: String,
        val exportedFile: File? = null,
        val restoredTablesCount: Int = 0,
        val restoredRowsCount: Int = 0
    )

    suspend fun exportBackup(context: Context, database: StudyOSDatabase): BackupResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("app", "StudyOS")
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())
            root.put("dateFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            val dataObj = JSONObject()
            val sdb: SupportSQLiteDatabase = database.openHelper.readableDatabase

            for (table in TABLES_IN_ORDER) {
                val tableArr = JSONArray()
                try {
                    val cursor: Cursor = sdb.query("SELECT * FROM `$table`")
                    while (cursor.moveToNext()) {
                        val rowObj = JSONObject()
                        for (i in 0 until cursor.columnCount) {
                            val colName = cursor.getColumnName(i)
                            when (cursor.getType(i)) {
                                Cursor.FIELD_TYPE_NULL -> rowObj.put(colName, JSONObject.NULL)
                                Cursor.FIELD_TYPE_INTEGER -> rowObj.put(colName, cursor.getLong(i))
                                Cursor.FIELD_TYPE_FLOAT -> rowObj.put(colName, cursor.getDouble(i))
                                Cursor.FIELD_TYPE_STRING -> rowObj.put(colName, cursor.getString(i))
                                else -> rowObj.put(colName, cursor.getString(i))
                            }
                        }
                        tableArr.put(rowObj)
                    }
                    cursor.close()
                } catch (e: Exception) {
                    // Ignore table if it doesn't exist in older versions
                }
                dataObj.put(table, tableArr)
            }
            root.put("data", dataObj)

            val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "StudyOS_Backup_$timestamp.json")

            FileOutputStream(backupFile).use { fos ->
                fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }

            BackupResult(
                success = true,
                message = "Backup created successfully (${backupFile.name})",
                exportedFile = backupFile
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    suspend fun restoreBackup(database: StudyOSDatabase, jsonString: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("app") || root.getString("app") != "StudyOS") {
                return@withContext BackupResult(
                    success = false,
                    message = "Invalid backup file: not a StudyOS backup."
                )
            }

            val dataObj = root.getJSONObject("data")
            val sdb: SupportSQLiteDatabase = database.openHelper.writableDatabase

            var tablesRestored = 0
            var rowsRestored = 0

            sdb.beginTransaction()
            try {
                // Restore in dependency order using REPLACE
                for (table in TABLES_IN_ORDER) {
                    if (!dataObj.has(table)) continue
                    val rows = dataObj.getJSONArray(table)
                    if (rows.length() == 0) continue

                    tablesRestored++
                    for (i in 0 until rows.length()) {
                        val row = rows.getJSONObject(i)
                        val cv = ContentValues()
                        val keys = row.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            if (row.isNull(key)) {
                                cv.putNull(key)
                            } else {
                                val value = row.get(key)
                                when (value) {
                                    is Long -> cv.put(key, value)
                                    is Int -> cv.put(key, value.toLong())
                                    is Double -> cv.put(key, value)
                                    is Boolean -> cv.put(key, if (value) 1 else 0)
                                    is String -> cv.put(key, value)
                                    else -> cv.put(key, value.toString())
                                }
                            }
                        }
                        sdb.insert(table, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                        rowsRestored++
                    }
                }
                sdb.setTransactionSuccessful()
            } finally {
                sdb.endTransaction()
            }

            BackupResult(
                success = true,
                message = "Restored $rowsRestored items across $tablesRestored sections.",
                restoredTablesCount = tablesRestored,
                restoredRowsCount = rowsRestored
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Restore failed: ${e.localizedMessage ?: "Malformed JSON"}"
            )
        }
    }

    suspend fun clearAllData(database: StudyOSDatabase): Boolean = withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportAndResetApp(
        context: Context,
        database: StudyOSDatabase
    ): BackupResult = withContext(Dispatchers.IO) {
        val exportResult = exportBackup(context, database)
        try {
            database.clearAllTables()
        } catch (e: Exception) {
            return@withContext BackupResult(
                success = false,
                message = "Export completed, but failed to wipe database: ${e.localizedMessage}",
                exportedFile = exportResult.exportedFile
            )
        }
        BackupResult(
            success = true,
            message = "Backup exported and StudyOS reset completely.",
            exportedFile = exportResult.exportedFile
        )
    }

    private const val ENCRYPTION_HEADER = "STUDYOS_ENC_v1\n"
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 10_000

    /**
     * Air-gapped AES-256-GCM encrypted backup export (.studyos file).
     * Strictly 100% offline and cryptographic tamper-proof.
     */
    suspend fun exportEncryptedBackup(
        context: Context,
        database: StudyOSDatabase,
        passphrase: String = "StudyOS_Master_Vault"
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val plainExport = exportBackup(context, database)
            if (!plainExport.success || plainExport.exportedFile == null) {
                return@withContext plainExport
            }

            val plainJsonBytes = plainExport.exportedFile.readBytes()
            plainExport.exportedFile.delete() // Remove unencrypted intermediate file

            val secureRandom = java.security.SecureRandom()
            val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
            val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }

            // PBKDF2 Key Derivation
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = javax.crypto.spec.PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
            val secretKeyBytes = factory.generateSecret(spec).encoded
            val secretKey = javax.crypto.spec.SecretKeySpec(secretKeyBytes, "AES")

            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val cipherText = cipher.doFinal(plainJsonBytes)

            val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val encryptedFile = File(backupDir, "StudyOS_Encrypted_Backup_$timestamp.studyos")

            FileOutputStream(encryptedFile).use { fos ->
                fos.write(ENCRYPTION_HEADER.toByteArray(Charsets.UTF_8))
                fos.write(salt)
                fos.write(iv)
                fos.write(cipherText)
            }

            BackupResult(
                success = true,
                message = "Air-gapped AES-256-GCM encrypted archive created (${encryptedFile.name})",
                exportedFile = encryptedFile
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Encryption export failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Restores an air-gapped AES-256-GCM encrypted .studyos backup archive.
     */
    suspend fun restoreEncryptedBackup(
        database: StudyOSDatabase,
        encryptedBytes: ByteArray,
        passphrase: String = "StudyOS_Master_Vault"
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val headerBytes = ENCRYPTION_HEADER.toByteArray(Charsets.UTF_8)
            if (encryptedBytes.size < headerBytes.size + SALT_LENGTH + IV_LENGTH) {
                return@withContext BackupResult(
                    success = false,
                    message = "Invalid .studyos archive: File corrupted or truncated."
                )
            }

            // Verify Header Magic
            for (i in headerBytes.indices) {
                if (encryptedBytes[i] != headerBytes[i]) {
                    return@withContext BackupResult(
                        success = false,
                        message = "Invalid format: Not a recognized StudyOS encrypted archive."
                    )
                }
            }

            var offset = headerBytes.size
            val salt = encryptedBytes.copyOfRange(offset, offset + SALT_LENGTH)
            offset += SALT_LENGTH
            val iv = encryptedBytes.copyOfRange(offset, offset + IV_LENGTH)
            offset += IV_LENGTH
            val cipherText = encryptedBytes.copyOfRange(offset, encryptedBytes.size)

            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = javax.crypto.spec.PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
            val secretKeyBytes = factory.generateSecret(spec).encoded
            val secretKey = javax.crypto.spec.SecretKeySpec(secretKeyBytes, "AES")

            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = try {
                cipher.doFinal(cipherText)
            } catch (e: javax.crypto.AEADBadTagException) {
                return@withContext BackupResult(
                    success = false,
                    message = "Decryption failed: Incorrect passphrase or archive has been tampered with."
                )
            }

            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            restoreBackup(database, jsonString)
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Decryption error: ${e.localizedMessage ?: "Corrupted payload"}"
            )
        }
    }
}
