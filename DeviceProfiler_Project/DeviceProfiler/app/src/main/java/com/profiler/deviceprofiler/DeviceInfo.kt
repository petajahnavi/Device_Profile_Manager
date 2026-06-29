package com.profiler.deviceprofiler

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.opengl.GLES20

data class DeviceInfoData(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val cpuCores: Int,
    val cpuArchitecture: String,
    val totalRamMB: Long,
    val cpuFrequencyMHz: Long,
    val gpuName: String,        // ✅ NEW
    val gpuVersion: String      // ✅ NEW
)

class DeviceInfo(private val context: Context) {

    fun collect(): DeviceInfoData {
        return DeviceInfoData(
            deviceModel       = Build.MODEL,
            manufacturer      = Build.MANUFACTURER,
            androidVersion    = Build.VERSION.RELEASE,
            apiLevel          = Build.VERSION.SDK_INT,
            cpuCores          = getCpuCores(),
            cpuArchitecture   = getCpuArchitecture(),
            totalRamMB        = getTotalRamMB(),
            cpuFrequencyMHz   = getCpuFrequency(),
            gpuName           = getGpuName(),        // ✅ ADD
            gpuVersion        = getGpuVersion()      // ✅ ADD
        )
    }

    private fun getCpuCores(): Int {
        return try {
            Runtime.getRuntime().availableProcessors()
        } catch (e: Exception) { 1 }
    }

    private fun getCpuArchitecture(): String {
        return try {
            Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    private fun getTotalRamMB(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem / (1024 * 1024)
        } catch (e: Exception) { 0L }
    }

    private fun getCpuFrequency(): Long {
        return try {
            val file = java.io.File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            if (file.exists()) file.readText().trim().toLong() / 1000
            else 0L
        } catch (e: Exception) { 0L }
    }

    // ---------------- GPU FUNCTIONS ----------------

    private fun getGpuName(): String {
        return try {
            GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getGpuVersion(): String {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.deviceConfigurationInfo.glEsVersion
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // ---------------- FORMAT OUTPUT ----------------

    fun format(data: DeviceInfoData): String {
        return buildString {
            appendLine("=== DEVICE INFO ===")
            appendLine("Model       : ${data.manufacturer} ${data.deviceModel}")
            appendLine("Android     : ${data.androidVersion} (API ${data.apiLevel})")
            appendLine("CPU Cores   : ${data.cpuCores}")
            appendLine("CPU Arch    : ${data.cpuArchitecture}")
            appendLine("Total RAM   : ${data.totalRamMB} MB")

            if (data.cpuFrequencyMHz > 0)
                appendLine("CPU MaxFreq : ${data.cpuFrequencyMHz} MHz")

            appendLine("GPU Name    : ${data.gpuName}")        // ✅ NEW
            appendLine("GPU Version : ${data.gpuVersion}")     // ✅ NEW
        }
    }
}