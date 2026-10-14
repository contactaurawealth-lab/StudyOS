## StudyOS v1.8.0 — In-Interface CSV Error Resolution & Production Hardening

### Overview
StudyOS v1.8.0 introduces an **In-Interface CSV Error Resolution & Question Rescue System** alongside five critical production hardening improvements across App Blocker, PDF pagination, database transactions, spaced repetition scheduling, and Feynman Audio Walk focus hygiene.

---

### 1. In-Interface CSV Error Resolution & Question Rescue
In traditional quiz tools and question banks, CSV imports are brittle: a single missing option column, unescaped quote, or misaligned row causes the entire file to be rejected, forcing the student to leave the app and edit spreadsheets.

In StudyOS v1.8.0, students can diagnose, repair, or rescue questions **directly on-screen**:

- **Automatic Inline Option Extraction**:
  When question stems contain inline option markers (e.g., `Which is true? (A) ... (B) ...`), `QuestionBankQuizEngine.parseQuestionContent` automatically isolates the stem and populates Option A/B/C/D without flagging errors.
- **Interactive "Issues Found" Cards**:
  Any row with missing options or syntax issues is displayed in an in-dialog issue list with the row number, exact diagnostic reason, and prompt snippet.
- **⚡ 1-Tap "Convert to Short Answer"**:
  Converts questions missing options to `SHORT_ANSWER` with a single tap, preserving the prompt and answer in the Question Bank and Flashcards without typing.
- **⚡ "Convert All to Short Answer"**:
  Bulk-rescues all flagged rows in one tap, saving time on large legacy datasets.
- **✏️ "Fix Options" (`FixQuestionRowDialog`)**:
  Opens an in-place modal editor allowing users to:
  - Edit question prompt
  - Switch format between `MCQ`, `Fill Blank`, `T / F`, and `Short Answer`
  - Input Option A, Option B, Option C, Option D
  - Tap quick selection chips (`[ A ]`, `[ B ]`, `[ C ]`, `[ D ]` or `[ True ]`, `[ False ]`)
  - Provide an optional explanation and marks weight
- **Dynamic Valid Count & Real-Time Preview**:
  Rescued and repaired questions immediately increment the valid count (`✓ X valid question(s)`) and render in the live question preview card with standardized `(A)`, `(B)`, `(C)`, `(D)` styling.

---

### 2. Visual Blueprint: How Each Question Looks

#### A. Multiple Choice Question (MCQ)
```
┌──────────────────────────────────────────────────────────┐
│ [MCQ]  [● Easy]  [🎯 2x tested]                 ★ 1 Mark │
│ Which particle is the primary carrier of electric       │
│ current in metallic conductors?                          │
│                                                          │
│  (A) Protons                                             │
│  (B) Free Electrons                           ✓ Correct  │
│  (C) Neutrons                                            │
│  (D) Positrons                                           │
│                                                          │
│  💡 Solution Key & Rationale:                            │
│  In metallic conductors, valence electrons are           │
│  delocalized and drift under an applied electric field.  │
└──────────────────────────────────────────────────────────┘
```

#### B. Fill-in-the-Blank (FIB)
```
┌──────────────────────────────────────────────────────────┐
│ [FILL BLANK]                                    ★ 1 Mark │
│ The light-absorbing green pigment found inside           │
│ chloroplasts is called [ ________ ].                     │
│                                                          │
│ 🎯 Target Blank Answer: [ Chlorophyll ]                  │
└──────────────────────────────────────────────────────────┘
```

#### C. True / False
```
┌──────────────────────────────────────────────────────────┐
│ [TRUE / FALSE]                                  ★ 1 Mark │
│ An exothermic reaction absorbs thermal energy from its   │
│ surroundings.                                            │
│                                                          │
│  [ TRUE ]               [ ✓ FALSE (Correct) ]            │
└──────────────────────────────────────────────────────────┘
```

#### D. Interactive Error Card (In-Dialog)
```
┌──────────────────────────────────────────────────────────┐
│ ⚠️ Row 4: MCQ requires at least OptionA and OptionB  ✕ Skip│
│ "Which vector represents the rate of change of momentum?"│
│                                                          │
│ [ ⚡ Convert to Short ]              [ ✏️ Fix Options ]    │
└──────────────────────────────────────────────────────────┘
```

---

### 3. CSV Import Blueprint & Ingestion Specifications

| Column Name | Accepted Aliases | Required? | Default Fallback | Purpose & Description |
|---|---|---|---|---|
| `SubjectName` | `subject`, `sub`, `course` | Optional | "General" | Subject hierarchy category |
| `ChapterName` | `chapter`, `chap`, `unit`, `module` | Optional | "General Practice" | Chapter grouping for practice |
| `QuestionType` | `type`, `qtype`, `category`, `tf` | Optional | Auto-detected | MCQ, FIB, TRUE_FALSE, or SHORT_ANSWER |
| `Question` | `questiontext`, `prompt`, `stem`, `q` | **Mandatory** | — | Question prompt or sentence |
| `OptionA..D` | `opta..d`, `a..d`, `choicea..d` | Conditional | Blank | Options (A & B required for MCQ) |
| `CorrectAnswer`| `answer`, `key`, `correct`, `ans` | **Mandatory** | — | Option letter, boolean, or exact text |
| `Explanation` | `solution`, `markingscheme`, `hint` | Optional | Blank | Pedagogical step-by-step reasoning |
| `Difficulty` | `diff`, `level`, `tier` | Optional | "MEDIUM" | EASY, MEDIUM, or HARD |
| `Marks` | `mark`, `score`, `points` | Optional | 1 | Numeric points weight |

**Engine Protections:**
- Auto-strips UTF-8 BOM byte order marks (`\uFEFF`, `\uEFBBBF`).
- Delimiter auto-detection supporting commas (`,`), semicolons (`;`), and tabs (`\t`).
- RFC-4180 state machine preserving embedded line breaks in quoted cells.
- Strips brackets, parentheses, and slashes during header normalization.
- Filters trailing blank lines (`line.all { it.isBlank() }`).

---

### 4. Five Production Hardening Upgrades
1. **App Blocker Resilience & Instant Kick-out**:
   - Synchronizes blocked apps on cold boot and `onServiceConnected()`.
   - Dispatches `performGlobalAction(GLOBAL_ACTION_HOME)` before launching `AppBlockerLockActivity` to instantly dismiss blocked apps.
   - 1000ms per-package debounce prevents event spamming.
2. **Intra-Question PDF Continuation Pagination**:
   - Evaluates vertical height line-by-line in `PdfQuestionPaperGenerator`.
   - Inserts `— Q<Num> (Continued) —` headers when questions span page boundaries.
   - Splits long unbroken mathematical formulas and tokens at the character level to prevent right-margin overflow.
3. **Room Database Transaction Atomicity**:
   - Wrapped multi-table operations across `QuizRepositoryImpl`, `QuestionPaperGenerator`, and `UniversalCsvProcessor` in `database.withTransaction`.
   - 250-row batch chunking prevents SQLite parameter limit exceptions.
4. **Spaced Repetition 04:00 AM Calendar Day Alignment**:
   - Next review intervals align deterministically to 04:00 AM on the target calendar day, eliminating time-of-day creep.
5. **Feynman Audio Walk Focus Hygiene**:
   - Releases audio focus immediately in `onError(error: Int)` and `onDispose`.

---

### 5. Verification & Asset Details
- **APK Package**: `StudyOS-v1.8.0.apk`
- **File Size**: 22.0 MB (23,059,606 bytes)
- **Target OS**: Android 8.0+ (API 26+) through Android 15 (API 35)
- **SHA-256 Checksum**: `7bb7fc8105dd3b90d8687c37a55bc0655f87c859f47474d3369ab5a07e568793`
- **Unit Tests**: 134/134 passing (`./gradlew testDebugUnitTest`)
- **Documentation & Web**: [https://docs-ten-sage.vercel.app/download](https://docs-ten-sage.vercel.app/download)
