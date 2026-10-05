# 95OS Features Specification & Traceability Matrix

This document defines every product capability in 95OS, detailing its purpose, user flow, data dependencies, connected modules, acceptance criteria, and implementation status.

---

## 1. Syllabus Intelligence (Topic Tracking)
- **Purpose:** Provide a granular academic tree down to individual topics so students know exactly what is mastered versus vulnerable.
- **User Flow:**
  1. Student selects Subject → Chapter → View Topics.
  2. Student marks topic status: `Not Started`, `Learning`, `Revised`, `Mastered`.
  3. Student sets exam relevance / weight (Low, Medium, High).
  4. System computes chapter and subject mastery percentages based on topic health.
- **Data Dependencies:** `SubjectEntity`, `ChapterEntity`, `TopicEntity`.
- **Connected Modules:** PaperPilot (filters paper generation by topics), 95% Target Engine, Universal CSV.
- **Acceptance Criteria:**
  - Topic states persist locally in Room with foreign keys to Chapter.
  - Adding/removing topics recalculates chapter progress in real-time.
- **Status:** Pending (Needs `TopicEntity` Room migration).

---

## 2. Recall Engine (Extended SM-2)
- **Purpose:** Spaced repetition engine targeting active recall and long-term retention.
- **User Flow:**
  1. Student starts Recall session (Quick 5, 10, 20 items or Due Today).
  2. Question prompt displayed → Student reflects → Taps reveal.
  3. Student self-rates: `Again`, `Hard`, `Good`, `Easy`.
  4. System updates interval, ease factor, and schedules next review.
- **Data Dependencies:** `RecallItemEntity`, `RecallAttemptEntity`, `FlashcardEntity`, `MistakeEntity`.
- **Connected Modules:** Syllabus (topic linkage), Mistake Loop (mistakes auto-generate recall items).
- **Acceptance Criteria:**
  - SM-2 algorithm correctly scales interval days based on rating.
  - Reviews due today query executes purely offline.
- **Status:** Partially Implemented (Core SM-2 exists; needs full mistake integration and Quick 5/10/20 presets).

---

## 3. PaperPilot (Exam Paper Generator)
- **Purpose:** Generate authentic, balanced examination papers from the local question bank.
- **User Flow:**
  1. Student taps "Create Paper" in PaperPilot.
  2. Selects Subject, Chapters/Topics, Total Marks (e.g. 50, 80, 100), Duration (e.g. 120m), Difficulty, Question Types, and Section structure (Section A: MCQs, Section B: Short Answer, Section C: Long Answer).
  3. System queries Question Bank prioritizing weak topics and untested questions.
  4. Paper is generated and saved as an offline exam record.
- **Data Dependencies:** `PaperEntity`, `PaperQuestionCrossRefEntity`, `QuestionBankEntity`, `TopicEntity`.
- **Connected Modules:** PDF Paper Generator, Real Exam Mode, Result Entry, Universal CSV.
- **Acceptance Criteria:**
  - Generates paper matching exact marks distribution without repeating recent questions unless pool is exhausted.
- **Status:** Pending.

---

## 4. Real Exam Mode
- **Purpose:** Replicate physical examination conditions and enforce deep focus.
- **User Flow:**
  1. Student opens generated paper → Taps "Start Exam Mode".
  2. Fullscreen countdown timer starts.
  3. Native `AppBlockerAccessibilityService` engages, blocking distracting apps.
  4. Student writes answers on physical paper.
  5. When time expires or student taps finish, exam session closes and transitions to Result Entry.
- **Data Dependencies:** `PaperEntity`, `StudySessionEntity`, `AppBlockerManager`.
- **Connected Modules:** PaperPilot, Study Timer, App Blocker.
- **Acceptance Criteria:**
  - Timer runs via foreground service surviving process backgrounding.
  - Exiting early prompts confirmation dialog.
- **Status:** Integration Pending (Timer and Blocker exist separately; require unifying Exam Mode coordinator).

---

## 5. Offline PDF Paper Generation
- **Purpose:** Produce publication-quality, printable PDF exam papers.
- **User Flow:**
  1. Student taps "Print / Export PDF" on any generated paper.
  2. Local PDF engine constructs pages with school header, date, duration, max marks, instructions, sections, and question marks.
  3. Native Android print/share intent triggered via `FileProvider`.
- **Data Dependencies:** `PaperEntity`, `QuestionBankEntity`.
- **Connected Modules:** PaperPilot, Document Viewer.
- **Acceptance Criteria:**
  - Generates PDF entirely on-device without internet.
  - Strict pagination: Clean page breaks with no split questions.
- **Status:** Pending.

---

## 6. Result Recording & Lost Marks System
- **Purpose:** Detailed diagnostic post-mortem of test results to understand where and why marks were lost.
- **User Flow:**
  1. Student enters Total Marks Obtained and Time Taken.
  2. For every question where full marks were not achieved, student logs marks lost and selects a Loss Category:
     - `Didn't know`
     - `Forgot`
     - `Concept error`
     - `Calculation error`
     - `Misread`
     - `Careless mistake`
     - `Poor presentation`
     - `Time shortage`
     - `Incomplete answer`
     - `Other`
  3. System records lost marks frequency and associates them with Chapter and Topic.
- **Data Dependencies:** `ExamResultEntity`, `LostMarksEntity`, `MistakeEntity`.
- **Connected Modules:** Mistake Bank, 95% Target Engine.
- **Acceptance Criteria:**
  - Aggregates monthly/weekly loss trends (e.g. "Careless mistakes: -14 marks").
- **Status:** Pending.

---

## 7. Connected Mistake Loop
- **Purpose:** Ensure every mistake is actively remediated until full recovery is verified.
- **User Flow:**
  1. Lost mark logged → Question automatically added to `MistakeBank`.
  2. Corresponding `RecallItem` is created or priority boosted in Recall queue.
  3. Next revision/paper generation prioritizes this concept.
  4. When re-tested and answered correctly, mistake is marked `Recovered`.
- **Data Dependencies:** `MistakeEntity`, `RecallItemEntity`, `QuestionBankEntity`.
- **Connected Modules:** Recall Engine, PaperPilot, Syllabus Intelligence.
- **Acceptance Criteria:**
  - Resolving a mistake marks recovery date and decreases topic weakness index.
- **Status:** Partially Implemented (Mistake Bank exists; needs automatic Recall bridge and recovery tracking).

---

## 8. 95% Target Engine
- **Purpose:** Transparent, deterministic mathematical engine projecting current score and charting the shortest path to 95%+.
- **User Flow:**
  1. Student sets target (default 95%).
  2. System analyzes:
     - Current weighted average across practice exams.
     - Syllabus completion and topic mastery percentage.
     - Recoverable marks from identified mistake categories.
  3. Displays projection: "Current: 89.2% | Gap: 5.8% | Top opportunities: Maths (+6 marks), Physics (+4 marks)".
- **Data Dependencies:** `ExamResultEntity`, `LostMarksEntity`, `TopicEntity`, `StudyPreferencesEntity`.
- **Connected Modules:** Today Dashboard, Progress Analytics.
- **Acceptance Criteria:**
  - Strictly deterministic calculations with no mock or fabricated data.
- **Status:** Pending.

---

## 9. Focus / Regain Layer
- **Purpose:** Distraction-free study tracking with app blocking.
- **User Flow:**
  1. Student launches focus session (Pomodoro or Custom duration).
  2. Foreground service displays persistent notification with remaining time.
  3. Blocked apps are prevented from opening via accessibility service.
- **Data Dependencies:** `StudySessionEntity`, `AppBlockerManager`.
- **Connected Modules:** Real Exam Mode, Progress Tracking.
- **Acceptance Criteria:**
  - Accessible without network; distraction logs stored locally.
- **Status:** Implemented (Working; needs Exam Mode hook).

---

## 10. Universal CSV System
- **Purpose:** Single, standardized offline CSV infrastructure for importing and exporting academic content.
- **User Flow:**
  1. Student selects Import/Export in Settings.
  2. Selects entity type: Syllabus (Subjects/Chapters/Topics), Question Bank, Flashcards/Recall, Mistakes.
  3. System provides downloadable template, previews parsed rows, validates relational foreign keys, flags errors, and commits via atomic Room transaction.
- **Data Dependencies:** All primary Room DAOs.
- **Connected Modules:** Syllabus, Question Bank, Recall Engine, Settings.
- **Acceptance Criteria:**
  - Rejects malformed rows with line numbers and clear error messages.
  - Exports cleanly formatted UTF-8 CSVs.
- **Status:** Pending.

---

## 11. Notes & Document Viewer
- **Purpose:** Rich markdown notes and offline viewing of syllabi, past papers, and study guides.
- **Data Dependencies:** `NoteEntity`, local storage URIs.
- **Connected Modules:** Subject & Chapter views.
- **Status:** Implemented.

---

## 12. Local Backup & Restore
- **Purpose:** Full sovereign data ownership via encrypted/compressed JSON/ZIP archive export and import using Android Storage Access Framework.
- **Data Dependencies:** `BackupManager`, Room SQLite instance.
- **Status:** Implemented.
