package io.github.toyota32k.logger

import android.util.Log

interface IUtVaLogger {
    fun debug(s: String, vararg args: Any?)
    fun warn(s: String, vararg args: Any?)
    fun error(s: String, vararg args: Any?)
    fun info(s: String, vararg args: Any?)
    fun verbose(s: String, vararg args: Any?)
    fun assert(chk:Boolean, msg:String?)
    fun stackTrace(e:Throwable, message:String?=null)
    fun print(level: Int, s: String, vararg args: Any?)
}

class UtLoggerInstance(private val tag:String) : IUtVaLogger {
    override fun debug(s: String, vararg args: Any?) {
        print(Log.DEBUG, s, *args)
    }

    override fun warn(s: String, vararg args: Any?) {
        print(Log.WARN, s, *args)
    }

    override fun error(s: String, vararg args: Any?) {
        print(Log.ERROR, s, *args)
    }

    override fun info(s: String, vararg args: Any?) {
        print(Log.INFO, s, *args)
    }

    override fun verbose(s: String, vararg args: Any?) {
        print(Log.VERBOSE, s, *args)
    }

    override fun assert(chk:Boolean, msg:String?) {
        if(!chk) {
            stackTrace(Exception("assertion failed."), msg)
        }
    }

    override fun stackTrace(e:Throwable, message:String?) {
        if(message!=null) {
            error(message)
        }
        e.message?.also { msg->
            error(msg)
        }
        error(e.stackTraceToString())
    }

    private fun printToSystemOut(tag: String, s: String): Int {
        println("$tag: $s")
//        Log.DEBUG
        return 0
    }

    private val isAndroid: Boolean by lazy {
        val runtime = System.getProperty("java.runtime.name")
        0 <= (runtime?.indexOf("Android") ?: -1)
    }

    private fun target(level: Int): (String, String) -> Int {
        if (!isAndroid) {
            return this::printToSystemOut
        }

        return when (level) {
            Log.DEBUG -> Log::d
            Log.ERROR -> Log::e
            Log.INFO -> Log::i
            Log.WARN -> Log::w
            else -> Log::v
        }
    }

    private fun logExternal(level:Int, s:String):Boolean {
        return UtLogConfig.externalLogger?.run {
            val ss = "$tag: $s"
            when(level) {
                Log.DEBUG->debug(ss)
                Log.ERROR->error(ss)
                Log.INFO->info(ss)
                Log.WARN->warn(ss)
                else -> verbose(ss)
            }
            true
        } ?: false
    }

    @Suppress("MemberVisibilityCanBePrivate")
    override fun print(level: Int, s: String, vararg args: Any?) {
        var ss = s
        if (args.isNotEmpty()) {
            ss = String.format(s, *args)
        }
        if(!logExternal(level, ss)) {
            target(level)(tag, ss)
        }
    }
}

object UtLogger : IUtVaLogger by UtLoggerInstance("Default")
