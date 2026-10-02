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
 * An EditText optimized for smooth note-taking:
 * - Natural tap-to-focus and immediate keyboard summoning on tap without lag or missed touches.
 * - Swiping to scroll through a note dismisses the soft keyboard and clears focus, allowing
 *   unobstructed full-screen reading.
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

    private val syncRunnable = Runnable {
        syncPending = false
        val spannable = text as? Spannable ?: return@Runnable
        val html = HtmlSpannableConverter.spannableToHtml(spannable)
        onHtmlSyncCallback?.invoke(html)
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        showSoftInputOnFocus = true
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
                if (!scrollDetected && (dx > touchSlop || dy > touchSlop)) {
                    scrollDetected = true
                    if (hasFocus()) {
                        // Dismiss soft keyboard when user scrolls, expanding readable screen space
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(windowToken, 0)
                        clearFocus()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!scrollDetected && !hasFocus()) {
                    requestFocus()
                    val offset = getOffsetForPosition(event.x, event.y)
                    if (offset >= 0 && text != null) {
                        Selection.setSelection(text, offset.coerceIn(0, text.length))
                    }
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) {
            flushPendingHtml()
        }
    }

    override fun onDetachedFromWindow() {
        flushPendingHtml()
        mainHandler.removeCallbacks(syncRunnable)
        super.onDetachedFromWindow()
    }
}
