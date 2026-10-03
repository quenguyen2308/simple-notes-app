package com.yourname.simplenotes.ui.editor

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.text.Selection
import android.text.Spannable
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.InputMethodManager
import com.yourname.simplenotes.util.HtmlSpannableConverter
import kotlin.math.abs

/**
 * An EditText optimized for smooth note reading and editing:
 * - Starts with `isFocusableInTouchMode = false` so gestures intended for scrolling/reading
 *   never accidentally request focus or summon the soft keyboard on ACTION_DOWN.
 * - Only enables focusable-in-touch-mode on confirmed tap gestures (not scroll gestures).
 * - Swiping to scroll through a note dismisses the soft keyboard and clears focus, allowing
 *   unobstructed full-screen reading.
 * - Resets `isFocusableInTouchMode = false` whenever focus is lost or scrolling is detected.
 * - Debounces HTML serialization and Compose state updates to keep typing fluid and drop-frame free.
 */
class ScrollAwareEditText(context: Context) : android.widget.EditText(context) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var downX = 0f
    private var downY = 0f
    private var scrollDetected = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var syncPending = false
    private var onHtmlSyncCallback: ((String) -> Unit)? = null

    /** The last HTML string synced to ViewModel or loaded externally. */
    var lastSyncedHtml: String? = null

    private val syncRunnable = Runnable {
        syncPending = false
        val spannable = text as? Spannable ?: return@Runnable
        val html = HtmlSpannableConverter.spannableToHtml(spannable)
        lastSyncedHtml = html
        onHtmlSyncCallback?.invoke(html)
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = false
    }

    fun setOnHtmlSyncListener(callback: (String) -> Unit) {
        onHtmlSyncCallback = callback
    }

    fun scheduleHtmlSync(delayMs: Long = 400L) {
        syncPending = true
        mainHandler.removeCallbacks(syncRunnable)
        mainHandler.postDelayed(syncRunnable, delayMs)
    }

    fun flushPendingHtml() {
        if (syncPending) {
            mainHandler.removeCallbacks(syncRunnable)
            syncRunnable.run()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                scrollDetected = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - downX)
                val dy = abs(event.y - downY)
                // Require significant vertical movement (not horizontal text selection or tiny jitter)
                if (!scrollDetected && dy > touchSlop * 2.5f && dy > dx * 1.5f) {
                    scrollDetected = true
                    if (hasFocus()) {
                        // Dismiss soft keyboard when user scrolls, expanding readable screen space
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(windowToken, 0)
                        clearFocus()
                    }
                    isFocusableInTouchMode = false
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
            flushPendingHtml()
        }
    }

    override fun onDetachedFromWindow() {
        flushPendingHtml()
        mainHandler.removeCallbacks(syncRunnable)
        super.onDetachedFromWindow()
    }
}
