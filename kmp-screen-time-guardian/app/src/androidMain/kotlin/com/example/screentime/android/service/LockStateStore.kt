package com.example.screentime.android.service

object LockStateStore {
    private val blockedApps: MutableSet<String> = mutableSetOf()

    fun updateBlockedPackages(packageNames: Set<String>) {
        blockedApps.clear()
        blockedApps.addAll(packageNames)
    }

    fun isBlocked(packageName: String): Boolean = blockedApps.contains(packageName)
}
