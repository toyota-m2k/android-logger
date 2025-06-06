package io.github.toyota32k.logger

import android.util.Log

/**
 * 時間計測用ログ出力クラス
 */
class Chronos @JvmOverloads constructor(callerLogger:UtLog, tag:String="TIME", val logLevel:Int= Log.DEBUG) {
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

    @JvmOverloads
    fun lap(msg: String = "") {
        logger.print(logLevel,"lap = ${formatMS(lapTime)} $msg")
    }

    fun formatMS(t: Long): String {
        return "${t / 1000f} sec"
    }

    inline fun <T> measure(msg: String? = null, fn: () -> T): T {
        val begin = System.currentTimeMillis()
        logger.print(logLevel,"enter ${msg ?: ""}")
        return try {
            fn()
        } finally {
            logger.print(logLevel, "exit ${formatMS(System.currentTimeMillis() - begin)} ${msg ?: ""}")
        }
    }

//    suspend fun <T> measureAsync(msg: String? = null, fn: suspend () -> T): T {
//        val begin = System.currentTimeMillis()
//        logger.debug("enter ${msg ?: ""}")
//        return try {
//            fn()
//        } finally {
//            logger.debug("exit ${formatMS(System.currentTimeMillis() - begin)} ${msg ?: ""}")
//        }
//    }
}