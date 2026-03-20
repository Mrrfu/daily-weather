package com.weatherglass.core.common

import android.util.Log

/**
 * 简单的性能监控工具
 * 用于追踪关键操作的执行时间
 */
object PerformanceMonitor {
    private const val TAG = "PerformanceMonitor"
    private val traces = mutableMapOf<String, Long>()

    /**
     * 开始追踪
     */
    fun startTrace(name: String) {
        traces[name] = System.currentTimeMillis()
        Log.d(TAG, "Started trace: $name")
    }

    /**
     * 结束追踪并记录耗时
     */
    fun stopTrace(name: String): Long {
        val startTime = traces.remove(name)
        if (startTime == null) {
            Log.w(TAG, "Trace not found: $name")
            return -1
        }
        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Trace '$name' completed in ${duration}ms")
        return duration
    }

    /**
     * 测量代码块执行时间
     */
    inline fun <T> measure(name: String, block: () -> T): T {
        startTrace(name)
        try {
            return block()
        } finally {
            stopTrace(name)
        }
    }

    /**
     * 测量挂起函数执行时间
     */
    suspend inline fun <T> measureAsync(name: String, block: suspend () -> T): T {
        startTrace(name)
        try {
            return block()
        } finally {
            stopTrace(name)
        }
    }

    /**
     * 记录自定义指标
     */
    fun recordMetric(name: String, value: Long) {
        Log.d(TAG, "Metric '$name': $value")
    }

    /**
     * 记录内存使用情况
     */
    fun logMemoryUsage(context: String = "") {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        Log.d(TAG, "Memory usage $context: ${usedMemory}MB / ${maxMemory}MB")
    }
}
