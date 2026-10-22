# Now
- [ ] Step 4: Universal CSV Infrastructure (`UniversalCsvProcessor` & Templates)

# Next
- [ ] Step 5: Question Bank Repository & Management UI
- [ ] Step 6: Recall Engine enhancements (SM-2 + Mistake Bank linkage)
- [ ] Step 7: PaperPilot Paper Generation Engine
- [ ] Step 8: Offline Printable PDF Paper Generator

# Later
- [ ] Step 9: Real Exam Mode (App Blocker + Study Timer integration)
- [ ] Step 11: Lost Marks System (Frequency, categories, and trends)
- [ ] Step 12: Connected Mistake Loop (Mistake -> Recall -> Next Paper remediation)
- [ ] Step 13: 95% Target Engine (Deterministic gap and leverage analysis)
- [ ] Step 14: Focus / Regain Layer polish
- [ ] Step 15: Connected Academic Analytics
- [ ] Step 16: UI / UX Polish & Theme integration
- [ ] Step 17: Full End-to-End Regression Testing

# Bugs
- [ ] (No open bugs currently)

# Completed
- [x] Step 1: Complete codebase inspection & 95OS architecture mapping
- [x] Step 7 (Documentation): Create AGENTS.md, FEATURES.md, ARCHITECTURE.md, PRODUCT.md, TODO.md
- [x] 95OS Logo generation & asset export (Procedural PIL script + multi-resolution PNGs)
- [x] Phase 2 Onboarding & First Complete User Journey
- [x] Step 2: Domain/data model extensions (Room Migration 10 → 11: `TopicEntity`, `QuestionBankEntity`, `PaperEntity`, `PaperQuestionCrossRefEntity`, `ExamResultEntity`, `LostMarksEntity`, repositories, DAOs, and tests)
- [x] Step 3: Syllabus & Topic Tracking UI and ViewModels (Section 4: Syllabus Intelligence, `SyllabusTopicsSection`, mastery states, exam relevance, and tests)
- [x] Step 10: Result Recording & Diagnostic Flow (`RecordExamResultUseCase`, `GetExamDiagnosticUseCase`, `RecordExamResultDialog`, 10 diagnostic categories, connected mistake bank loop, and comprehensive unit tests)
- [x] Subject, Chapter & Revision Simplification:
  - Streamlined `SubjectsScreen.kt`: clean title, chapter count subtitle, single readiness badge, clean status summary, and 1-tap action pill (no micro-badge noise).
  - Streamlined `ChapterDetailScreen.kt`: unified 3-chip Status Selector (`Not Started`, `In Progress`, `Completed`) with auto-mapped progress, fine-tuning slider only when in progress, cleaned overflow dropdown menu (no redundant actions).
  - Streamlined `RevisionDashboardScreen.kt`: Zen Due Today hero banner ("Start Revision Blitz →"), clean filter chips (`All Due`, `Chapters`, `Flashcards`, `Weak Topics`), moved Leitner pipeline strip into Practice Lab to eliminate daily revision cognitive overload.
