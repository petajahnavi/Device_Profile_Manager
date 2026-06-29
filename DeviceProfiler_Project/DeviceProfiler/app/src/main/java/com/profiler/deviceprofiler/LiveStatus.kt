package com.profiler.deviceprofiler

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class LiveStatusData(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val chargingSource: String,
    val availableRamMB: Long,
    val usedRamMB: Long,
    val totalRamMB: Long,
    val ramUsagePercent: Int,
    val isLowMemory: Boolean
)

class LiveStatus(private val context: Context) {

    fun collect(): LiveStatusData {
        val battery = getBatteryInfo()
        val ram     = getRamInfo()

        return LiveStatusData(
            batteryPercent   = battery.first,
            isCharging       = battery.second,
            chargingSource   = battery.third,
            availableRamMB   = ram.available,
            usedRamMB        = ram.used,
            totalRamMB       = ram.total,
            ramUsagePercent  = ram.usagePercent,
            isLowMemory      = ram.isLow
        )
    }

    private fun getBatteryInfo(): Triple<Int, Boolean, String> {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, intentFilter)

            val level   = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale   = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status  = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1

            val percent  = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL

            val source = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC       -> "AC Charger"
                BatteryManager.BATTERY_PLUGGED_USB      -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Not charging"
            }
            Triple(percent, charging, source)
        } catch (e: Exception) {
            Triple(-1, false, "Unknown")
        }
    }

    private data class RamInfo(
        val available: Long,
        val used: Long,
        val total: Long,
        val usagePercent: Int,
        val isLow: Boolean
    )

    private fun getRamInfo(): RamInfo {
        return try {
            val am      = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)

            val totalMB    = memInfo.totalMem / (1024 * 1024)
            val availMB    = memInfo.availMem / (1024 * 1024)
            val usedMB     = totalMB - availMB
            val usagePct   = if (totalMB > 0) ((usedMB * 100) / totalMB).toInt() else 0

            RamInfo(availMB, usedMB, totalMB, usagePct, memInfo.lowMemory)
        } catch (e: Exception) {
            RamInfo(0L, 0L, 0L, 0, false)
        }
    }

    fun format(data: LiveStatusData): String {
        return buildString {
            appendLine("=== LIVE STATUS ===")
            appendLine("Battery   : ${data.batteryPercent}%  " +
                (if (data.isCharging) "[CHARGING - ${data.chargingSource}]" else "[NOT CHARGING]"))
            appendLine("Free RAM  : ${data.availableRamMB} MB / ${data.totalRamMB} MB")
            appendLine("RAM Usage : ${data.ramUsagePercent}%  " +
                (if (data.isLowMemory) "[LOW MEMORY WARNING]" else "[OK]"))
        }
    }
}