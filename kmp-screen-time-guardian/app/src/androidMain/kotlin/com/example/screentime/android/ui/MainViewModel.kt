package com.example.screentime.android.ui

import androidx.lifecycle.ViewModel
import com.example.screentime.android.data.UsageStatsReader
import com.example.screentime.android.service.LockStateStore
import com.example.screentime.shared.domain.ScreenTimePolicyEngine
import com.example.screentime.shared.model.AppLimitConfig
import com.example.screentime.shared.model.RestrictionDecision

data class MainUiState(
    val decisions: List<RestrictionDecision> = emptyList(),
    val statusMessage: String = "Grant Usage Access and Accessibility permissions, then refresh."
)

class MainViewModel(
    private val usageStatsReader: UsageStatsReader,
    private val policyEngine: ScreenTimePolicyEngine = ScreenTimePolicyEngine()
) : ViewModel() {

    private val configuredLimits = listOf(
        AppLimitConfig(packageName = "com.instagram.android", dailyLimitMinutes = 30),
        AppLimitConfig(packageName = "com.zhiliaoapp.musically", dailyLimitMinutes = 45),
        AppLimitConfig(packageName = "com.google.android.youtube", dailyLimitMinutes = 60)
    )

    fun refresh(): MainUiState {
        val usage = usageStatsReader.readTodayUsage()
        val decisions = policyEngine.evaluate(configuredLimits, usage)
        val blocked = decisions.filter { it.shouldLock }.map { it.packageName }.toSet()
        LockStateStore.updateBlockedPackages(blocked)
        return MainUiState(
            decisions = decisions,
            statusMessage = if (blocked.isEmpty()) "All monitored apps are within limits." else "Some apps exceeded limits and will be locked."
        )
    }
}
