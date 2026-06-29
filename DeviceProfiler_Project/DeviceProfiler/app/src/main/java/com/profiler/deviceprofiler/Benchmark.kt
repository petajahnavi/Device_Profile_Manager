package com.profiler.deviceprofiler

import kotlin.math.sqrt

data class BenchmarkResult(
    val matrixTimeMs: Long,
    val floatOpsTimeMs: Long,
    val memoryTimeMs: Long,
    val totalTimeMs: Long,
    val passed: Boolean,
    val estimatedMFlops: Double
)

class Benchmark {

    companion object {
        private const val MATRIX_SIZE       = 50
        private const val FLOAT_OPS_COUNT   = 100_000
        private const val MEMORY_ARRAY_SIZE = 1_000_000
        private const val TIME_LIMIT_MS     = 2000L
    }

    fun run(): BenchmarkResult {
        val matrixMs = runMatrixMultiply()
        val floatMs  = runFloatOps()
        val memMs    = runMemoryAccess()
        val total    = matrixMs + floatMs + memMs

        val flops  = 2.0 * MATRIX_SIZE * MATRIX_SIZE * MATRIX_SIZE
        val mflops = if (matrixMs > 0) (flops / matrixMs.toDouble() / 1000.0) else 0.0

        return BenchmarkResult(
            matrixTimeMs    = matrixMs,
            floatOpsTimeMs  = floatMs,
            memoryTimeMs    = memMs,
            totalTimeMs     = total,
            passed          = total <= TIME_LIMIT_MS,
            estimatedMFlops = mflops
        )
    }

    private fun runMatrixMultiply(): Long {
        val n = MATRIX_SIZE
        val a = Array(n) { row -> FloatArray(n) { col -> (row + col + 1).toFloat() } }
        val b = Array(n) { row -> FloatArray(n) { col -> (row * col + 1).toFloat() } }
        val c = Array(n) { FloatArray(n) }

        val start = System.currentTimeMillis()
        for (i in 0 until n) {
            for (j in 0 until n) {
                var sum = 0f
                for (k in 0 until n) {
                    sum += a[i][k] * b[k][j]
                }
                c[i][j] = sum
            }
        }
        return System.currentTimeMillis() - start
    }

    private fun runFloatOps(): Long {
        var result = 1.0
        val start  = System.currentTimeMillis()
        for (i in 1..FLOAT_OPS_COUNT) {
            result += sqrt(i.toDouble()) * 0.001
        }
        if (result < 0) throw RuntimeException("Should not happen")
        return System.currentTimeMillis() - start
    }

    private fun runMemoryAccess(): Long {
        val arr   = IntArray(MEMORY_ARRAY_SIZE)
        val start = System.currentTimeMillis()
        for (i in arr.indices) arr[i] = i * 2
        var sum = 0L
        for (v in arr) sum += v
        if (sum < 0) throw RuntimeException("Should not happen")
        return System.currentTimeMillis() - start
    }

    fun format(result: BenchmarkResult): String {
        return buildString {
            appendLine("=== BENCHMARK ===")
            appendLine("Matrix (50x50) : ${result.matrixTimeMs} ms")
            appendLine("Float Ops      : ${result.floatOpsTimeMs} ms")
            appendLine("Memory Access  : ${result.memoryTimeMs} ms")
            appendLine("Total Time     : ${result.totalTimeMs} ms  " +
                (if (result.passed) "[PASS]" else "[SLOW - exceeded 2s]"))
            appendLine("Est. MFLOPs    : ${"%.2f".format(result.estimatedMFlops)}")
        }
    }
}