package com.studyos.app.core.util

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class KeyboardAction {
    object NewNote : KeyboardAction()
    object OpenAiAssistant : KeyboardAction()
    object ToggleTimerOrFlip : KeyboardAction()
    object SwipeLeft : KeyboardAction()
    object SwipeRight : KeyboardAction()
}

/**
 * Handles physical hardware keyboard shortcuts for tablets, Chromebooks, and external keyboards:
 * - Ctrl + N: Create new note
 * - Ctrl + Space: Quick AI Assistant
 * - Spacebar: Timer start/pause or Flashcard flip
 * - Left Arrow: Flashcard swipe left (wrong)
 * - Right Arrow: Flashcard swipe right (correct)
 */
object KeyboardShortcutManager {

    private val _actions = MutableSharedFlow<KeyboardAction>(extraBufferCapacity = 10)
    val actions: SharedFlow<KeyboardAction> = _actions.asSharedFlow()

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val isCtrlPressed = event.isCtrlPressed || event.isMetaPressed

        // Ctrl + N -> New Note
        if (isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_N) {
            _actions.tryEmit(KeyboardAction.NewNote)
            return true
        }

        // Ctrl + Space -> AI Assistant
        if (isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_SPACE) {
            _actions.tryEmit(KeyboardAction.OpenAiAssistant)
            return true
        }

        // Spacebar without Ctrl -> Toggle timer or flip card
        if (!isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_SPACE) {
            _actions.tryEmit(KeyboardAction.ToggleTimerOrFlip)
            return false // Allow typing if an input field is focused
        }

        // Arrow keys for Flashcards
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            _actions.tryEmit(KeyboardAction.SwipeLeft)
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            _actions.tryEmit(KeyboardAction.SwipeRight)
            return true
        }

        return false
    }
}
