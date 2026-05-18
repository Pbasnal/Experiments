package com.example.screentime.shared.domain

import com.example.screentime.shared.model.AppLimitConfig
import com.example.screentime.shared.model.AppUsageSnapshot
import com.example.screentime.shared.model.RestrictionDecision

class ScreenTimePolicyEngine {
    fun evaluate(
        config: List<AppLimitConfig>,
        usage: List<AppUsageSnapshot>
    ): List<RestrictionDecision> {
        val usageByPackage = usage.associateBy { it.packageName }
        return config.filter { it.isEnabled }.map { appConfig ->
            val minutesUsed = usageByPackage[appConfig.packageName]?.usedMinutesToday ?: 0
            val remaining = (appConfig.dailyLimitMinutes - minutesUsed).coerceAtLeast(0)
            RestrictionDecision(
                packageName = appConfig.packageName,
                shouldLock = minutesUsed >= appConfig.dailyLimitMinutes,
                remainingMinutes = remaining
            )
        }
    }
}
