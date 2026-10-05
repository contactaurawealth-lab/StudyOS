# 95OS: Agent & Engineering Rules

## 1. Core Engineering Philosophy
95OS is a native, offline-first exam operating system designed to elevate students to a 95%+ target score. We work upon a battle-tested Android codebase. 
**Cardinal Rule:** Do NOT rebuild working features from scratch. Extend, integrate, refactor, and test.

---

## 2. Technology Stack & Environment
- **Language:** Kotlin 2.0.20
- **Android Target:** `compileSdk = 34`, `targetSdk = 34`, `minSdk = 26` (Android 8.0+)
- **UI Toolkit:** Jetpack Compose with Material 3 (Compose BOM 2024.09.02)
- **Local Persistence:** Room 2.6.1 with KSP code generation and SQLite foreign keys enforced
- **Preferences:** AndroidX DataStore Preferences 1.1.1
- **Concurrency & Async:** Kotlin Coroutines 1.8.1, Flow / StateFlow
- **Navigation:** Navigation Compose 2.8.1
- **Architecture:** Clean Architecture (UI → ViewModel → UseCases → Repositories → Room/DataStore)
- **Dependency Injection:** Central manual DI container via `StudyOSAppContainer` hosted on `StudyOSApplication` (No Hilt/Dagger)
- **No Cloud Backend:** Strictly NO Supabase, NO Firebase, NO cloud databases, NO remote authentication.

---

## 3. Architecture & Layering Rules
1. **Unidirectional Data Flow (UDF):** Compose UI observes immutable `StateFlow<UiState>`. User events call ViewModel functions. ViewModels trigger domain UseCases.
2. **Repository Abstraction:** ViewModels never access Room DAOs or DataStore directly. All persistence is mediated through clean Domain Repositories.
3. **Database Integrity:**
   - Always enforce foreign keys: `PRAGMA foreign_keys = ON;`.
   - All schema changes MUST use Room incremental migrations (`MIGRATION_X_Y`) with automated migration verification tests.
   - Do NOT delete or drop existing tables.
4. **Offline Invariant:** Every core loop feature must work seamlessly with Airplane Mode ON, No SIM, No Wi-Fi, No Internet. Network is strictly restricted to optional, user-configured BYOK AI features.
5. **No Feature Duplication:**
   - Extend the existing SM-2 logic into the comprehensive Recall Engine.
   - Extend the existing `AppBlockerAccessibilityService` and `StudyTimer` into Real Exam Mode.
   - Extend the existing `MistakeEntity` / `MistakeBank` into the Connected Mistake Loop.
   - Extend `Subject` / `Chapter` into Syllabus Intelligence with `TopicEntity`.

---

## 4. Naming Conventions
- **Room Entities:** `[Feature]Entity.kt` (e.g., `TopicEntity`, `MistakeEntity`, `PaperEntity`) located in `core/database/entity/`.
- **DAOs:** `[Feature]Dao.kt` located in `core/database/dao/`.
- **Domain Models:** Clean Kotlin data classes without Android/Room annotations in `domain/model/`.
- **Domain Repositories:** Interfaces in `domain/repository/` (e.g., `TopicRepository.kt`), implementations in `data/repository/` (e.g., `OfflineTopicRepository.kt`).
- **Use Cases:** Single-responsibility classes named `[Verb][Noun]UseCase.kt` (e.g., `GenerateExamPaperUseCase.kt`, `Calculate95TargetGapUseCase.kt`) in `domain/usecase/`.
- **ViewModels:** `[Feature]ViewModel.kt` exposing a single `StateFlow<[Feature]UiState>`.
- **UI Screens:** `[Feature]Screen.kt` in `features/[feature]/ui/`.
- **Theme Tokens:** Always use `StudyOSTheme.colors`, `StudyOSTheme.typography`, `StudyOSTheme.shapes`, `StudyOSTheme.spacing`. Never hardcode colors.

---

## 5. Testing Rules
1. Every domain UseCase must have dedicated unit tests under `app/src/test/java/com/studyos/app/domain/`.
2. Every ViewModel must have state flow unit tests using `StandardTestDispatcher`, `runTest`, and fake in-memory repositories.
3. Database migrations must be tested to ensure no loss of existing user data.
4. Run tests before concluding any task:
   `./gradlew testDebugUnitTest`
5. Verify build integrity before concluding any task:
   `./gradlew assembleDebug`

---

## 6. Definition of Done (DoD)
A feature or task is COMPLETE only when:
- [x] Works 100% offline (Airplane Mode tested).
- [x] Data persists correctly in Room/DataStore and survives process termination.
- [x] Existing features continue functioning without regressions.
- [x] Properly integrates with the upstream and downstream data graph (e.g., Mistakes connect to Recall and 95% Engine).
- [x] Empty, loading, and error states are cleanly handled with zero unhandled exceptions.
- [x] Universal CSV import/export operates cleanly for the entity where applicable.
- [x] Automated unit tests pass.
- [x] Documentation (`ARCHITECTURE.md`, `FEATURES.md`, `TODO.md`) is accurately updated.
