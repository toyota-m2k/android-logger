package io.github.toyota32k.logger

import android.util.Log
import io.github.toyota32k.logger.OnMemoryLogger.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.CoroutineContext

interface IUtLogger {
    fun writeLog(level:Int, tag:String, msg:String)
}

class UtLoggerChain : IUtLogger {
    private var loggers: MutableList<IUtLogger> = mutableListOf(DebugLogger)

    operator fun plus(logger: IUtLogger): UtLoggerChain
        = apply {
            synchronized(loggers) {
                loggers.add(logger)
            }
        }
    operator fun plusAssign(logger: IUtLogger) {
        synchronized(loggers) {
            loggers.add(logger)
        }
    }
    operator fun minus(logger: IUtLogger): UtLoggerChain
        = apply {
            synchronized(loggers) {
                loggers.remove(logger)
            }
        }
    operator fun minusAssign(logger: IUtLogger) {
        synchronized(loggers) {
            loggers.remove(logger)
        }
    }

    fun disableDefaultLogger():UtLoggerChain
        = apply {
            synchronized(loggers) {
                loggers.remove(DebugLogger)
            }
        }

    override fun writeLog(level: Int, tag: String, msg: String) {
        loggers.forEach {
            it.writeLog(level, tag, msg)
        }
    }
}

class OnMemoryLogger(val maxCount:Int = 1000) : IUtLogger {
    data class LogEntry(val level:Int, val tag:String, val msg:String) {
        companion object {
            fun levelToString(level:Int):String {
                return when(level) {
                    android.util.Log.DEBUG -> "[DEBUG]"
                    android.util.Log.ERROR -> "[ERROR]"
                    android.util.Log.INFO -> "[INFO]"
                    android.util.Log.VERBOSE -> "[VERBOSE]"
                    android.util.Log.WARN -> "[WARN]"
                    else -> "[INVALID]"
                }
            }
        }
        override fun toString(): String {
            return "${levelToString(level)} $tag: $msg"
        }
    }
    private val list = ArrayDeque<LogEntry>(maxCount)
    val logs:List<LogEntry> get() = list
    override fun toString(): String {
        return list.fold(StringBuffer()) { sb, e-> sb.append(e.toString()).append("\n") }.toString()
    }

    override fun writeLog(level:Int, tag:String, msg:String) {
        if(list.size>=maxCount) {
            list.removeFirst()
        }
        list.add(LogEntry(level, tag, msg))
    }
}

class FlowLogger(val flowCollector: FlowCollector<LogEntry> = MutableSharedFlow<LogEntry>(), val coroutineContext:CoroutineContext= Dispatchers.IO) : IUtLogger {
    val scope = CoroutineScope(coroutineContext)
    override fun writeLog(level:Int, tag:String, msg:String) {
        scope.launch {
            flowCollector.emit(LogEntry(level, tag, msg))
        }
    }
}


class FileLogger(val outputDirectory: String, val fileName: String, val maxFileSize: Long = 10 * 1024 * 1024, val maxFileCount: Int = 4) : IUtLogger {
    private val baseFile: File
    private var currentFile: File
    private var fileWriter: FileWriter? = null
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    init {
        // 出力ディレクトリの作成を確認
        val directory = File(outputDirectory)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        // 基本ファイル名と現在のファイルを設定
        baseFile = File(directory, fileName)
        currentFile = baseFile

        // ローテーションファイルの整理
        cleanupRotatedFiles()

        // ファイルライターの初期化
        try {
            fileWriter = FileWriter(currentFile, true)
        } catch (e: IOException) {
            android.util.Log.e("FileLogger", "Failed to initialize FileWriter: ${e.message}")
        }
    }

    private fun cleanupRotatedFiles() {
        val directory = File(outputDirectory)
        val baseNameWithoutExt = fileName.substringBeforeLast(".", "")
        val extension = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""

        // ローテーションファイルを取得して日付順に並べる
        val rotatedFiles = directory.listFiles { file ->
            file.name.startsWith(baseNameWithoutExt) &&
            file.name.endsWith(extension) &&
            file.name != fileName
        }?.sortedBy { it.lastModified() } ?: emptyList()

        // ファイル数が最大数を超えている場合、古いファイルを削除
        if (rotatedFiles.size >= maxFileCount - 1) {  // -1 は現在のファイル用
            val filesToDelete = rotatedFiles.size - (maxFileCount - 1)
            if (filesToDelete > 0) {
                rotatedFiles.take(filesToDelete).forEach { it.delete() }
            }
        }
    }

    private fun checkRotation() {
        if (currentFile.exists() && currentFile.length() >= maxFileSize) {
            rotateLogFile()
        }
    }

    private fun rotateLogFile() {
        closeWriter()

        // 現在のファイルをローテーション済みファイルとして名前変更
        val timestamp = dateFormat.format(Date())
        val baseNameWithoutExt = fileName.substringBeforeLast(".", "")
        val extension = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
        val rotatedFileName = "${baseNameWithoutExt}_${timestamp}${extension}"
        val rotatedFile = File(outputDirectory, rotatedFileName)

        if (currentFile.exists()) {
            currentFile.renameTo(rotatedFile)
        }

        // 新しいカレントファイルを作成
        currentFile = File(outputDirectory, fileName)

        // ファイルライターを再初期化
        try {
            fileWriter = FileWriter(currentFile, true)
        } catch (e: IOException) {
            android.util.Log.e("FileLogger", "Failed to initialize FileWriter after rotation: ${e.message}")
        }

        // ローテーションファイルの整理
        cleanupRotatedFiles()
    }

    override fun writeLog(level: Int, tag: String, msg: String) {
        checkRotation()

        try {
            val levelString = OnMemoryLogger.LogEntry.levelToString(level)
            val timestamp = dateFormat.format(Date())
            val logLine = "$timestamp $levelString $tag: $msg\n"

            fileWriter?.write(logLine)
            fileWriter?.flush()
        } catch (e: IOException) {
            android.util.Log.e("FileLogger", "Failed to write log: ${e.message}")
        }
    }

    private fun closeWriter() {
        try {
            fileWriter?.flush()
            fileWriter?.close()
            fileWriter = null
        } catch (e: IOException) {
            android.util.Log.e("FileLogger", "Failed to close FileWriter: ${e.message}")
        }
    }

    // リソース解放のためデストラクタを実装
    fun close() {
        closeWriter()
    }
}

object DebugLogger : IUtLogger {
    private val isAndroid: Boolean by lazy {
        val runtime = System.getProperty("java.runtime.name")
        0 <= (runtime?.indexOf("Android") ?: -1)
    }
    private fun printToSystemOut(tag: String, s: String): Int {
        println("$tag: $s")
        return 0
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

    override fun writeLog(level: Int, tag: String, msg: String) {
        target(level)(tag, msg)
    }
}
