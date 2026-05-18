package com.example.screentime.shared.model

import kotlinx.datetime.LocalTime

data class AppLimitConfig(
    val packageName: String,
    val dailyLimitMinutes: Int,
    val isEnabled: Boolean = true
)

data class AppUsageSnapshot(
    val packageName: String,
    val usedMinutesToday: Int,
    val capturedAt: LocalTime
)

data class RestrictionDecision(
    val packageName: String,
    val shouldLock: Boolean,
    val remainingMinutes: Int
)
