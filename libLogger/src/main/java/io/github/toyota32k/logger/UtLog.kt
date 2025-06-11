package io.github.toyota32k.logger

import android.util.Log
import io.github.toyota32k.logger.UtLogConfig.logLevelProvider
import java.io.Closeable

open class UtLog @JvmOverloads constructor(
    val tag:String,
    val parent:UtLog?=null,
    val omissionNamespace:String?=parent?.omissionNamespace,
    protected val outputClassName:Boolean=true,
    protected val outputMethodName:Boolean=true) {
    constructor(tag:String, parent:UtLog?, omissionNamespaceClass:Class<*>, outputClassName:Boolean=true, outputMethodName:Boolean=true):this(tag, parent, namespaceOfClass(omissionNamespaceClass), outputClassName, outputMethodName)
    companion object {
        fun hierarchicTag(tag:String, parent:UtLog?):String {
            return if(parent!=null) {
                "${hierarchicTag(parent.tag, parent.parent)}.${tag}"
            } else {
                tag
            }
        }
        fun namespaceOfClass(clazz:Class<*>):String {
            return clazz.name.substringBeforeLast(".", "").run {
                if(isEmpty()) {
                    clazz.name
                } else {
                    "$this."
                }
            }
        }
    }

    open val logLevel: Int get() = logLevelProvider?.invoke() ?: UtLogConfig.logLevel
    val logger: IUtLogger = UtLogConfig.logChain
    var loggerClassName = UtLog::class.java.name
    var chronosClassName = Chronos::class.java.name

    private fun stripNamespace(classname:String):String {
        if(!omissionNamespace.isNullOrBlank() && classname.startsWith(omissionNamespace)) {
            return classname.substring(omissionNamespace.length)
        } else {
            return classname
        }
    }

    private fun getCallerStack():StackTraceElement {
        val stack = Throwable().stackTrace  // Thread.currentThread().stackTrace  Throwable().stackTraceの方が速いらしい。
        var n = 0
        while(n<stack.size-1 && !stack[n].className.startsWith(loggerClassName)) { n++ }
        while(n<stack.size-1 && (stack[n].className.startsWith(loggerClassName)||stack[n].className.startsWith(chronosClassName))) { n++ }
        return stack[n]
    }

    fun compose(message:String?):String {
        return if(outputClassName||outputMethodName) {
            val e = getCallerStack()
            if(!outputClassName) {
                if(message!=null) "${e.methodName}: $message" else e.methodName
            } else if(!outputMethodName) {
                if(message!=null) "${stripNamespace(e.className)}: ${message}" else stripNamespace(e.className)
            } else {
                if(message!=null) "${stripNamespace(e.className)}.${e.methodName}: ${message}" else "${stripNamespace(e.className)}.${e.methodName}"
            }
        } else {
            message ?: ""
        }
    }

    @JvmOverloads
    fun debug(msg: String?=null) {
        if(logLevel<=Log.DEBUG) {
            logger.writeLog(Log.DEBUG, tag, compose(msg))
        }
    }
    fun debug(fn:()->String?) {
        if(logLevel<=Log.DEBUG) {
            logger.writeLog(Log.DEBUG, tag, compose(fn()?:return))
        }
    }
    fun debug(flag:Boolean, fn:()->String) {
        if(flag && logLevel<=Log.DEBUG) {
            logger.writeLog(Log.DEBUG, tag, compose(fn()))
        }
    }

    @JvmOverloads
    fun warn(msg: String?=null) {
        if(logLevel<=Log.WARN) {
            logger.writeLog(Log.WARN, tag, compose(msg))
        }
    }

    @JvmOverloads
    fun error(msg: String?=null) {
        logger.writeLog(Log.ERROR, tag, compose(msg))
    }

    @JvmOverloads
    fun error(e:Throwable, msg:String?=null) {
        error(msg)
        e.message?.also { msg->
            error(msg)
        }
        error(e.stackTraceToString())
    }

    @JvmOverloads
    fun info(msg: String?=null) {
        if(logLevel<=Log.INFO) {
            logger.writeLog(Log.INFO, tag, compose(msg))
        }
    }

    @JvmOverloads
    fun verbose(msg: String?=null) {
        if(logLevel<=Log.VERBOSE) {
            logger.writeLog(Log.VERBOSE, tag, compose(msg))
        }
    }
    fun verbose(fn: () -> String?) {
        if(logLevel<=Log.VERBOSE) {
            logger.writeLog(Log.VERBOSE, tag, compose(fn()?:return))
        }
    }
    fun verbose(flag:Boolean, fn:()->String) {
        if(flag && logLevel<=Log.VERBOSE) {
            logger.writeLog(Log.VERBOSE, tag, compose(fn()))
        }
    }

    @JvmOverloads
    fun stackTrace(e:Throwable, msg:String?=null) {
        if(msg!=null) {
            error(msg)
        }
        e.message?.also { msg->
            error(msg)
        }
        error(e.stackTraceToString())
    }
    @JvmOverloads
    fun stackTrace(level:Int, e:Throwable, msg:String?=null) {
        if (logLevel <= level) {
            if (msg != null) {
                print(level, msg)
            }
            e.message?.also { msg ->
                print(level, msg)
            }
            print(level, e.stackTraceToString())
        }
    }

    @JvmOverloads
    fun print(level:Int, msg:String?=null) {
        if(logLevel <= level) {
            logger.writeLog(level, tag, compose(msg))
        }
    }

    @JvmOverloads
    fun assert(chk:Boolean, msg:String?=null) {
        if(!chk) {
            stackTrace(Exception("assertion failed."), msg)
        }
    }

    @JvmOverloads
    fun assertStrongly(chk:Boolean, msg:String?=null) {
        if(!chk) {
            stackTrace(Exception("assertion failed."), msg)
            if (UtLogConfig.debug) {
                // デバッグ版なら例外を投げる
                throw AssertionError(compose(msg))
            }
        }
    }

    @JvmOverloads
    fun scopeWatch(msg:String?=null, level:Int=Log.DEBUG) : Closeable {
        val composed = compose(msg)
        logger.writeLog(level, tag, "$composed - enter")
        return ScopeWatcher { logger.writeLog(level, tag, "$composed - exit") }
    }

    private class ScopeWatcher(val leaving:()->Unit) : Closeable {
        override fun close() {
            leaving()
        }
    }

    inline fun <T> scopeCheck(msg:String?=null, level: Int=Log.DEBUG, fn:()->T):T {
        return try {
            print(level, "$msg - enter")
            fn()
        } finally {
            print(level, "$msg - exit")
        }
    }

    @JvmOverloads
    inline fun <T> chronos(tag:String="TIME", msg:String="", level: Int=Log.DEBUG,  fn:()->T):T {
        return if (level >= logLevel) {
            Chronos(this, tag = tag, logLevel = level).measure(msg) {
                fn()
            }
        } else fn()
    }

}