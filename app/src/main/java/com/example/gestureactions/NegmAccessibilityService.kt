package com.example.gestureactions

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Optional, Play-Policy compliant AccessibilityService.
 * Executes explicit global actions (Back, Home, Recents, Notifications) only when invoked
 * by the user's recognized hand gesture. Never reads window content or performs hidden clicks.
 */
class NegmAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally no-op: we never read or collect user screen content.
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instanceRef?.get() === this) {
            instanceRef = null
        }
    }

    fun executeGlobalBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun executeGlobalHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun executeGlobalRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun executeNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    companion object {
        @Volatile
        private var instanceRef: WeakReference<NegmAccessibilityService>? = null

        fun currentInstance(): NegmAccessibilityService? = instanceRef?.get()

        fun isServiceEnabled(context: Context): Boolean {
            if (currentInstance() != null) return true
            val expected = ComponentName(context, NegmAccessibilityService::class.java).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }
}
