package com.studyos.app.core.paper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.studyos.app.domain.model.LossReasonCategory
import com.studyos.app.domain.model.LostMark
import com.studyos.app.domain.model.Mistake
import com.studyos.app.domain.model.RecallItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfPreExamPrimerGenerator {

    fun generatePrimerPdf(
        context: Context,
        subjectName: String,
        recallItems: List<RecallItem>,
        lostMarks: List<LostMark>,
        mistakes: List<Mistake>,
        examTitle: String? = null
    ): File {
        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - (margin * 2)

        // Paints
        val headerTitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val headerSubPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val sectionTitlePaint = Paint().apply {
            color = Color.rgb(20, 40, 80)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val normalBodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val trapTextPaint = Paint().apply {
            color = Color.rgb(160, 20, 20)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val trapHeaderPaint = Paint().apply {
            color = Color.rgb(180, 20, 20)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(248, 249, 250)
            style = Paint.Style.FILL
        }

        val cardBorderPaint = Paint().apply {
            color = Color.rgb(220, 224, 230)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        val trapCardBgPaint = Paint().apply {
            color = Color.rgb(255, 245, 245)
            style = Paint.Style.FILL
        }

        val trapCardBorderPaint = Paint().apply {
            color = Color.rgb(240, 180, 180)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + 12f

        fun drawFooter() {
            canvas.drawText(
                "StudyOS 95OS • 100% Offline Exam Operating System • Page $pageNumber",
                pageWidth / 2f,
                pageHeight - margin + 10f,
                footerPaint
            )
        }

        fun ensureVerticalSpace(neededHeight: Float) {
            if (y + neededHeight > pageHeight - margin - 20f) {
                drawFooter()
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin + 12f
            }
        }

        fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)
            return lines
        }

        // 1. Header Banner
        val headerBoxHeight = 58f
        canvas.drawRoundRect(
            RectF(margin, y, pageWidth - margin, y + headerBoxHeight),
            8f, 8f, cardBgPaint
        )
        canvas.drawRoundRect(
            RectF(margin, y, pageWidth - margin, y + headerBoxHeight),
            8f, 8f, cardBorderPaint
        )

        y += 18f
        val displayTitle = examTitle ?: "$subjectName • PRE-EXAM RAPID PRIMER"
        canvas.drawText(displayTitle.uppercase(Locale.US), pageWidth / 2f, y, headerTitlePaint)
        y += 14f
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        canvas.drawText(
            "Target: 95%+ Mastery War Map  •  Last 60 Minutes Revision  •  Generated $dateStr",
            pageWidth / 2f,
            y,
            headerSubPaint
        )
        y += 36f

        // 2. Section: Personal Minefield & Careless Trap Warnings
        ensureVerticalSpace(45f)
        canvas.drawText("⚡ SECTION 1: PERSONAL MINEFIELD & CARELESS TRAP ALERTS", margin, y, sectionTitlePaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, cardBorderPaint)
        y += 12f

        val executionLostMarks = lostMarks.filter {
            it.category in setOf(
                LossReasonCategory.CARELESS_MISTAKE,
                LossReasonCategory.CALCULATION_ERROR,
                LossReasonCategory.MISREAD,
                LossReasonCategory.TIME_SHORTAGE
            )
        }
        val totalExecutionMarksLost = executionLostMarks.sumOf { it.marksLost }

        val trapWarnings = mutableListOf<String>()
        if (totalExecutionMarksLost > 0) {
            trapWarnings.add(
                "🚨 Recoverable Score Warning: You leaked ${String.format(Locale.US, "%.1f", totalExecutionMarksLost)} marks on careless mistakes and calculation slips in previous exams. Run a mandatory 5-minute units/signs audit before submitting."
            )
        }

        mistakes.take(4).forEach { mistake ->
            val cleanQ = mistake.question.take(75)
            val cleanAns = mistake.correctAnswer.take(60)
            trapWarnings.add("⚠️ Trap Question: \"$cleanQ...\" → Crucial Note: $cleanAns")
        }

        if (trapWarnings.isEmpty()) {
            trapWarnings.add("✅ Zero recurring traps detected. Maintain pristine presentation, clearly boxed answers, and methodical step working.")
        }

        trapWarnings.forEach { warning ->
            val wrapped = wrapText(warning, trapTextPaint, contentWidth - 18f)
            val boxH = (wrapped.size * 12f) + 14f
            ensureVerticalSpace(boxH + 6f)

            canvas.drawRoundRect(
                RectF(margin, y, pageWidth - margin, y + boxH),
                6f, 6f, trapCardBgPaint
            )
            canvas.drawRoundRect(
                RectF(margin, y, pageWidth - margin, y + boxH),
                6f, 6f, trapCardBorderPaint
            )

            var lineY = y + 12f
            wrapped.forEach { line ->
                canvas.drawText(line, margin + 10f, lineY, trapTextPaint)
                lineY += 12f
            }
            y += boxH + 8f
        }

        // 3. Section: High-Yield Formulas & Definitions
        ensureVerticalSpace(45f)
        canvas.drawText("📖 SECTION 2: HIGH-YIELD FORMULAS & DEFINITIONS", margin, y, sectionTitlePaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, cardBorderPaint)
        y += 14f

        val formulaItems = recallItems.ifEmpty {
            listOf(
                RecallItem(
                    chapterId = "default",
                    subjectId = "default",
                    questionType = com.studyos.app.domain.model.RecallQuestionType.CONCEPT_EXPLANATION,
                    prompt = "First Principles Protocol",
                    expectedAnswer = "State law, define variables, write standard equation, substitute with units.",
                    explanation = "Standard derivation template"
                ),
                RecallItem(
                    chapterId = "default",
                    subjectId = "default",
                    questionType = com.studyos.app.domain.model.RecallQuestionType.FORMULA_RECALL,
                    prompt = "Dimensional Consistency",
                    expectedAnswer = "Verify LHS units = RHS units before concluding numerical problems.",
                    explanation = "Dimensional analysis error check"
                ),
                RecallItem(
                    chapterId = "default",
                    subjectId = "default",
                    questionType = com.studyos.app.domain.model.RecallQuestionType.DEFINITION_RECALL,
                    prompt = "Boundary Conditions & Edge Cases",
                    expectedAnswer = "Check values at zero, infinity, and equilibrium states to verify formula validity.",
                    explanation = "Edge case sanity checks"
                )
            )
        }

        formulaItems.forEachIndexed { idx, item ->
            val promptWrapped = wrapText("${idx + 1}. ${item.prompt}", boldBodyPaint, contentWidth - 16f)
            val ansWrapped = wrapText("💡 ${item.expectedAnswer}", normalBodyPaint, contentWidth - 24f)
            val boxH = ((promptWrapped.size + ansWrapped.size) * 12f) + 16f

            ensureVerticalSpace(boxH + 6f)

            canvas.drawRoundRect(
                RectF(margin, y, pageWidth - margin, y + boxH),
                6f, 6f, cardBgPaint
            )
            canvas.drawRoundRect(
                RectF(margin, y, pageWidth - margin, y + boxH),
                6f, 6f, cardBorderPaint
            )

            var textY = y + 13f
            promptWrapped.forEach { line ->
                canvas.drawText(line, margin + 8f, textY, boldBodyPaint)
                textY += 12f
            }
            textY += 2f
            ansWrapped.forEach { line ->
                canvas.drawText(line, margin + 14f, textY, normalBodyPaint)
                textY += 12f
            }
            y += boxH + 7f
        }

        // 4. Section: Exam-Hall Pacing & Audit Strategy
        ensureVerticalSpace(75f)
        canvas.drawText("⏱️ SECTION 3: EXAM-HALL PACING & AUDIT PROTOCOL", margin, y, sectionTitlePaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, cardBorderPaint)
        y += 14f

        val strategies = listOf(
            "1. Pacing Standard: 1.2 to 1.4 minutes per mark. Never exceed 15 minutes on a single sub-problem.",
            "2. First Pass Rule: Solve 100% of confident questions first. Star complex questions and move on.",
            "3. Mandatory 15-Minute Audit: Stop writing new answers 15 mins before time. Check calculations, decimal points, and unit labels."
        )

        strategies.forEach { strat ->
            ensureVerticalSpace(15f)
            canvas.drawText(strat, margin + 4f, y, normalBodyPaint)
            y += 14f
        }

        drawFooter()
        document.finishPage(page)

        // Write to file
        val outputDir = File(context.cacheDir, "pre_exam_primers")
        if (!outputDir.exists()) outputDir.mkdirs()

        val cleanSubName = subjectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(outputDir, "Primer_${cleanSubName}_95OS.pdf")
        val fos = FileOutputStream(outputFile)
        document.writeTo(fos)
        document.close()
        fos.flush()
        fos.close()

        return outputFile
    }
}
