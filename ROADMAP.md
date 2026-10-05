# StudyOS — Roadmap

## Current Status: Phase 13 (Polish)

The project has completed core feature development through Phase 12 and is in the polish/enhancement phase.

---

## PHASE 0 — Project Foundation ✅

**Goal:** Set up a properly structured native Android project with Kotlin, Jetpack Compose, and Material 3.

### Implementation Tasks
- [x] Create Android project with Kotlin DSL Gradle
- [x] Configure Jetpack Compose with BOM
- [x] Configure Material 3
- [x] Set up KSP for Room compiler
- [x] Configure minSdk 26, targetSdk 34
- [x] Create package structure (core, data, domain, features, navigation, theme)
- [x] Set up edge-to-edge rendering

### Acceptance Criteria
- [x] `./gradlew assembleDebug` produces APK
- [x] Empty app launches on device/emulator
- [x] Package name: `com.studyos.app`

---

## PHASE 1 — Design System ✅

**Goal:** Create a centralized Material 3 theme system with custom design tokens.

### Implementation Tasks
- [x] Define color palettes (light/dark)
- [x] Define typography scale
- [x] Define shape tokens
- [x] Define spacing tokens
- [x] Create `StudyOSTheme` composable
- [x] Support System/Light/Dark modes
- [x] Support custom accent colors
- [x] Create reusable UI components (buttons, dialogs, text fields, states, layouts)
- [x] Create glass-morphism components

### Acceptance Criteria
- [x] Theme switches cleanly between light/dark/system
- [x] Custom accent color applies across the app
- [x] No hardcoded colors in screens
- [x] Consistent spacing and typography

---

## PHASE 2 — Onboarding ✅

**Goal:** Create a polished native onboarding flow for first-time users.

### Implementation Tasks
- [x] Welcome screen with app introduction
- [x] Profile step (name/nickname, no login required)
- [x] Subjects step (add custom subjects with suggestions)
- [x] Preferences step (daily goal, pomodoro duration, break duration)
- [x] Review step (summary before entering home)
- [x] Onboarding completion state (DataStore)
- [x] OnboardingViewModel with state management
- [x] AddSubjectBottomSheet

### Acceptance Criteria
- [x] First launch shows onboarding
- [x] Subsequent launches go directly to home
- [x] Profile, subjects, preferences persist via Room/DataStore
- [x] Killing and reopening app preserves onboarding completion

---

## PHASE 3 — Home Dashboard ✅

**Goal:** Create a polished home dashboard that adapts to user data.

### Implementation Tasks
- [x] Greeting with user's name
- [x] Daily progress bar (time studied vs. goal)
- [x] Today's tasks with checkboxes
- [x] Planned study sessions
- [x] Quick action buttons
- [x] Empty states for each section
- [x] TodayViewModel with aggregated data

### Acceptance Criteria
- [x] Dashboard shows real data from Room
- [x] Progress updates in real-time
- [x] Empty states guide users to first action
- [x] Task completion toggles persist

---

## PHASE 4 — Subjects ✅

**Goal:** Full subject management with chapter hierarchy.

### Implementation Tasks
- [x] Subject list screen
- [x] Subject detail screen with chapters
- [x] Chapter detail screen (practice hub)
- [x] Add/edit/delete subjects
- [x] Add/edit/delete chapters within subjects
- [x] Chapter reordering
- [x] Chapter progress tracking
- [x] Chapter status management (Not Started, In Progress, Completed)

### Acceptance Criteria
- [x] CRUD operations on subjects persist
- [x] Chapters display under correct subjects
- [x] Chapter progress updates correctly
- [x] Deleting a subject cascades to chapters

---

## PHASE 5 — Notes ✅

**Goal:** Native note management linked to chapters.

### Implementation Tasks
- [x] Note editor screen (create/edit)
- [x] Notes list within chapter practice hub
- [x] Markdown-style rendering
- [x] Pin/unpin notes
- [x] Delete notes
- [x] Search notes

### Acceptance Criteria
- [x] Notes persist in Room
- [x] Notes are linked to chapters
- [x] Editor supports basic formatting
- [x] Notes appear in search results

---

## PHASE 6 — Tasks & Planner ✅

**Goal:** Task management with planner views.

### Implementation Tasks
- [x] Task creation with title, description, subject, due date, priority
- [x] Task list screen with filters (Today, Upcoming, Completed)
- [x] Task completion toggle
- [x] Edit task bottom sheet
- [x] Planner with day/week views
- [x] Study session planning
- [x] Session drag-and-drop/move
- [x] Week calendar row navigation

### Acceptance Criteria
- [x] Tasks persist and filter correctly
- [x] Planner shows sessions on correct dates
- [x] Tasks appear on home dashboard
- [x] Due dates work with native date pickers

---

## PHASE 7 — Study Timer ✅

**Goal:** Fully local study timer with Pomodoro support.

### Implementation Tasks
- [x] Pomodoro timer (work/break cycles)
- [x] Custom duration timer
- [x] Start, pause, resume, reset
- [x] Session completion → save to Room
- [x] Subject/chapter selection for sessions
- [x] Foreground service for timer persistence
- [x] Ambient audio bar
- [x] Coffee time overlay (break screen)
- [x] Progress update on session finish

### Acceptance Criteria
- [x] Timer continues across configuration changes
- [x] Timer survives app backgrounding (foreground service)
- [x] Completed sessions appear in progress
- [x] No server dependency

---

## PHASE 8 — Flashcards ✅

**Goal:** Local flashcard system with spaced repetition.

### Implementation Tasks
- [x] Flashcard creation (question/answer)
- [x] Flashcard study interface (tap to reveal)
- [x] Difficulty rating (Again, Hard, Good, Easy)
- [x] Spaced repetition algorithm (SM-2 variant)
- [x] Flashcard review tracking
- [x] Due flashcard calculation
- [x] Flashcard import (CSV/text)

### Acceptance Criteria
- [x] Flashcards persist in Room
- [x] Review history tracks correctly
- [x] Due cards surface at appropriate intervals
- [x] No internet required

---

## PHASE 9 — Progress ✅

**Goal:** Progress tracking calculated from actual data.

### Implementation Tasks
- [x] Study time tracking (daily, weekly)
- [x] Completed tasks count
- [x] Chapter completion percentage
- [x] Subject-level progress aggregation
- [x] Academic progress overview
- [x] Progress screen with visualizations
- [x] Empty state when no data

### Acceptance Criteria
- [x] All statistics derive from real Room data
- [x] No fabricated numbers
- [x] Progress updates when sessions/tasks complete
- [x] Empty state guides user to start studying

---

## PHASE 10 — Search ✅

**Goal:** Local search across all content.

### Implementation Tasks
- [x] Search screen with text input
- [x] Search subjects
- [x] Search chapters
- [x] Search notes
- [x] Search tasks
- [x] Search flashcards
- [x] Results categorized by type
- [x] Navigation to result item

### Acceptance Criteria
- [x] Search queries Room database directly
- [x] Results appear instantly (no network)
- [x] Tapping result navigates to correct screen
- [x] Empty search state is informative

---

## PHASE 11 — Settings ✅

**Goal:** Comprehensive settings management.

### Implementation Tasks
- [x] Appearance settings (theme, accent color)
- [x] Study preferences (daily goal, pomodoro/break duration)
- [x] Profile editing
- [x] Subject management (add/rename/delete)
- [x] Data management section
- [x] About section

### Acceptance Criteria
- [x] Settings persist via DataStore/Room
- [x] Theme changes apply immediately
- [x] Study preferences reflect in timer/progress

---

## PHASE 12 — Backup / Restore ✅

**Goal:** Local data export and import.

### Implementation Tasks
- [x] JSON backup export via Storage Access Framework
- [x] Backup import with data restoration
- [x] FileProvider for secure backup sharing
- [x] Backup encryption support

### Acceptance Criteria
- [x] User can save backup to device storage
- [x] Import restores data correctly
- [x] No uploads to any server
- [x] Backup format is JSON-based

---

## PHASE 13 — Polish 🔄 (Current)

**Goal:** Refine UI, improve empty states, add finishing touches.

### Implementation Tasks
- [x] Notification system (study reminders)
- [x] Home screen widgets (Daily Plan, Exam Countdown, Focus Timer)
- [x] App shortcuts
- [x] Document viewer/sharing
- [x] Keyboard shortcuts
- [x] Quiz system
- [x] Mistake bank
- [x] Active recall engine
- [x] Revision scheduling
- [x] Exam readiness engine
- [x] AI tutor (optional, user-provided API key)
- [x] App blocker during focus sessions
- [x] Knowledge graph visualization
- [ ] Enhanced empty states across all screens
- [ ] Improved error messages (no stack traces exposed)
- [ ] Smooth animations throughout
- [ ] Tablet-optimized layouts
- [ ] Accessibility audit

### Acceptance Criteria
- [ ] Every major feature has useful empty state
- [ ] No technical errors exposed to user
- [ ] UI looks polished on small/large phones and tablets
- [ ] Animations are smooth and purposeful

---

## PHASE 14 — Testing 🔲

**Goal:** Comprehensive test coverage.

### Implementation Tasks
- [x] Unit tests for core use cases (subjects, chapters, tasks, progress)
- [x] ViewModel tests (onboarding, search, subject detail, progress, tasks)
- [x] Database integrity tests
- [x] Backup/encryption tests
- [ ] UI tests with Compose testing framework
- [ ] Integration tests for critical flows
- [ ] Edge case testing (empty DB, corrupted data)
- [ ] Dark/light theme visual verification
- [ ] Configuration change survival testing

### Acceptance Criteria
- [ ] All critical paths have test coverage
- [ ] Tests pass on `./gradlew test`
- [ ] No regressions in core functionality

---

## PHASE 15 — APK Release 🔲

**Goal:** Production-ready APK.

### Implementation Tasks
- [x] Debug APK builds successfully
- [x] Release APK builds with debug signing
- [ ] ProGuard/R8 minification (currently disabled)
- [ ] App icon finalized
- [ ] Version name/code updated
- [ ] Privacy policy finalized
- [ ] Final lint pass
- [ ] Performance profiling

### Acceptance Criteria
- [x] `./gradlew assembleDebug` succeeds
- [ ] `./gradlew assembleRelease` with proper signing
- [ ] APK size is reasonable
- [ ] No lint errors at error severity
- [ ] App performs well on mid-range devices

---

## Future Phases (Post-MVP)

### PHASE 16 — Enhanced Study Intelligence
- Advanced spaced repetition tuning
- Study pattern analysis
- Personalized recommendations engine
- Weekly/monthly study reports

### PHASE 17 — Collaboration (Optional)
- Export/share flashcard decks
- Export study plans
- QR code sharing between devices

### PHASE 18 — Cloud Sync (Optional, Opt-in)
- User-controlled cloud backup
- Multi-device sync
- End-to-end encryption
- No vendor lock-in

### PHASE 19 — Platform Expansion
- Wear OS companion
- Desktop app (Kotlin Multiplatform)
- Tablet-first layouts

---

*Last updated: October 2026*
