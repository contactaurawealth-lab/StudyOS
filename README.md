# StudyOS 🎓

> **The Personal Study Operating System for Android**  
> A minimal, distraction-free study workspace built with Kotlin and Jetpack Compose, featuring glassmorphism surfaces, context-aware AI learning, spaced repetition, and an integrated practice hub.

[![Release](https://img.shields.io/badge/Release-v1.1.0%20Stable-F59E0B?style=flat-square)](https://github.com/contactaurawealth-lab/StudyOS/releases/tag/v1.1.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Notifications](https://img.shields.io/badge/Notifications-100%25%20Local%20%26%20Free-10B981?style=flat-square)](#6--free-native-local-notifications--study-reminders)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Offline--First-22C55E?style=flat-square)](#architecture)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

---

## 📥 Direct APK Download

Download the latest signed release Android APK directly:

[![Download StudyOS APK](https://img.shields.io/badge/Download-StudyOS%20v1.1.0%20APK%20(12.7%20MB)-F59E0B?style=for-the-badge&logo=android&logoColor=white)](docs/downloads/StudyOS-v1.1.0.apk)

- **File Name:** [`StudyOS-v1.1.0.apk`](docs/downloads/StudyOS-v1.1.0.apk) *(Also symlinked as `StudyOS.apk`)*
- **File Size:** `12.7 MB`
- **Supported Architectures:** `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` (Universal APK)
- **Target OS:** Android 8.0 (API 26) through Android 14+
- **SHA-256 Checksum:** `0aa7730334562b0c5ae8f2ed86568d8c9788dc44cd51bb107931f79676f44d17`
- **GitHub Release:** [v1.1.0 — Local Notifications & Revision Edition](https://github.com/contactaurawealth-lab/StudyOS/releases/tag/v1.1.0)

> 🌐 **Live Web Experience**:
> - **[https://studyos-workspace.vercel.app](https://studyos-workspace.vercel.app)** *(Global Production Vercel Edge)*
> - **[https://studyos-android.vercel.app](https://studyos-android.vercel.app)** *(Alternative Mirror)*
> - **[https://contactaurawealth-lab.github.io/StudyOS/](https://contactaurawealth-lab.github.io/StudyOS/)** *(GitHub Pages)*

---

## 📱 Quick Installation Options

### Option 1: Standard Android Install
1. Tap the **[Download APK (v1.1.0)](docs/downloads/StudyOS-v1.1.0.apk)** link directly on your phone or computer.
2. In your phone's File Manager, open the **Download** folder.
3. Tap **`StudyOS-v1.1.0.apk`** and tap **Install** *(Allow installation from this source if prompted by Android)*.

### Option 2: Termux (Direct Terminal Install)
```bash
# Copy to Android Download folder
cp /root/StudyOS/docs/downloads/StudyOS-v1.1.0.apk /sdcard/Download/StudyOS-v1.1.0.apk

# Or launch system package installer directly
termux-open /root/StudyOS/docs/downloads/StudyOS-v1.1.0.apk
```

### Option 3: ADB
```bash
adb install -r docs/downloads/StudyOS-v1.1.0.apk
```

---

## ✨ Design Philosophy & Aesthetic

StudyOS adopts a calm, high-efficiency **"study workspace"** aesthetic rather than a generic school dashboard.
- **Glassmorphic Surfaces**: Translucent layers (`GlassSurface`, `GlassCard`) with delicate 1dp borders and subtle backdrop elevation.
- **Calm Palette**: Crisp graphite typography on paper/slate backgrounds with warm amber accents. Zero purple or harsh neon gradients.
- **Micro-Interactions**: Tactile spring press feedback (`bounceClick`), non-intrusive haptic confirmations on completions, and smooth 450ms progress transitions.
- **Ergonomic Touch Targets**: All interactive elements enforce $\ge 44\text{dp}\times 44\text{dp}$ touch targets with semantic content descriptions.
- **Responsive Layout**: Seamlessly adapts from compact smartphones (floating glass bottom bar + modal drawer) to tablets (adaptive navigation rail).

---

## 🔄 The Continuous Mastery Loop

Features in StudyOS form a connected cycle where practice, recall, and mistakes actively strengthen conceptual understanding:

```
┌────────────────────────────────────────────────────────┐
│               LEARN SUBJECT & CHAPTER                  │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                   MAKE NOTES (MD)                      │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│              CREATE FLASHCARDS (Leitner)               │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                 PRACTICE QUIZZES                       │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│          MISTAKE BANK (Resolve & Auto-Convert)         │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│         ACTIVE RECALL (5 / 10 / 15-min Blitz)          │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                    CHAPTER MASTERY                     │
└────────────────────────────────────────────────────────┘
```

---

## 🚀 Core Features

### 1. 🏠 Today Workspace & Focus Screen
- **Minimal Greeting & Date**: Compact modern header with quick access drawer (`☰`) and overflow menu (`⋮`).
- **Urgent Alerts**: Exam countdown cards and spaced-repetition due alerts.
- **Today's Focus**: Intelligent detection of the next scheduled session or recommended chapter.
- **Tasks & Progress**: Daily goal progress bar and interactive task checklists.

### 2. 📚 Academic Hierarchy & Chapter Practice Hub
- **2-Digit Chapter Numbering**: Structured chapter indexes (`01`, `02`, `03`) with live progress percentages.
- **Unified Chapter Practice Hub**:
  - **Notes**: Markdown-enabled notes editor.
  - **Flashcards**: Active recall cards with Again/Hard/Good/Easy spaced-repetition ratings.
  - **Quizzes**: Timed, multiple-choice quizzes with mistake tracking.
  - **Mistake Bank**: Captures failed questions and converts them into review flashcards.

### 3. 🧠 Context-Aware AI Study Engine
- **Chapter-Aware Doubt Solver**: Receives chapter context, progress percentage, and weak topics.
- **Adaptive Modes**:
  - *"Teach Me"* (guided Socratic questions)
  - *Explain Simply* (ELI5)
  - *Generate Practice Questions* (Easy, Medium, Hard)
- **Streaming Response UI**: Clean markdown rendering with equations, code blocks, retry, copy, and stop generation.

### 4. 🔁 Active Recall & Spaced Repetition (Phase 11)
- **Memory Decay Prevention**: Intelligent scheduling based on student accuracy and Leitner boxes.
- **Timed Blitz Sessions**: Quick 5, 10, or 15-minute recall sessions.
- **Weak-Topic Queue**: Prioritizes topics with lowest historical retention.

### 5. 📊 Academic Progress Engine
- **Smooth Numerical Easing**: Animated counter transitions (`42% → 43%`) over 500ms.
- **Subject Breakdowns**: Chapter counts, completion ratios, and visual progress bars.

### 6. 🔔 Free Native Local Notifications & Study Reminders
- **100% Free & Local**: Zero Firebase Cloud Messaging (FCM), zero OneSignal, zero third-party accounts. Runs directly on the device.
- **Reliable Scheduling**: Uses Android-native `AlarmManager.setAndAllowWhileIdle()` to reliably trigger even in Android Doze mode.
- **Reboot Persistence**: Broadcast receiver (`BootReceiver`) listens for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, and time/timezone shifts to immediately restore all upcoming study sessions and daily reminders.
- **Interactive Deep Links**: Notifications take you directly into the planned subject, chapter, or session using `singleTop` intent routing.
- **Android 13+ Educational Flow**: Explains why notifications are helpful before prompting for runtime `POST_NOTIFICATIONS` permission.
- **Glassmorphic Notification Hub**: Accessible right from Settings with instant preview toggles, daily reminder time picker, and system permission recovery alerts.

---

## 🏛 Architecture & Tech Stack

```
com.studyos.app/
├── core/
│   ├── database/       # Room DB v7 (Mappers, Entities, DAOs)
│   ├── datastore/      # Preferences DataStore (Settings & Reminders)
│   ├── notification/   # AlarmScheduler, BootReceiver, NotificationManager, AlarmReceiver
│   └── ui/component/   # GlassCard, GlassTopBar, GlassBottomBar, Shimmer, etc.
├── domain/
│   ├── model/          # Pure immutable Kotlin domain models
│   ├── repository/     # Repository contracts
│   └── usecase/        # Clean single-responsibility use cases
├── data/
│   └── repository/     # Offline-first repository implementations
├── features/
│   ├── today/          # Today screen & study session runner
│   ├── subjects/       # Subject list, subject details, chapter screen
│   ├── practice/       # Notes, Flashcards, QuizRunner, MistakeBank, Revision
│   ├── ai/             # AI chat assistant & practice generator
│   ├── progress/       # Academic progress analytics
│   ├── planner/        # Calendar & session planner
│   ├── exams/          # Exam countdowns & revision tracking
│   └── settings/       # Preferences, Notifications, & theme toggles
├── navigation/         # StudyOSNavHost, destinations & deep-link intent routing
└── theme/              # Color, Shape, Spacing, Typography & Theme
```

| Layer / Tool | Technology |
|---|---|
| **Language** | Kotlin 2.0 (Coroutines, StateFlow, Flow) |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Local Database** | Room Database v7 (100% offline-first SQLite) |
| **Notifications** | Android AlarmManager + NotificationManager (100% local) |
| **Data Storage** | AndroidX DataStore Preferences |
| **Design Tokens** | Custom Glassmorphism, 8-pt Spacing tokens, Material 3 shapes |
| **Testing** | JUnit 4, Kotlinx Coroutines Test, Room Testing |

---

## 🛠 Building from Source

### Prerequisites
- Android Studio Ladybug / Koala or CLI with JDK 17+
- Android SDK 34+

### Clone & Build
```bash
git clone https://github.com/StudyOS/StudyOS.git
cd StudyOS

# Run all unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Output will be generated at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

StudyOS is licensed under the [MIT License](LICENSE).
Feel free to use, modify, and distribute this software for personal or academic purposes.
