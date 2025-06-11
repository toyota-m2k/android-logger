package io.github.toyota32k.logger

import android.util.Log

/**
 * 時間計測用ログ出力クラス
 */
open class Chronos @JvmOverloads constructor(callerLogger:UtLog, tag:String="TIME", val logLevel:Int= Log.DEBUG) {
    var logger = UtLog(tag, callerLogger, callerLogger.omissionNamespace)
    var prev: Long = 0
    var start: Long = 0

    init {
        reset()
    }

    fun reset() {
        prev = System.currentTimeMillis()
        start = prev
    }

    private val lapTime: Long
        get() {
            val c = System.currentTimeMillis()
            val d = c - prev
            prev = c
            return d
        }
    private val totalTime: Long get() = System.currentTimeMillis() - start

    @JvmOverloads
    fun total(msg: String = "") {
        logger.print(logLevel, "total = ${formatMS(totalTime)} $msg")
    }

    fun resetLap() {
        prev = System.currentTimeMillis()
    }

    fun formatMS(t: Long): String {
        return "${t / 1000f} sec"
    }

    open fun formatLap(msg:String):String {
        return "lap = ${formatMS(lapTime)} $msg"
    }

    open fun formatEnter(msg:String):String {
        return "enter $msg"
    }
    open fun formatExit(msg:String, begin:Long, end:Long):String {
        return "exit ${formatMS(end - begin)} $msg"
    }

    @JvmOverloads
    fun lap(msg: String = "") {
        logger.print(logLevel, formatLap(msg))
    }

    @JvmOverloads
    inline fun <T> measure(msg: String = "", fn: () -> T): T {
        logger.print(logLevel, formatEnter(msg))
        val begin = System.currentTimeMillis()
        return try {
            fn()
        } finally {
            logger.print(logLevel, formatExit(msg, begin, System.currentTimeMillis()))
        }
    }
}