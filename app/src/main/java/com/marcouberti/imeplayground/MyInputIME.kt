package com.marcouberti.imeplayground

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

/**
 * This class sets up and handles virtual keyboard events
 */
class MyInputIME : InputMethodService(), OnKeyboardActionListener {

    private lateinit var keyboard: Keyboard
    private lateinit var keyboardView: KeyboardView
    private val DEFAULT_HEIGHT = 250

    // called initially when inflating keyboard
    override fun onCreateInputView(): View {
        val layout = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = layout.findViewById(R.id.keyboard_view)
        val ctx = layout.context

        keyboard = MyKeyboard(ctx, R.xml.keyboard_qwerty, DEFAULT_HEIGHT)

        keyboard.isShifted = false
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.invalidateAllKeys()

        return layout
    }


    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point keyboard is
     * bound to client, and is now receiving all of the detailed information
     * about the target of edits.
     */
    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
    }

    /**
     * Reload user preferences every time keyboard is inflated
     * as these preferences may have changed
     */
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
    }

    /**
     * Handle keyboard key presses; if key is held down this fires multiple times
     */
    override fun onKey(primaryCode: Int, keyCodes: IntArray) {
        currentInputConnection?.commitText(primaryCode.toChar().toString(), 0)
    }

    /**
     * Fired each time key is pressed; if key is held down this fires only once
     */
    override fun onPress(i: Int) {

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    override fun onRelease(i: Int) {}

    override fun onText(charSequence: CharSequence) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

}