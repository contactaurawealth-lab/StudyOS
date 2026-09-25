# StudyOS

> A minimal exam-focused study system designed to help students plan, revise, and prepare for exams.

[![Release](https://img.shields.io/badge/Release-v1.3.0%20Exam%20Edition-F59E0B?style=flat-square)](https://github.com/contactaurawealth-lab/studyos-exam/releases/tag/v1.3.0)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Offline--First-10B981?style=flat-square)](#architecture--tech-stack)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

---

## 💡 Overview

StudyOS strips away the social noise, gamification traps, and generic AI bloat of modern study apps. Instead, it provides a quiet, fast, distraction-free environment calibrated around one mission: **acing your exams**.

- **Calm, Academic Design**: Purposeful typography, soft slate and paper surfaces, zero neon or purple gradients.
- **100% Offline-First**: Instant startup, local Room SQLite persistence, zero cloud lock-in.
- **Exam-Centric Flow**: From chapter breakdown to spaced revision, mock testing, and last-minute emergency review.

---

## 🎯 10 High-Value Exam Features

1. **Exam Countdown**
   - Precise live countdowns (e.g., *"23 days until Mathematics"*).
   - Supports multiple simultaneous exams with target dates, syllabus coverage, and priority tags.

2. **Syllabus Completion Tracker**
   - Per-subject visual breakdowns showing Total, Completed, In Progress, and Not Started chapters.
   - Transparent percentage indicators so you always know where you stand.

3. **Weak Chapter Detector**
   - Automatically identifies high-friction chapters based on completion status, revision frequency, and confidence ratings.
   - Clean, non-judgmental `"Needs Attention"` tags without manipulative claims.

4. **Exam Readiness Indicator**
   - Transparent 4-pillar readiness calculation:
     - Syllabus completion (35%)
     - Revision completion (30%)
     - Question practice completion (25%)
     - Self-confidence ratings (10%)
   - Displays real preparedness metrics without false reassurance.

5. **Past Paper & Question Practice Tracker**
   - Track questions attempted, correct, incorrect, overall accuracy rate, and logged practice sessions.
   - Dedicated session logger with time and accuracy analytics.

6. **Mock Test Mode**
   - Full simulated exam environment with subject selection, question count, and live countdown timer.
   - Detailed post-test score report, accuracy breakdown, and automatic recording of question attempts.

7. **Mistake Book**
   - Dedicated repository for questions missed during practice or past papers.
   - Record question/topic, what went wrong, and the correct underlying concept.
   - Filter by subject and track resolution status.

8. **Revision Heatmap**
   - Minimalist calendar grid showing study days, revision sessions, and practice drills.
   - Visual consistency tracking without intrusive streaks or guilt triggers.

9. **Exam Day Mode**
   - High-focus, distraction-free dashboard unlocked as your exam approaches.
   - Displays exam name, countdown, completion checklist, vital formulas, and key notes.

10. **Last-Minute Revision Mode**
    - High-yield rapid revision mode designed for the final 48–72 hours before test day.
    - Aggregates important chapters, weak topics, recorded mistakes, and pending flashcard reviews.

---

## 📱 Core Navigation & Destinations

StudyOS enforces a simple, predictable 5-destination primary hierarchy:

1. **Home (`Today`)**: Today's study plan, upcoming exam countdowns, exam readiness indicators, and revision heatmap.
2. **Subjects**: Academic subject roster, syllabus progress bar, chapters list, and quick "Needs Attention" badges.
3. **Planner**: Daily study tasks, upcoming milestones, priority flags, and estimated study duration.
4. **Revision**: Spaced-revision schedule, chapters due for recall, Blitz sessions, and revision history.
5. **Exams**: Master exam schedules, mock test launcher, exam day mode, and last-minute revision access.

*Settings, backups, and appearance options are cleanly accessed via the top overflow menu.*

---

## 🏛 Architecture & Tech Stack

```
com.studyos.app/
├── core/
│   ├── database/       # Room DB v10 (SQLite offline storage, DAOs, Entities, TypeConverters)
│   ├── datastore/      # Preferences DataStore (User settings, theme state)
│   └── ui/component/   # ExamCountdownCard, SyllabusTrackerCard, ExamReadinessCard,
│                       # PracticeTrackerComponents, RevisionHeatmap, GlassCard, GlassTopBar
├── domain/
│   ├── model/          # Pure immutable Kotlin domain models (Subject, Chapter, Exam, Task, Mistake)
│   └── repository/     # Repository interfaces
├── data/
│   └── repository/     # Offline-first Room repository implementations
├── features/
│   ├── today/          # Today screen, active sessions, and readiness overview
│   ├── subjects/       # Subject list, syllabus tracker, chapter detail, notes
│   ├── planner/        # Study task planning, date filters, task creation
│   ├── practice/       # Practice hub, Flashcards, MockTestScreen, MistakeBankScreen
│   ├── exams/          # Exam list, countdowns, ExamDayModeScreen, LastMinuteRevisionScreen
│   └── settings/       # Dark/light theme, local backup, reset options
└── navigation/         # Type-safe StudyOSNavHost with 5 core destinations & deep sub-routes
```

### Tech Stack Details

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0 (Coroutines, StateFlow, Flow) |
| **UI Framework** | Jetpack Compose + Material 3 Design System |
| **Local Database** | Room Database v10 (Offline-first SQLite) |
| **Settings Storage**| AndroidX DataStore Preferences |
| **Architecture** | Clean Architecture + MVI/MVVM with unidirectional data flow |
| **Design** | Android-native, warm academic palette, light and dark mode |

---

## 📸 Screenshots & Interface

```
┌─────────────────────────────────┐   ┌─────────────────────────────────┐
│ 📅 TODAY                        │   │ 🎯 EXAM DAY MODE                │
│ Mathematics Paper 1 — 23d left  │   │ Physics Final — In 18 Hours     │
│ ─────────────────────────────── │   │ ─────────────────────────────── │
│ Exam Readiness: 78%             │   │ Checklist:                      │
│ [Syllabus: 85% | Recall: 75%]   │   │  ☑ Formula sheet reviewed       │
│                                 │   │  ☑ Waves & Optics flashcards    │
│ Today's Study Plan:             │   │  ☐ Thermodynamics mistakes      │
│  ☐ Calculus: Integrals (45m)    │   │                                 │
│  ☐ Linear Algebra Quiz (30m)    │   │ Important Notes:                │
│                                 │   │  • Remember boundary conditions │
│ Revision Heatmap: ■ ■ ▨ ■ ▨ ■   │   │  • Check signs in work integral │
└─────────────────────────────────┘   └─────────────────────────────────┘
```

---

## 🛠 Installation & Development

### Prerequisites

- Android Studio Koala / Ladybug or newer
- JDK 17 or JDK 21
- Android SDK 34+

### Clone & Setup

```bash
git clone https://github.com/contactaurawealth-lab/studyos-exam.git
cd studyos-exam
```

### Build Commands

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌐 Web Landing Page & Deployment

StudyOS includes a dedicated, responsive web landing page in the `web/` directory.

### Live Production Deployment
- **Vercel Edge**: [https://studyos-exam-workspace.vercel.app](https://studyos-exam-workspace.vercel.app)
- **GitHub Pages**: [https://contactaurawealth-lab.github.io/studyos-exam/](https://contactaurawealth-lab.github.io/studyos-exam/)

### Local Development
```bash
cd web
python3 -m http.server 3000
# Open http://localhost:3000
```

### Production Deployment (Vercel)
```bash
cd web
vercel --prod
```

---

## 🔐 Environment & Privacy

- **Zero Cloud API Keys Required**: StudyOS operates 100% locally on your device.
- **Data Privacy**: No telemetry, no third-party tracking scripts, and no external AI services consuming your study notes.
- **Data Safety**: All study records, mistake logs, and practice sessions remain strictly in local SQLite storage.

---

## 📄 License

StudyOS is open-source software licensed under the [MIT License](LICENSE).
Feel free to use, modify, and distribute for academic and personal productivity.
