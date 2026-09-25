package com.studyos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.studyos.app.domain.model.RecallAttempt
import com.studyos.app.domain.model.RecallItem
import com.studyos.app.domain.model.RecallQuestionType
import com.studyos.app.domain.model.RecallState
import org.json.JSONArray
import java.util.UUID

@Entity(
    tableName = "recall_items",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("chapterId"),
        Index("subjectId"),
        Index("recallState"),
        Index("nextReviewTimestamp")
    ]
)
data class RecallItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val chapterId: String,
    val subjectId: String,
    val questionType: String = RecallQuestionType.CONCEPT_EXPLANATION.name,
    val prompt: String,
    val expectedAnswer: String,
    val explanation: String,
    val optionsJson: String = "[]",
    val recallState: String = RecallState.NEW.name,
    val lastStudiedTimestamp: Long? = null,
    val lastRecalledTimestamp: Long? = null,
    val recallAccuracy: Int = 0,
    val recallAttempts: Int = 0,
    val consecutiveCorrect: Int = 0,
    val consecutiveIncorrect: Int = 0,
    val confidence: Float = 0.5f,
    val intervalDays: Int = 1,
    val nextReviewTimestamp: Long = System.currentTimeMillis()
)

fun RecallItemEntity.toDomain(): RecallItem {
    val type = try {
        RecallQuestionType.valueOf(questionType)
    } catch (_: Exception) {
        RecallQuestionType.CONCEPT_EXPLANATION
    }

    val state = try {
        RecallState.valueOf(recallState)
    } catch (_: Exception) {
        RecallState.NEW
    }

    val parsedOptions = mutableListOf<String>()
    try {
        val arr = JSONArray(optionsJson)
        for (i in 0 until arr.length()) {
            parsedOptions.add(arr.getString(i))
        }
    } catch (_: Exception) {}

    return RecallItem(
        id = id,
        chapterId = chapterId,
        subjectId = subjectId,
        questionType = type,
        prompt = prompt,
        expectedAnswer = expectedAnswer,
        explanation = explanation,
        options = parsedOptions,
        recallState = state,
        lastStudiedTimestamp = lastStudiedTimestamp,
        lastRecalledTimestamp = lastRecalledTimestamp,
        recallAccuracy = recallAccuracy,
        recallAttempts = recallAttempts,
        consecutiveCorrect = consecutiveCorrect,
        consecutiveIncorrect = consecutiveIncorrect,
        confidence = confidence,
        intervalDays = intervalDays,
        nextReviewTimestamp = nextReviewTimestamp
    )
}

fun RecallItem.toEntity(): RecallItemEntity {
    val arr = JSONArray()
    options.forEach { arr.put(it) }

    return RecallItemEntity(
        id = id,
        chapterId = chapterId,
        subjectId = subjectId,
        questionType = questionType.name,
        prompt = prompt,
        expectedAnswer = expectedAnswer,
        explanation = explanation,
        optionsJson = arr.toString(),
        recallState = recallState.name,
        lastStudiedTimestamp = lastStudiedTimestamp,
        lastRecalledTimestamp = lastRecalledTimestamp,
        recallAccuracy = recallAccuracy,
        recallAttempts = recallAttempts,
        consecutiveCorrect = consecutiveCorrect,
        consecutiveIncorrect = consecutiveIncorrect,
        confidence = confidence,
        intervalDays = intervalDays,
        nextReviewTimestamp = nextReviewTimestamp
    )
}

@Entity(
    tableName = "recall_attempts",
    foreignKeys = [
        ForeignKey(
            entity = RecallItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["recallItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("recallItemId"),
        Index("chapterId"),
        Index("timestamp")
    ]
)
data class RecallAttemptEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val recallItemId: String,
    val chapterId: String,
    val userAnswer: String,
    val wasCorrect: Boolean,
    val confidenceRating: Int = 3,
    val timestamp: Long = System.currentTimeMillis()
)

fun RecallAttemptEntity.toDomain(): RecallAttempt {
    return RecallAttempt(
        id = id,
        recallItemId = recallItemId,
        chapterId = chapterId,
        userAnswer = userAnswer,
        wasCorrect = wasCorrect,
        confidenceRating = confidenceRating,
        timestamp = timestamp
    )
}

fun RecallAttempt.toEntity(): RecallAttemptEntity {
    return RecallAttemptEntity(
        id = id,
        recallItemId = recallItemId,
        chapterId = chapterId,
        userAnswer = userAnswer,
        wasCorrect = wasCorrect,
        confidenceRating = confidenceRating,
        timestamp = timestamp
    )
}
