package com.example.screentime.android.data

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.screentime.shared.model.AppUsageSnapshot
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class UsageStatsReader(private val context: Context) {
    fun readTodayUsage(): List<AppUsageSnapshot> {
        val now = System.currentTimeMillis()
        val startOfDay = now - 24L * 60L * 60L * 1000L
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            now
        )

        val capturedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        return usageStats.map {
            AppUsageSnapshot(
                packageName = it.packageName,
                usedMinutesToday = (it.totalTimeInForeground / 60000L).toInt(),
                capturedAt = capturedAt
            )
        }
    }
}
