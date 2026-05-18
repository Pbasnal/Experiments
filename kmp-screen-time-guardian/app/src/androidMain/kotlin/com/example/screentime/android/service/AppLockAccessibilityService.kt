package com.example.screentime.android.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.screentime.android.ui.LockActivity

class AppLockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val openedPackage = event?.packageName?.toString() ?: return
        if (!LockStateStore.isBlocked(openedPackage)) return

        val lockIntent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(LockActivity.EXTRA_BLOCKED_PACKAGE, openedPackage)
        }
        startActivity(lockIntent)
    }

    override fun onInterrupt() = Unit
}
