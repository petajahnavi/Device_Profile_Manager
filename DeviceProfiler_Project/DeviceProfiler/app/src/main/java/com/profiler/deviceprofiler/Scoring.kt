package com.profiler.deviceprofiler

data class ScoreResult(
    val cpuScore: Int,
    val memoryScore: Int,
    val batteryScore: Int,
    val gpuScore: Int,        // ✅ NEW
    val finalScore: Int,
    val grade: String
)

class Scoring {

    fun calculate(
        benchmarkTimeMs: Long,
        cpuCores: Int,
        totalRamMB: Long,
        batteryLevel: Int,
        gpuVersion: String     // ✅ NEW INPUT
    ): ScoreResult {

        val cpuScore     = calcCpuScore(benchmarkTimeMs, cpuCores)
        val memoryScore  = calcMemoryScore(totalRamMB)
        val batteryScore = calcBatteryScore(batteryLevel)
        val gpuScore     = calcGpuScore(gpuVersion)   // ✅ NEW

        // 🔥 UPDATED FINAL SCORE (GPU included)
        val finalScore = (
                cpuScore * 0.40 +
                        memoryScore * 0.30 +
                        batteryScore * 0.10 +
                        gpuScore * 0.20
                ).toInt().coerceIn(0, 100)

        val grade = when {
            finalScore >= 80 -> "A"
            finalScore >= 65 -> "B"
            finalScore >= 50 -> "C"
            else             -> "D"
        }

        return ScoreResult(cpuScore, memoryScore, batteryScore, gpuScore, finalScore, grade)
    }

    // ---------------- CPU ----------------
    private fun calcCpuScore(benchmarkTimeMs: Long, cpuCores: Int): Int {
        val timeScore = when {
            benchmarkTimeMs <= 100  -> 80
            benchmarkTimeMs >= 2000 -> 0
            else -> ((2000 - benchmarkTimeMs) * 80 / (2000 - 100)).toInt()
        }
        val coreBonus = ((cpuCores - 1) * 5).coerceIn(0, 20)
        return (timeScore + coreBonus).coerceIn(0, 100)
    }

    // ---------------- MEMORY ----------------
    private fun calcMemoryScore(totalRamMB: Long): Int {
        return when {
            totalRamMB <= 512  -> 10
            totalRamMB <= 2048 -> lerp(10, 50, totalRamMB - 512,  2048 - 512)
            totalRamMB <= 4096 -> lerp(50, 80, totalRamMB - 2048, 4096 - 2048)
            totalRamMB <= 8192 -> lerp(80, 100, totalRamMB - 4096, 8192 - 4096)
            else               -> 100
        }.coerceIn(0, 100)
    }

    // ---------------- BATTERY ----------------
    private fun calcBatteryScore(batteryLevel: Int): Int {
        return when {
            batteryLevel <= 0 -> 0
            batteryLevel < 20 -> (batteryLevel * 2)
            else              -> batteryLevel
        }.coerceIn(0, 100)
    }

    // ---------------- GPU (NEW) ----------------
    private fun calcGpuScore(gpuVersion: String): Int {
        return when {
            gpuVersion.startsWith("3") -> 90
            gpuVersion.startsWith("2") -> 70
            else -> 50
        }
    }

    private fun lerp(from: Int, to: Int, value: Long, range: Long): Int {
        if (range <= 0) return from
        return (from + (to - from) * value.toDouble() / range.toDouble()).toInt()
    }

    // ---------------- FORMAT ----------------
    fun format(result: ScoreResult): String {
        return buildString {
            appendLine("=== SCORES ===")
            appendLine("CPU Score     : ${result.cpuScore} / 100")
            appendLine("Memory Score  : ${result.memoryScore} / 100")
            appendLine("Battery Score : ${result.batteryScore} / 100")
            appendLine("GPU Score     : ${result.gpuScore} / 100")   // ✅ NEW
            appendLine("─────────────────────────")
            appendLine("FINAL SCORE   : ${result.finalScore} / 100  [Grade: ${result.grade}]")
        }
    }
}