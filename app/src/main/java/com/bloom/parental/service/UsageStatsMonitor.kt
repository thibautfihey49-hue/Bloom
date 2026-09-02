package com.bloom.parental.service

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.bloom.parental.data.Prefs
import java.util.concurrent.TimeUnit

data class AppInfo(
    val packageName: String,
    val name: String,
    val usedMinutes: Int,
    val limitMinutes: Int,
    val isBlocked: Boolean,
    val isSystem: Boolean
)

class UsageStatsMonitor(private val ctx: Context) {
    private fun hasPermission(): Boolean {
        return try {
            val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }
    }

    fun getTodayMinutes(): Int {
        if (!hasPermission()) return 0
        return try {
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis())
            TimeUnit.MILLISECONDS.toMinutes(stats.sumOf { it.totalTimeInForeground }).toInt()
        } catch (e: Exception) { 0 }
    }

    fun getAppsToday(): List<AppInfo> {
        if (!hasPermission()) return emptyList()
        return try {
            val pm = ctx.packageManager
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis())
            val usageMap = stats.groupBy { it.packageName }.mapValues { it.value.sumOf { s -> s.totalTimeInForeground } }
            usageMap.filter { it.value >= 60000 }.mapNotNull { (pkg, time) ->
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    AppInfo(pkg, info.loadLabel(pm).toString(), TimeUnit.MILLISECONDS.toMinutes(time).toInt(),
                        Prefs.getAppLimit(pkg, 0), Prefs.isAppBlocked(pkg), (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                } catch (e: PackageManager.NameNotFoundException) { null }
            }.sortedByDescending { it.usedMinutes }
        } catch (e: Exception) { emptyList() }
    }
}
