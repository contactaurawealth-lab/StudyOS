package com.studyos.app.core.agent

import com.studyos.app.core.database.StudyOSDatabase
import com.studyos.app.core.database.entity.ExamEntity
import com.studyos.app.core.database.entity.FlashcardEntity
import com.studyos.app.core.database.entity.NoteEntity
import com.studyos.app.core.database.entity.StudySessionEntity
import com.studyos.app.core.database.entity.TaskEntity
import com.studyos.app.domain.model.StudySessionStatus
import com.studyos.app.domain.model.TaskPriority
import com.studyos.app.domain.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AgentExecutionResult(
    val success: Boolean,
    val actionType: String,
    val message: String,
    val entityTitle: String? = null,
    val details: String? = null
)

class StudyOSAgentActionExecutor(
    private val database: StudyOSDatabase
) {
    private val noteDao = database.noteDao()
    private val flashcardDao = database.flashcardDao()
    private val taskDao = database.taskDao()
    private val examDao = database.examDao()
    private val sessionDao = database.studySessionDao()
    private val subjectDao = database.subjectDao()

    suspend fun executeActionFromBlock(jsonString: String): AgentExecutionResult = withContext(Dispatchers.IO) {
        try {
            val cleanJson = jsonString.trim().removePrefix("```studyos_action").removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(cleanJson)
            val action = json.optString("action", json.optString("type")).uppercase(Locale.ROOT)
            val payload = if (json.has("payload")) json.getJSONObject("payload") else json

            when (action) {
                "CREATE_NOTE" -> {
                    val title = payload.optString("title", "AI Study Note").ifBlank { "AI Study Note" }
                    val content = payload.optString("content", "")
                    val isPinned = payload.optBoolean("isPinned", false)
                    createNote(title, content, isPinned)
                }
                "EDIT_NOTE" -> {
                    val titleQuery = payload.optString("titleQuery", payload.optString("title", ""))
                    val newTitle = payload.optString("newTitle", titleQuery).ifBlank { titleQuery }
                    val newContent = payload.optString("content", "")
                    val isPinned = if (payload.has("isPinned")) payload.optBoolean("isPinned") else null
                    editNote(titleQuery, newTitle, newContent, isPinned)
                }
                "DELETE_NOTE" -> {
                    val titleQuery = payload.optString("title", payload.optString("query", ""))
                    deleteNote(titleQuery)
                }
                "CREATE_FLASHCARD" -> {
                    val question = payload.optString("question", payload.optString("q", ""))
                    val answer = payload.optString("answer", payload.optString("a", ""))
                    createFlashcard(question, answer)
                }
                "DELETE_FLASHCARD" -> {
                    val questionQuery = payload.optString("question", payload.optString("query", ""))
                    deleteFlashcard(questionQuery)
                }
                "CREATE_TASK", "ADD_TASK", "CREATE_EVENT", "ADD_EVENT" -> {
                    val title = payload.optString("title", "New Task").ifBlank { "New Task" }
                    val description = payload.optString("description", "")
                    val dueInHours = payload.optInt("dueInHours", -1)
                    val priority = payload.optString("priority", "MEDIUM")
                    createTask(title, description, dueInHours, priority)
                }
                "COMPLETE_TASK" -> {
                    val titleQuery = payload.optString("title", payload.optString("query", ""))
                    completeTask(titleQuery)
                }
                "DELETE_TASK" -> {
                    val titleQuery = payload.optString("title", payload.optString("query", ""))
                    deleteTask(titleQuery)
                }
                "ADD_EXAM", "CREATE_EXAM" -> {
                    val name = payload.optString("name", payload.optString("title", "Upcoming Exam"))
                    val daysFromNow = payload.optInt("daysFromNow", 7)
                    val targetScore = if (payload.has("targetScore")) payload.optInt("targetScore") else null
                    addExam(name, daysFromNow, targetScore)
                }
                "DELETE_EXAM" -> {
                    val nameQuery = payload.optString("name", payload.optString("query", ""))
                    deleteExam(nameQuery)
                }
                "LOG_STUDY_SESSION", "START_SESSION" -> {
                    val title = payload.optString("title", "Study Session")
                    val minutes = payload.optInt("minutes", 25)
                    logStudySession(title, minutes)
                }
                else -> {
                    AgentExecutionResult(false, action, "Unknown agent action: $action")
                }
            }
        } catch (e: Exception) {
            AgentExecutionResult(false, "ERROR", "Failed to execute agent action: ${e.message}")
        }
    }

    /**
     * Checks if input contains natural language command intent or structured JSON block,
     * and executes it autonomously.
     */
    suspend fun tryExecuteIntent(input: String): AgentExecutionResult? = withContext(Dispatchers.IO) {
        val trimmed = input.trim()

        // 1. Check for structured block ```studyos_action
        val actionMarker = "```studyos_action"
        if (trimmed.contains(actionMarker)) {
            val startIdx = trimmed.indexOf(actionMarker)
            val endIdx = trimmed.indexOf("```", startIdx + actionMarker.length)
            if (endIdx != -1) {
                val block = trimmed.substring(startIdx, endIdx + 3)
                return@withContext executeActionFromBlock(block)
            }
        }

        // 2. High-precision natural language command patterns
        val lower = trimmed.lowercase(Locale.ROOT)

        // Note Creation: "create note", "add note", "take note", "new note"
        if (lower.startsWith("create note") || lower.startsWith("add note") || lower.startsWith("new note") || lower.startsWith("take note")) {
            val body = trimmed.substringAfter("note", "").trim().removePrefix(":").removePrefix("-").trim()
            val lines = body.lines()
            val title = lines.firstOrNull()?.trim() ?: "AI Study Note"
            val content = lines.drop(1).joinToString("\n").trim().ifBlank { title }
            return@withContext createNote(title, content, false)
        }

        // Note Deletion: "delete note", "remove note"
        if (lower.startsWith("delete note") || lower.startsWith("remove note")) {
            val query = trimmed.substringAfter("note", "").trim().removePrefix(":").trim()
            if (query.isNotBlank()) return@withContext deleteNote(query)
        }

        // Flashcard Creation: "create flashcard", "add flashcard", "new flashcard"
        if (lower.startsWith("create flashcard") || lower.startsWith("add flashcard") || lower.startsWith("new flashcard")) {
            val body = trimmed.substringAfter("flashcard", "").trim().removePrefix(":").trim()
            val q = when {
                body.contains("A:") -> body.substringBefore("A:").removePrefix("Q:").trim()
                body.contains(" - ") -> body.substringBefore(" - ").trim()
                body.contains("?") -> body.substringBefore("?") + "?"
                else -> body
            }
            val a = when {
                body.contains("A:") -> body.substringAfter("A:").trim()
                body.contains(" - ") -> body.substringAfter(" - ").trim()
                body.contains("?") -> body.substringAfter("?").trim()
                else -> "Key Concept"
            }
            if (q.isNotBlank()) return@withContext createFlashcard(q, a)
        }

        // Task Creation: "create task", "add task", "add event", "new task"
        if (lower.startsWith("create task") || lower.startsWith("add task") || lower.startsWith("add event") || lower.startsWith("new task")) {
            val body = trimmed.substringAfter("task", "").ifBlank { trimmed.substringAfter("event", "") }.trim().removePrefix(":").trim()
            if (body.isNotBlank()) {
                val dueHours = when {
                    lower.contains("tomorrow") -> 24
                    lower.contains("tonight") -> 6
                    lower.contains("in 2 hours") -> 2
                    else -> 12
                }
                return@withContext createTask(body, "", dueHours, "HIGH")
            }
        }

        // Complete Task: "complete task", "finish task", "done task"
        if (lower.startsWith("complete task") || lower.startsWith("finish task") || lower.startsWith("done task")) {
            val query = trimmed.substringAfter("task", "").trim().removePrefix(":").trim()
            if (query.isNotBlank()) return@withContext completeTask(query)
        }

        // Exam Scheduling: "add exam", "create exam", "schedule exam"
        if (lower.startsWith("add exam") || lower.startsWith("create exam") || lower.startsWith("schedule exam")) {
            val body = trimmed.substringAfter("exam", "").trim().removePrefix(":").trim()
            if (body.isNotBlank()) {
                val days = if (lower.contains("in ")) {
                    val part = lower.substringAfter("in ").substringBefore(" ").trim()
                    part.toIntOrNull() ?: 7
                } else 7
                return@withContext addExam(body, days, 90)
            }
        }

        // Study Session: "log session", "start session", "log study session"
        if (lower.startsWith("log session") || lower.startsWith("start session") || lower.startsWith("log study session")) {
            val body = trimmed.substringAfter("session", "").trim().removePrefix(":").trim()
            val mins = body.filter { it.isDigit() }.toIntOrNull() ?: 25
            val title = body.replace(Regex("\\d+"), "").replace("minutes", "").replace("min", "").trim().ifBlank { "Deep Work Focus" }
            return@withContext logStudySession(title, mins)
        }

        null
    }

    // --- Entity Mutations ---

    private suspend fun createNote(title: String, content: String, isPinned: Boolean): AgentExecutionResult {
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            content = content.trim(),
            isPinned = isPinned,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insert(note)
        return AgentExecutionResult(
            success = true,
            actionType = "CREATE_NOTE",
            message = "Successfully created note in your StudyOS notebook.",
            entityTitle = title
        )
    }

    private suspend fun editNote(titleQuery: String, newTitle: String, newContent: String, isPinned: Boolean?): AgentExecutionResult {
        val allNotes = noteDao.observeAllNotes().firstOrNull() ?: emptyList()
        val target = allNotes.firstOrNull { it.title.contains(titleQuery, ignoreCase = true) || it.id == titleQuery }
            ?: return AgentExecutionResult(false, "EDIT_NOTE", "Could not find a note matching \"$titleQuery\"")

        val updated = target.copy(
            title = newTitle.ifBlank { target.title },
            content = if (newContent.isNotBlank()) newContent else target.content,
            isPinned = isPinned ?: target.isPinned,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.update(updated)
        return AgentExecutionResult(
            success = true,
            actionType = "EDIT_NOTE",
            message = "Updated note: \"${updated.title}\"",
            entityTitle = updated.title
        )
    }

    private suspend fun deleteNote(titleQuery: String): AgentExecutionResult {
        val allNotes = noteDao.observeAllNotes().firstOrNull() ?: emptyList()
        val target = allNotes.firstOrNull { it.title.contains(titleQuery, ignoreCase = true) || it.id == titleQuery }
            ?: return AgentExecutionResult(false, "DELETE_NOTE", "Could not find note \"$titleQuery\" to delete.")

        noteDao.deleteById(target.id)
        return AgentExecutionResult(
            success = true,
            actionType = "DELETE_NOTE",
            message = "Deleted note: \"${target.title}\"",
            entityTitle = target.title
        )
    }

    private suspend fun createFlashcard(question: String, answer: String): AgentExecutionResult {
        val subjects = subjectDao.getAllSubjectsOnce()
        val defaultSubjectId = subjects.firstOrNull()?.id ?: run {
            // Auto-create a default general subject if none exists
            val sub = com.studyos.app.core.database.entity.SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = "General Knowledge",
                isCustom = true,
                createdAt = System.currentTimeMillis()
            )
            subjectDao.insert(sub)
            sub.id
        }

        val card = FlashcardEntity(
            id = UUID.randomUUID().toString(),
            subjectId = defaultSubjectId,
            question = question.trim(),
            answer = answer.trim(),
            difficulty = "MEDIUM",
            createdAt = System.currentTimeMillis()
        )
        flashcardDao.insert(card)
        return AgentExecutionResult(
            success = true,
            actionType = "CREATE_FLASHCARD",
            message = "Added new flashcard to deck.",
            entityTitle = question
        )
    }

    private suspend fun deleteFlashcard(questionQuery: String): AgentExecutionResult {
        val allCards = flashcardDao.observeAllFlashcards().firstOrNull() ?: emptyList()
        val target = allCards.firstOrNull { it.question.contains(questionQuery, ignoreCase = true) || it.id == questionQuery }
            ?: return AgentExecutionResult(false, "DELETE_FLASHCARD", "Could not find flashcard matching \"$questionQuery\"")

        flashcardDao.deleteById(target.id)
        return AgentExecutionResult(
            success = true,
            actionType = "DELETE_FLASHCARD",
            message = "Deleted flashcard: \"${target.question}\"",
            entityTitle = target.question
        )
    }

    private suspend fun createTask(title: String, description: String, dueInHours: Int, priorityStr: String): AgentExecutionResult {
        val priority = try {
            TaskPriority.valueOf(priorityStr.uppercase(Locale.ROOT))
        } catch (_: Exception) {
            TaskPriority.MEDIUM
        }

        val dueAt = if (dueInHours > 0) {
            System.currentTimeMillis() + (dueInHours * 3600 * 1000L)
        } else null

        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.ifBlank { null },
            dueAt = dueAt,
            priority = priority.name,
            status = TaskStatus.TODO.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.insert(task)
        return AgentExecutionResult(
            success = true,
            actionType = "CREATE_TASK",
            message = "Scheduled task: \"$title\"",
            entityTitle = title
        )
    }

    private suspend fun completeTask(titleQuery: String): AgentExecutionResult {
        val allTasks = taskDao.observeTasks().firstOrNull() ?: emptyList()
        val target = allTasks.firstOrNull { it.title.contains(titleQuery, ignoreCase = true) || it.id == titleQuery }
            ?: return AgentExecutionResult(false, "COMPLETE_TASK", "Could not find task matching \"$titleQuery\"")

        taskDao.updateStatus(target.id, TaskStatus.COMPLETED.name)
        return AgentExecutionResult(
            success = true,
            actionType = "COMPLETE_TASK",
            message = "Marked task as completed: \"${target.title}\"",
            entityTitle = target.title
        )
    }

    private suspend fun deleteTask(titleQuery: String): AgentExecutionResult {
        val allTasks = taskDao.observeTasks().firstOrNull() ?: emptyList()
        val target = allTasks.firstOrNull { it.title.contains(titleQuery, ignoreCase = true) || it.id == titleQuery }
            ?: return AgentExecutionResult(false, "DELETE_TASK", "Could not find task \"$titleQuery\" to delete.")

        taskDao.deleteById(target.id)
        return AgentExecutionResult(
            success = true,
            actionType = "DELETE_TASK",
            message = "Deleted task: \"${target.title}\"",
            entityTitle = target.title
        )
    }

    private suspend fun addExam(name: String, daysFromNow: Int, targetScore: Int?): AgentExecutionResult {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysFromNow.coerceAtLeast(1))
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }
        val exam = ExamEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            date = cal.timeInMillis,
            targetScore = targetScore,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        examDao.insertExam(exam)
        val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(cal.timeInMillis))
        return AgentExecutionResult(
            success = true,
            actionType = "ADD_EXAM",
            message = "Added exam \"$name\" scheduled for $formattedDate",
            entityTitle = name
        )
    }

    private suspend fun deleteExam(nameQuery: String): AgentExecutionResult {
        val exams = examDao.observeAllExams().firstOrNull() ?: emptyList()
        val target = exams.firstOrNull { it.name.contains(nameQuery, ignoreCase = true) || it.id == nameQuery }
            ?: return AgentExecutionResult(false, "DELETE_EXAM", "Could not find exam \"$nameQuery\" to delete.")

        examDao.deleteExamById(target.id)
        return AgentExecutionResult(
            success = true,
            actionType = "DELETE_EXAM",
            message = "Removed exam: \"${target.name}\"",
            entityTitle = target.name
        )
    }

    private suspend fun logStudySession(title: String, minutes: Int): AgentExecutionResult {
        val session = StudySessionEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            plannedMinutes = minutes,
            actualMinutes = minutes,
            status = StudySessionStatus.COMPLETED.name,
            scheduledStart = System.currentTimeMillis() - (minutes * 60 * 1000L),
            scheduledEnd = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        sessionDao.insert(session)
        return AgentExecutionResult(
            success = true,
            actionType = "LOG_STUDY_SESSION",
            message = "Logged $minutes min study session for \"$title\"",
            entityTitle = title
        )
    }
}
