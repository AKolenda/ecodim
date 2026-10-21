package com.ecodim.app

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Draws the dimming layer. An accessibility overlay sits above the status bar, notification shade
 * and navigation bar, which an ordinary app overlay can't reach. It never reads screen content.
 */
class DimService : AccessibilityService() {

    private lateinit var scope: CoroutineScope
    private lateinit var windowManager: WindowManager
    private var overlay: View? = null
    private var connected = false

    // Whether the clock was inside the schedule window at the last check, or null before the first
    // check. The schedule only acts when this flips, so manual on/off in between is respected.
    private var lastInWindow: Boolean? = null

    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = checkSchedule()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WindowManager::class.java)
        scope = MainScope()

        val clockFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(this, clockReceiver, clockFilter, ContextCompat.RECEIVER_NOT_EXPORTED)

        scope.launch {
            combine(Dimmer.active, Dimmer.level, Dimmer.warmth, ::Triple).collect { (on, level, warmth) ->
                render(on, level, warmth)
            }
        }
        scope.launch {
            Dimmer.schedule.collect {
                lastInWindow = null
                checkSchedule()
            }
        }
        connected = true
        Dimmer.setServiceRunning(true)
    }

    private fun checkSchedule() {
        val schedule = Dimmer.schedule.value
        if (!schedule.enabled) {
            lastInWindow = null
            return
        }
        val inWindow = schedule.containsNow()
        val previous = lastInWindow
        lastInWindow = inWindow
        when {
            // First check (service start or schedule edited): switch on if we're inside the window,
            // but don't force it off outside, in case the user turned it on by hand.
            previous == null -> if (inWindow) Dimmer.setActive(true)
            previous != inWindow -> Dimmer.setActive(inWindow)
        }
    }

    private fun render(on: Boolean, level: Float, warmth: Float) {
        if (on) {
            val view = overlay ?: addOverlay().also { overlay = it }
            view.setBackgroundColor(overlayColor(warmth))
            view.animate().cancel()
            view.animate().alpha(level).setDuration(FADE_MS).start()
            Notifications.showActive(this, Dimmer.levelPercent)
        } else {
            overlay?.let { view ->
                overlay = null
                view.animate().cancel()
                view.animate().alpha(0f).setDuration(FADE_MS).withEndAction { removeOverlay(view) }.start()
            }
            Notifications.cancel(this)
        }
    }

    private fun addOverlay(): View {
        val view = View(this).apply { alpha = 0f }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                // Don't shrink to avoid the system bars; covering them is the point.
                fitInsetsTypes = 0
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        windowManager.addView(view, params)
        return view
    }

    private fun removeOverlay(view: View) {
        if (view.isAttachedToWindow) windowManager.removeView(view)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        tearDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    private fun tearDown() {
        if (!connected) return
        connected = false
        scope.cancel()
        runCatching { unregisterReceiver(clockReceiver) }
        overlay?.let { view ->
            view.animate().cancel()
            removeOverlay(view)
        }
        overlay = null
        Notifications.cancel(this)
        Dimmer.setServiceRunning(false)
    }

    private companion object {
        const val FADE_MS = 220L

        /** Black, blended toward deep amber as warmth rises, for a night-light look. */
        fun overlayColor(warmth: Float): Int =
            Color.rgb((150 * warmth).toInt(), (62 * warmth).toInt(), 0)
    }
}
