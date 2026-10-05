# 95OS — Product Vision & Blueprint

## 1. Product Promise
«A complete offline exam system designed to help students reach their target score, especially 95%+.»

95OS is not another generic productivity app or basic note-taking tool. It is a deterministic, closed-loop academic operating system that connects every student activity into a continuous mastery cycle:
```
Syllabus → Recall → Practice Exam → Result → Lost Marks → Mistakes → Revision → Re-test → Score Progress
```

---

## 2. Product Philosophy & Non-Negotiables
1. **The Closed-Loop Exam System:** Every action must produce actionable data. Every test result must influence what the student studies next. Every lost mark becomes a scheduled opportunity for recovery.
2. **Deterministic Calculations, Not AI Gimmicks:** The 95% Target Engine is built on transparent mathematical modeling. Students must clearly understand why they are projected at 89.2% and see the exact chapters and topics required to close the 5.8% gap.
3. **100% Local & Offline Sovereignty:** 
   «Your study data belongs to you and stays on your device.»
   No cloud databases, no user accounts, no tracking, and no required internet connections.
4. **Physicality in Exam Simulation:** 95OS recognizes that high-stakes examinations are taken with pen and paper. PaperPilot generates genuine printable PDF examination papers. The app locks the device while the student writes the exam on paper.
5. **Separation of Optional AI:** External AI answer evaluation is strictly an optional sidecar workflow. 95OS does not depend on cloud AI to evaluate tests, score marks, or generate papers.

---

## 3. What 95OS Is NOT
- **NOT a generic to-do list:** Tasks exist only in service of the academic syllabus and exam countdowns.
- **NOT an online quiz trivia app:** It simulates real, timed, printable examination papers.
- **NOT a cloud SaaS:** Zero subscription paywalls, zero cloud sync dependencies, zero login friction.
- **NOT a social network:** Zero feeds, zero leaderboards, zero gamified distractions.

---

## 4. Target User Persona
- **High School & University Students:** Preparing for competitive board exams, entrance tests (e.g., CBSE, ICSE, JEE, NEET, SAT, AP, GCSE, A-Levels), and university finals.
- **Driven Achievers (Target 95%+):** Students who already study hard but lose crucial marks to careless errors, forgot formulas, or misread questions, and need a scientific, systematic method to stop bleeding marks.

---

## 5. Core Feature Ecosystem
1. **Syllabus Intelligence:** Granular hierarchy: `Subject → Chapter → Topic`. Tracks status (`Not Started`, `Learning`, `Revised`, `Mastered`), exam weighting, and weakness index.
2. **Recall Engine:** Adaptive active recall rooted in SM-2 spaced repetition, automatically populated by exam mistakes.
3. **PaperPilot:** Offline generation of authentic, printable examination papers from a local question bank, calibrated by syllabus weight and weak topics.
4. **Real Exam Mode:** Device-wide focus lockdown utilizing the native `AccessibilityService` and foreground timer.
5. **Printable PDF Generator:** Creates professional examination papers with school headers, section divisions, marks per question, and printable typography.
6. **Lost Marks System:** Granular post-exam diagnosis categorizing lost marks (e.g., Concept Error, Calculation Error, Careless, Misread, Time Shortage).
7. **Connected Mistake Loop:** Feeds wrong answers directly into active recall queues, tracking time-to-recovery and re-test outcomes.
8. **95% Target Engine:** Transparent mathematical engine projecting total score and prioritizing highest-leverage mark recovery opportunities.
9. **Focus / Regain Layer:** Native foreground timer coupled with distraction blocking and focus statistics.
10. **Universal CSV Engine:** Single unified import/export layer for offline bulk syllabus, question bank, and card management.

---

## 6. UX & Design Principles
- **Aesthetic:** Minimal, academic, serious, premium. Glassmorphic surfaces with restrained warm amber and electric cyan accents.
- **Tone:** "This app is serious about marks."
- **Ergonomics:** 44dp+ minimum touch targets, comprehensive edge-to-edge support, dark mode preservation for late-night study sessions.
- **Zero Clutter:** No intrusive popups, banners, or redundant animations.
