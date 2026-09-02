package com.bloom.parental.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
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
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                ctx.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun getTodayMinutes(): Int {
        if (!hasPermission()) return 0
        return try {
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            val start = cal.timeInMillis
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
            val total = stats.sumOf { it.totalTimeInForeground }
            TimeUnit.MILLISECONDS.toMinutes(total).toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getAppsToday(): List<AppInfo> {
        if (!hasPermission()) return emptyList()
        return try {
            val pm = ctx.packageManager
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            val start = cal.timeInMillis
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, System.currentTimeMillis())

            val usageMap = mutableMapOf<String, Long>()
            for (s in stats) {
                usageMap[s.packageName] = (usageMap[s.packageName] ?: 0L) + s.totalTimeInForeground
            }

            val apps = mutableListOf<AppInfo>()
            for ((pkg, time) in usageMap) {
                if (time < 60000) continue
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    val name = info.loadLabel(pm).toString()
                    val used = TimeUnit.MILLISECONDS.toMinutes(time).toInt()
                    val limit = Prefs.getAppLimit(pkg, 0)
                    val blocked = Prefs.isAppBlocked(pkg)
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    apps.add(AppInfo(pkg, name, used, limit, blocked, isSystem))
                } catch (e: PackageManager.NameNotFoundException) {
                    continue
                }
            }
            apps.sortedByDescending { it.usedMinutes }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
