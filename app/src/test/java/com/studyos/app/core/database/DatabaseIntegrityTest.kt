package com.studyos.app.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseIntegrityTest {

    @Test
    fun testCleanIntegrityReport() {
        val report = IntegrityReport(isClean = true, errors = emptyList())
        assertTrue(report.isClean)
        assertTrue(report.errors.isEmpty())
        assertTrue(report.checkedAt > 0L)
    }

    @Test
    fun testCorruptedIntegrityReportRecordsErrors() {
        val errors = listOf("row 42 corrupted b-tree page", "foreign key violation")
        val report = IntegrityReport(isClean = false, errors = errors)
        assertFalse(report.isClean)
        assertEquals(2, report.errors.size)
        assertEquals("row 42 corrupted b-tree page", report.errors[0])
    }

    @Test
    fun testMigrationSafetyDefinitionsExist() {
        // Ensure all migration definitions are present up to version 10
        assertEquals(1, StudyOSDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, StudyOSDatabase.MIGRATION_1_2.endVersion)
        assertEquals(8, StudyOSDatabase.MIGRATION_8_9.startVersion)
        assertEquals(9, StudyOSDatabase.MIGRATION_8_9.endVersion)
        assertEquals(9, StudyOSDatabase.MIGRATION_9_10.startVersion)
        assertEquals(10, StudyOSDatabase.MIGRATION_9_10.endVersion)
        assertEquals(10, StudyOSDatabase.MIGRATION_10_11.startVersion)
        assertEquals(11, StudyOSDatabase.MIGRATION_10_11.endVersion)
    }
}
