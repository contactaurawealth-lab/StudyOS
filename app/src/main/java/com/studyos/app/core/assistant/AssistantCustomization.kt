package com.studyos.app.core.assistant

import androidx.compose.ui.graphics.Color

enum class AssistantThemeGlow(
    val title: String,
    val description: String,
    val startColor: Color,
    val endColor: Color,
    val accentColor: Color
) {
    GEMINI_AURORA(
        title = "Gemini Aurora",
        description = "Cosmic cyan, indigo & purple gradient",
        startColor = Color(0xFF4285F4),
        endColor = Color(0xFF9B51E0),
        accentColor = Color(0xFF7086FF)
    ),
    STUDYOS_GOLD(
        title = "StudyOS Amber",
        description = "Signature warm gold & copper glow",
        startColor = Color(0xFFD4AF37),
        endColor = Color(0xFFFF9800),
        accentColor = Color(0xFFFFB300)
    ),
    CYBER_EMERALD(
        title = "Cyber Emerald",
        description = "Deep focus neon mint & teal sheen",
        startColor = Color(0xFF00E676),
        endColor = Color(0xFF00B0FF),
        accentColor = Color(0xFF1DE9B6)
    ),
    OBSIDIAN_MINIMAL(
        title = "Obsidian Dark",
        description = "Monochrome titanium glass border",
        startColor = Color(0xFF757575),
        endColor = Color(0xFF424242),
        accentColor = Color(0xFF9E9E9E)
    )
}

enum class AssistantPersona(val title: String, val promptInstruction: String) {
    SOCRATIC_TUTOR(
        title = "Socratic Tutor",
        promptInstruction = "You are a warm, encouraging Socratic tutor. Guide the student to understanding with clear explanations, illustrative analogies, and a thought-provoking follow-up question."
    ),
    EXAM_CRAM(
        title = "Exam Cram & Solver",
        promptInstruction = "You are an urgent exam-prep assistant. Give ultra-concise, high-yield bulleted answers, exact definitions, formulas, and common exam pitfalls."
    ),
    NOTE_MAKER(
        title = "Summarizer & Notes",
        promptInstruction = "Format the answer as clean, structured Markdown study notes with bold headings, concise bullet points, and key takeaways ready to save into a notebook."
    ),
    FLASHCARD_GENERATOR(
        title = "Flashcard Q&A",
        promptInstruction = "Formulate high-yield question-and-answer pairs designed for active recall and spaced repetition flashcard revision."
    )
}

data class AssistantCustomization(
    val themeGlow: AssistantThemeGlow = AssistantThemeGlow.GEMINI_AURORA,
    val persona: AssistantPersona = AssistantPersona.SOCRATIC_TUTOR,
    val autoListenOnLaunch: Boolean = true,
    val speakResponses: Boolean = false,
    val autoCaptureScreen: Boolean = true,
    val hapticFeedback: Boolean = true
)
