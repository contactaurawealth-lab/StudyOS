/**
 * StudyOS — Main Interactive JavaScript Engine
 * Powers the interactive study timer, readiness calculator, mistake bank explorer,
 * spotlight cards, FAQ accordions, and mobile navigation.
 */

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initStudyTimer();
  initReadinessSimulator();
  initMistakeExplorer();
  initFeatureTabs();
  initFaqAccordion();
  initSpotlightEffect();
  initScrollReveal();
  initCopyButtons();
});

/* ==========================================================================
   1. Navbar & Mobile Drawer
   ========================================================================== */
function initNavbar() {
  const navbar = document.querySelector('.navbar');
  const mobileToggle = document.querySelector('.mobile-toggle');
  const drawer = document.querySelector('.mobile-drawer');
  const drawerClose = document.querySelector('.drawer-close');

  if (navbar) {
    window.addEventListener('scroll', () => {
      if (window.scrollY > 20) {
        navbar.classList.add('scrolled');
      } else {
        navbar.classList.remove('scrolled');
      }
    }, { passive: true });
  }

  if (mobileToggle && drawer) {
    mobileToggle.addEventListener('click', () => {
      drawer.classList.add('open');
      document.body.style.overflow = 'hidden';
    });
  }

  if (drawerClose && drawer) {
    drawerClose.addEventListener('click', () => {
      drawer.classList.remove('open');
      document.body.style.overflow = '';
    });
  }

  // Close drawer if user clicks a link inside it
  if (drawer) {
    drawer.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        drawer.classList.remove('open');
        document.body.style.overflow = '';
      });
    });
  }
}

/* ==========================================================================
   2. Interactive Study Timer Widget
   ========================================================================== */
function initStudyTimer() {
  const clockEl = document.getElementById('timerClock');
  const startBtn = document.getElementById('timerStartBtn');
  const resetBtn = document.getElementById('timerResetBtn');
  const progressCircle = document.getElementById('timerProgressCircle');
  const phaseBadge = document.getElementById('timerPhaseBadge');
  const presetChips = document.querySelectorAll('.preset-chip');

  if (!clockEl || !startBtn || !resetBtn || !progressCircle) return;

  const circumference = 2 * Math.PI * 90; // r = 90
  progressCircle.style.strokeDasharray = `${circumference} ${circumference}`;

  let totalSeconds = 25 * 60;
  let remainingSeconds = totalSeconds;
  let isRunning = false;
  let timerInterval = null;

  function updateDisplay() {
    const mins = Math.floor(remainingSeconds / 60);
    const secs = remainingSeconds % 60;
    clockEl.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

    const fraction = remainingSeconds / totalSeconds;
    const offset = circumference - (fraction * circumference);
    progressCircle.style.strokeDashoffset = offset;
  }

  function startTimer() {
    if (isRunning) {
      // Pause
      clearInterval(timerInterval);
      isRunning = false;
      startBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg> Resume`;
      if (phaseBadge) phaseBadge.textContent = 'Paused';
    } else {
      // Start
      isRunning = true;
      startBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="4" width="4" height="16"></rect><rect x="14" y="4" width="4" height="16"></rect></svg> Pause`;
      if (phaseBadge) phaseBadge.textContent = 'Deep Focus';

      timerInterval = setInterval(() => {
        if (remainingSeconds > 0) {
          remainingSeconds--;
          updateDisplay();
        } else {
          clearInterval(timerInterval);
          isRunning = false;
          startBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg> Start`;
          if (phaseBadge) phaseBadge.textContent = 'Complete! 🎉';
          alert('Study Session Complete! Take a 5-minute break.');
        }
      }, 1000);
    }
  }

  function resetTimer() {
    clearInterval(timerInterval);
    isRunning = false;
    remainingSeconds = totalSeconds;
    updateDisplay();
    startBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg> Start`;
    if (phaseBadge) phaseBadge.textContent = 'Focus Session';
  }

  startBtn.addEventListener('click', startTimer);
  resetBtn.addEventListener('click', resetTimer);

  presetChips.forEach(chip => {
    chip.addEventListener('click', () => {
      presetChips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');

      const minutes = parseInt(chip.dataset.minutes, 10) || 25;
      totalSeconds = minutes * 60;
      resetTimer();
    });
  });

  updateDisplay();
}

/* ==========================================================================
   3. Interactive Exam Readiness Simulator
   ========================================================================== */
function initReadinessSimulator() {
  const syllabusInput = document.getElementById('simSyllabus');
  const recallInput = document.getElementById('simRecall');
  const mistakesInput = document.getElementById('simMistakes');
  const proximityInput = document.getElementById('simProximity');

  const syllabusVal = document.getElementById('valSyllabus');
  const recallVal = document.getElementById('valRecall');
  const mistakesVal = document.getElementById('valMistakes');
  const proximityVal = document.getElementById('valProximity');

  const scoreEl = document.getElementById('readinessScore');
  const badgeEl = document.getElementById('readinessBadge');
  const noteEl = document.getElementById('readinessNote');

  if (!syllabusInput || !recallInput || !mistakesInput || !proximityInput || !scoreEl) return;

  function calculate() {
    const s = parseInt(syllabusInput.value, 10);
    const r = parseInt(recallInput.value, 10);
    const m = parseInt(mistakesInput.value, 10);
    const p = parseInt(proximityInput.value, 10);

    syllabusVal.textContent = `${s}%`;
    recallVal.textContent = `${r}%`;
    mistakesVal.textContent = `${m}`;
    proximityVal.textContent = `${p}d`;

    // StudyOS Algorithm:
    // Base score = (Syllabus * 0.35) + (Recall * 0.35) + (Practice/Proximity * 0.30)
    // Mistake Penalty = m * 2.5% (capped at 25%)
    let baseScore = (s * 0.38) + (r * 0.42) + Math.min(20, (30 - Math.min(30, p)) * 0.6);
    let penalty = Math.min(25, m * 2.5);
    let finalScore = Math.max(0, Math.min(100, Math.round(baseScore - penalty)));

    scoreEl.textContent = `${finalScore}%`;

    if (finalScore >= 80) {
      scoreEl.style.color = 'var(--success)';
      badgeEl.textContent = 'EXAM READY';
      badgeEl.style.background = 'rgba(16, 185, 129, 0.15)';
      badgeEl.style.color = '#34D399';
      noteEl.textContent = 'High retention across core chapters. Focus on light mock papers.';
    } else if (finalScore >= 60) {
      scoreEl.style.color = 'var(--accent)';
      badgeEl.textContent = 'NEEDS TARGETED RECALL';
      badgeEl.style.background = 'rgba(245, 158, 11, 0.15)';
      badgeEl.style.color = '#FBBF24';
      noteEl.textContent = 'Clear concept gaps in weak chapters. Review dynamic revision queue.';
    } else {
      scoreEl.style.color = 'var(--error)';
      badgeEl.textContent = 'CRITICAL GAPS';
      badgeEl.style.background = 'rgba(239, 68, 68, 0.15)';
      badgeEl.style.color = '#F87171';
      noteEl.textContent = 'High mistake concentration. Complete a 25m AI study session today.';
    }
  }

  [syllabusInput, recallInput, mistakesInput, proximityInput].forEach(inp => {
    inp.addEventListener('input', calculate);
  });

  calculate();
}

/* ==========================================================================
   4. Interactive Mistake Bank Taxonomy Explorer
   ========================================================================== */
function initMistakeExplorer() {
  const chips = document.querySelectorAll('.mistake-chip');
  const titleEl = document.getElementById('mistakeCategoryTitle');
  const scenarioEl = document.getElementById('mistakeScenario');
  const rootCauseEl = document.getElementById('mistakeRootCause');
  const actionEl = document.getElementById('mistakeAction');

  if (!chips.length || !titleEl) return;

  const taxonomyData = {
    concept: {
      title: 'Concept Gap',
      scenario: 'Confused Electric Potential with Electric Field intensity when solving 3D point charge questions.',
      rootCause: 'Underlying physical theorem was memorized without geometric derivation intuition.',
      action: 'StudyOS AI Study Session prompts you with first-principles derivation before allowing practice problem retry.'
    },
    memory: {
      title: 'Memory Gap',
      scenario: 'Forgot the second constant in the Arrhenius chemical kinetics rate equation during timed quiz.',
      rootCause: 'Decay curve exceeded 7 days without spaced recall reinforcement.',
      action: 'Automatically schedules 3 consecutive prompt cards in your Smart Dynamic Revision Queue.'
    },
    calculation: {
      title: 'Calculation Error',
      scenario: 'Multiplied exponents instead of adding them when simplifying algebraic fractions.',
      rootCause: 'Cognitive speed rushed past sign & algebraic identity verification.',
      action: 'Tags the step with a red calculation marker and requires scratchpad step-by-step re-verification.'
    },
    misread: {
      title: 'Misread Question',
      scenario: 'Solved for maximum height when the problem specifically asked for time of flight.',
      rootCause: 'Skimmed the problem prompt too quickly under exam pressure.',
      action: 'StudyOS prompts you to highlight the exact target variable in yellow before solving.'
    },
    careless: {
      title: 'Careless Mistake',
      scenario: 'Transcribed positive 8 as negative 8 between line 3 and line 4.',
      rootCause: 'Fatigue accumulation after 75 minutes of continuous study without rest.',
      action: 'Study Countdown Timer enforces a 5-minute cognitive reset before repeating similar problem types.'
    }
  };

  chips.forEach(chip => {
    chip.addEventListener('click', () => {
      chips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');

      const cat = chip.dataset.category;
      const data = taxonomyData[cat];
      if (data) {
        titleEl.textContent = data.title;
        scenarioEl.textContent = data.scenario;
        rootCauseEl.textContent = data.rootCause;
        actionEl.textContent = data.action;
      }
    });
  });
}

/* ==========================================================================
   5. Interactive Feature Switcher Tabs
   ========================================================================== */
function initFeatureTabs() {
  const tabBtns = document.querySelectorAll('.tab-btn');
  const tabPanels = document.querySelectorAll('.tab-panel');

  if (!tabBtns.length || !tabPanels.length) return;

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => b.classList.remove('active'));
      tabPanels.forEach(p => p.classList.remove('active'));

      btn.classList.add('active');
      const targetId = btn.dataset.tab;
      const targetPanel = document.getElementById(targetId);
      if (targetPanel) {
        targetPanel.classList.add('active');
      }
    });
  });
}

/* ==========================================================================
   6. FAQ Accordion
   ========================================================================== */
function initFaqAccordion() {
  const faqItems = document.querySelectorAll('.faq-item');

  faqItems.forEach(item => {
    const question = item.querySelector('.faq-question');
    if (!question) return;

    question.addEventListener('click', () => {
      const isOpen = item.classList.contains('open');

      // Close other items
      faqItems.forEach(other => {
        if (other !== item) other.classList.remove('open');
      });

      if (isOpen) {
        item.classList.remove('open');
      } else {
        item.classList.add('open');
      }
    });
  });
}

/* ==========================================================================
   7. Mouse Spotlight Effect on Cards
   ========================================================================== */
function initSpotlightEffect() {
  const cards = document.querySelectorAll('.spotlight-card');

  cards.forEach(card => {
    card.addEventListener('mousemove', e => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      card.style.setProperty('--mouse-x', `${x}px`);
      card.style.setProperty('--mouse-y', `${y}px`);
    });
  });
}

/* ==========================================================================
   8. Scroll Reveal Animations
   ========================================================================== */
function initScrollReveal() {
  const revealElements = document.querySelectorAll('.reveal');

  if (!revealElements.length) return;

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.1 });

  revealElements.forEach(el => observer.observe(el));
}

/* ==========================================================================
   9. Copy-to-Clipboard Buttons
   ========================================================================== */
function initCopyButtons() {
  const copyBtns = document.querySelectorAll('.copy-btn');

  copyBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetText = btn.dataset.copyText;
      if (!targetText) return;

      navigator.clipboard.writeText(targetText).then(() => {
        const originalHtml = btn.innerHTML;
        btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg> Copied!`;
        btn.style.background = 'var(--success)';
        btn.style.borderColor = 'var(--success)';
        btn.style.color = '#000';

        setTimeout(() => {
          btn.innerHTML = originalHtml;
          btn.style.background = '';
          btn.style.borderColor = '';
          btn.style.color = '';
        }, 2200);
      });
    });
  });
}
