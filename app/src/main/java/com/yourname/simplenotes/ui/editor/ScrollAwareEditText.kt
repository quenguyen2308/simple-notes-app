package com.yourname.simplenotes.ui.editor

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * An EditText that only grabs focus (and summons the keyboard) on a confirmed tap,
 * not on scroll gestures. Solves the UX issue where swiping to read a long note
 * accidentally focuses the field and pops up the IME.
 *
 * Strategy: start with isFocusableInTouchMode = false so any touch doesn't auto-focus.
 * On ACTION_UP, if the gesture never exceeded the touch slop (i.e. it was a tap and
 * not a scroll), re-enable focusable-in-touch-mode right before calling super so that
 * View.onTouchEvent's built-in focus-on-UP logic fires normally. On focus loss,
 * reset back to non-focusable-in-touch so the next scroll doesn't re-focus.
 */
class ScrollAwareEditText(context: Context) : android.widget.EditText(context) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var downY = 0f
    private var scrollDetected = false

    init {
        isFocusableInTouchMode = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                scrollDetected = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scrollDetected && abs(event.y - downY) > touchSlop) {
                    scrollDetected = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!scrollDetected && !hasFocus()) {
                    isFocusableInTouchMode = true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) {
            isFocusableInTouchMode = false
        }
    }
}
