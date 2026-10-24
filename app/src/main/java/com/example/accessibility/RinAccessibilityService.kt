package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class RinAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("RinAccessibilityService", "RIN Accessibility Service connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Safe processing of window events for navigation context
    }

    override fun onInterrupt() {
        Log.w("RinAccessibilityService", "RIN Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun openRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openNotificationsPanel(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettingsPanel(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun clickNodeWithText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        if (!nodes.isNullOrEmpty()) {
            for (node in nodes) {
                if (performClickOnNode(node)) {
                    return true
                }
            }
        }
        return false
    }

    private fun performClickOnNode(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    companion object {
        var instance: RinAccessibilityService? = null
            private set

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${RinAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                if (componentNameString.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }
}
