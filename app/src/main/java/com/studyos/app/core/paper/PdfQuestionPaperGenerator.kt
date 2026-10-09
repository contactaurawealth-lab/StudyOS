package com.studyos.app.core.paper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.studyos.app.core.database.entity.PaperEntity
import com.studyos.app.core.database.entity.QuestionBankEntity
import java.io.File
import java.io.FileOutputStream

class PdfQuestionPaperGenerator {

    fun generatePdf(
        context: Context,
        paper: PaperEntity,
        subjectName: String,
        chapterName: String?,
        questions: List<QuestionBankEntity>,
        includeMarkingScheme: Boolean = false
    ): File {
        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = pageWidth - (margin * 2)

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val instructionPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val solutionPaint = Paint().apply {
            color = Color.rgb(40, 80, 140)
            textSize = 8.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 0.8f
        }

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = margin + 15f

        fun drawPageFooter(c: Canvas, pageNum: Int) {
            c.drawLine(margin, pageHeight - margin + 4f, pageWidth - margin, pageHeight - margin + 4f, linePaint)
            c.drawText("StudyOS Offline Examination Board • 100% Air-Gapped", margin, pageHeight - margin + 16f, footerPaint)
            val pText = "Page $pageNum"
            c.drawText(pText, pageWidth - margin - footerPaint.measureText(pText), pageHeight - margin + 16f, footerPaint)
        }

        fun drawRunningHeader(c: Canvas) {
            val hText = "$subjectName • ${paper.title}"
            c.drawText(hText, margin, margin + 10f, instructionPaint)
            val typeText = if (includeMarkingScheme) "OFFICIAL MARKING SCHEME" else "QUESTION PAPER"
            c.drawText(typeText, pageWidth - margin - instructionPaint.measureText(typeText), margin + 10f, instructionPaint)
            c.drawLine(margin, margin + 16f, pageWidth - margin, margin + 16f, linePaint)
            y = margin + 30f
        }

        fun drawHeader(c: Canvas) {
            c.drawText("STUDYOS EXAMINATION BOARD", pageWidth / 2f, y, titlePaint)
            y += 16f
            val subTitle = if (includeMarkingScheme) {
                "OFFICIAL MARKING SCHEME & MODEL SOLUTIONS"
            } else {
                "OFFLINE MODEL QUESTION PAPER"
            }
            c.drawText(subTitle, pageWidth / 2f, y, subtitlePaint)
            y += 14f

            c.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 14f

            // Left meta
            c.drawText("Subject: $subjectName", margin, y, metaPaint)
            // Right meta
            val marksStr = "Max Marks: ${paper.totalMarks}"
            c.drawText(marksStr, pageWidth - margin - metaPaint.measureText(marksStr), y, metaPaint)
            y += 14f

            val scopeStr = "Syllabus: ${chapterName ?: "Entire Subject Curriculum"}"
            c.drawText(scopeStr, margin, y, metaPaint)
            val timeStr = "Time Allowed: ${paper.durationMinutes} Minutes"
            c.drawText(timeStr, pageWidth - margin - metaPaint.measureText(timeStr), y, metaPaint)
            y += 14f

            c.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 12f

            // Instructions
            c.drawText("General Instructions:", margin, y, instructionPaint)
            y += 11f
            if (includeMarkingScheme) {
                c.drawText("1. This document contains the official marking rubric, model answers, and step marks.", margin + 10f, y, instructionPaint)
                y += 11f
                c.drawText("2. Full credit is awarded for logical progression, correct units, and relevant derivations.", margin + 10f, y, instructionPaint)
                y += 11f
                c.drawText("3. Partial marks are indicated where multi-step problem solving is involved.", margin + 10f, y, instructionPaint)
            } else {
                c.drawText("1. All questions are compulsory. Read questions carefully before answering.", margin + 10f, y, instructionPaint)
                y += 11f
                c.drawText("2. Marks allocated to each question are indicated against the question on the right margin.", margin + 10f, y, instructionPaint)
                y += 11f
                c.drawText("3. Write concise, structured, and legible answers. Show formulas and steps where applicable.", margin + 10f, y, instructionPaint)
            }
            y += 14f
            c.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 16f
        }

        fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            val paragraphs = text.split("\n")
            val lines = mutableListOf<String>()

            for (paragraph in paragraphs) {
                if (paragraph.isBlank()) {
                    lines.add("")
                    continue
                }
                val words = paragraph.split(" ")
                val currentLine = StringBuilder()

                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(testLine) <= maxWidth) {
                        currentLine.setLength(0)
                        currentLine.append(testLine)
                    } else {
                        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                        currentLine.setLength(0)
                        currentLine.append(word)
                    }
                }
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
            }
            return lines
        }

        // Draw header on first page
        drawHeader(canvas)

        // Group questions by section
        val secA = questions.filter { it.marks == 1 }
        val secB = questions.filter { it.marks in 2..3 }
        val secC = questions.filter { it.marks >= 4 }

        var qNum = 1

        fun renderSection(title: String, sectionQuestions: List<QuestionBankEntity>) {
            if (sectionQuestions.isEmpty()) return

            if (y > pageHeight - margin - 80f) {
                drawPageFooter(canvas, pageNumber)
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawRunningHeader(canvas)
            }

            canvas.drawText(title, margin, y, sectionPaint)
            y += 6f
            canvas.drawLine(margin, y, margin + sectionPaint.measureText(title) + 10f, y, linePaint)
            y += 14f

            for (q in sectionQuestions) {
                val qPrefix = "Q$qNum. "
                val prefixWidth = metaPaint.measureText(qPrefix)
                val marksText = "[${q.marks} Mark${if (q.marks > 1) "s" else ""}]"
                val marksWidth = metaPaint.measureText(marksText)

                val availableTextWidth = contentWidth - prefixWidth - marksWidth - 10f
                val wrappedLines = wrapText(q.questionText, bodyPaint, availableTextWidth)

                val schemeText = if (includeMarkingScheme) {
                    q.markingScheme?.trim()?.ifBlank { null } ?: "Consult standard textbook solution."
                } else null
                val schemeLines = if (schemeText != null) {
                    wrapText("💡 Solution / Marking Scheme: $schemeText", solutionPaint, contentWidth - prefixWidth - 10f)
                } else emptyList()

                val qHeight = (wrappedLines.size * 13f) + (schemeLines.size * 12f) + (if (includeMarkingScheme) 18f else 12f)

                if (y + qHeight > pageHeight - margin - 25f) {
                    drawPageFooter(canvas, pageNumber)
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    drawRunningHeader(canvas)
                }

                // Draw question number
                canvas.drawText(qPrefix, margin, y, metaPaint)

                // Draw first line of text
                if (wrappedLines.isNotEmpty()) {
                    canvas.drawText(wrappedLines[0], margin + prefixWidth, y, bodyPaint)
                }

                // Draw marks on the right
                canvas.drawText(marksText, pageWidth - margin - marksWidth, y, metaPaint)
                y += 13f

                // Draw subsequent lines of question text
                for (lineIdx in 1 until wrappedLines.size) {
                    canvas.drawText(wrappedLines[lineIdx], margin + prefixWidth, y, bodyPaint)
                    y += 13f
                }

                // If marking scheme is enabled, render solution below question
                if (schemeLines.isNotEmpty()) {
                    y += 3f
                    for (sLine in schemeLines) {
                        canvas.drawText(sLine, margin + prefixWidth, y, solutionPaint)
                        y += 12f
                    }
                }

                y += 8f
                qNum++
            }
            y += 10f
        }

        renderSection("SECTION A: Objective & Conceptual Questions (1 Mark Each)", secA)
        renderSection("SECTION B: Short Answer Questions (2 - 3 Marks Each)", secB)
        renderSection("SECTION C: Long Answer & Problem Solving Questions (4+ Marks Each)", secC)

        // Draw End of Paper footer
        if (y < pageHeight - margin - 20f) {
            val endPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 9f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val endText = if (includeMarkingScheme) "— END OF MARKING SCHEME & MODEL SOLUTIONS —" else "— END OF QUESTION PAPER —"
            canvas.drawText(endText, pageWidth / 2f, pageHeight - margin - 10f, endPaint)
        }

        drawPageFooter(canvas, pageNumber)
        document.finishPage(page)

        val outputDir = File(context.cacheDir, "shared_preview")
        outputDir.mkdirs()
        val fileName = if (includeMarkingScheme) "marking_scheme_${paper.id}.pdf" else "question_paper_${paper.id}.pdf"
        val pdfFile = File(outputDir, fileName)
        FileOutputStream(pdfFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return pdfFile
    }
}
