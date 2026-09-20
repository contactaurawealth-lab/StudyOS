package com.studyos.app.data.repository

import com.studyos.app.core.database.dao.RevisionDao
import com.studyos.app.core.database.entity.ActiveRecallLogEntity
import com.studyos.app.core.database.entity.RevisionScheduleEntity
import com.studyos.app.core.datastore.PreferencesDataSource
import com.studyos.app.domain.model.ActiveRecallSessionSummary
import com.studyos.app.domain.model.RevisionSchedule
import com.studyos.app.domain.repository.RevisionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.Calendar
import kotlin.math.roundToInt

class RevisionRepositoryImpl(
    private val revisionDao: RevisionDao,
    private val preferencesDataSource: PreferencesDataSource
) : RevisionRepository {

    override fun observeDueSchedules(currentTime: Long): Flow<List<RevisionSchedule>> {
        return revisionDao.observeDueSchedules(currentTime).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeAllSchedules(): Flow<List<RevisionSchedule>> {
        return revisionDao.observeAllSchedules().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeScheduleForChapter(chapterId: String): Flow<RevisionSchedule?> {
        return revisionDao.getScheduleForChapter(chapterId).map { it?.toDomain() }
    }

    override suspend fun getScheduleForChapter(chapterId: String): RevisionSchedule? {
        return revisionDao.getScheduleForChapterOnce(chapterId)?.toDomain()
    }

    override suspend fun saveSchedule(schedule: RevisionSchedule) {
        revisionDao.insertOrUpdateSchedule(schedule.toEntity())
    }

    override suspend fun recordChapterRevisionCompleted(
        chapterId: String,
        subjectId: String,
        qualityScore: Int
    ) {
        val existing = revisionDao.getScheduleForChapterOnce(chapterId)
        val now = System.currentTimeMillis()

        val newSchedule = if (existing != null) {
            val count = existing.revisionCount
            val ef = existing.easeFactor
            if (qualityScore >= 3) {
                val nextInterval = when (count) {
                    0 -> 1
                    1 -> 3
                    2 -> 7
                    3 -> 14
                    else -> (existing.intervalDays * ef).roundToInt().coerceAtLeast(existing.intervalDays + 1)
                }
                val newEf = (ef + (0.1f - (5 - qualityScore) * (0.08f + (5 - qualityScore) * 0.02f)))
                    .coerceIn(1.3f, 2.5f)
                existing.copy(
                    intervalDays = nextInterval,
                    revisionCount = count + 1,
                    lastRevisedAt = now,
                    nextRevisionDue = now + (nextInterval * 86_400_000L),
                    easeFactor = newEf
                )
            } else {
                existing.copy(
                    intervalDays = 1,
                    revisionCount = 0,
                    lastRevisedAt = now,
                    nextRevisionDue = now + 86_400_000L,
                    easeFactor = (ef - 0.2f).coerceAtLeast(1.3f)
                )
            }
        } else {
            RevisionScheduleEntity(
                chapterId = chapterId,
                subjectId = subjectId,
                intervalDays = if (qualityScore >= 3) 3 else 1,
                revisionCount = if (qualityScore >= 3) 1 else 0,
                lastRevisedAt = now,
                nextRevisionDue = now + (if (qualityScore >= 3) 3 else 1) * 86_400_000L,
                easeFactor = 2.5f
            )
        }
        revisionDao.insertOrUpdateSchedule(newSchedule)
    }

    override suspend fun recordRecallSession(summary: ActiveRecallSessionSummary): Int {
        val todayEpochDay = LocalDate.now().toEpochDay()
        val newStreak = preferencesDataSource.updateRevisionStreak(todayEpochDay)

        val log = ActiveRecallLogEntity(
            sessionType = summary.sessionType.name,
            durationSeconds = summary.durationSeconds,
            completedAt = System.currentTimeMillis(),
            itemsReviewedCount = summary.itemsReviewedCount,
            correctCount = summary.correctCount,
            accuracyPercentage = summary.accuracyPercentage,
            streakDays = newStreak
        )
        revisionDao.insertRecallLog(log)
        return newStreak
    }

    override fun observeMinutesRevisedToday(): Flow<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_400_000L - 1L
        return revisionDao.getMinutesRevisedToday(startOfDay, endOfDay).map { it ?: 0 }
    }

    override val revisionStreakDays: Flow<Int> = preferencesDataSource.revisionStreakDays
    override val dailyRevisionTargetMinutes: Flow<Int> = preferencesDataSource.dailyRevisionTargetMinutes

    override suspend fun setDailyRevisionTargetMinutes(minutes: Int) {
        preferencesDataSource.setDailyRevisionTargetMinutes(minutes)
    }

    private fun RevisionScheduleEntity.toDomain() = RevisionSchedule(
        chapterId = chapterId,
        subjectId = subjectId,
        intervalDays = intervalDays,
        revisionCount = revisionCount,
        lastRevisedAt = lastRevisedAt,
        nextRevisionDue = nextRevisionDue,
        easeFactor = easeFactor
    )

    private fun RevisionSchedule.toEntity() = RevisionScheduleEntity(
        chapterId = chapterId,
        subjectId = subjectId,
        intervalDays = intervalDays,
        revisionCount = revisionCount,
        lastRevisedAt = lastRevisedAt,
        nextRevisionDue = nextRevisionDue,
        easeFactor = easeFactor
    )
}
