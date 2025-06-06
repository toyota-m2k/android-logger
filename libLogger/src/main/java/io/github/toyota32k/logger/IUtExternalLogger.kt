package io.github.toyota32k.logger

interface IUtExternalLogger {
    fun debug(msg:String)
    fun warn(msg:String)
    fun error(msg:String)
    fun info(msg:String)
    fun verbose(msg:String)
}
