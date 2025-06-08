package io.github.toyota32k.logger

import android.util.Log

/**
 * ロガーの設定
 */
object UtLogConfig {
    /**
     * ログ出力先の追加・登録
     * IUtLoggerの実装クラスを登録することができます。
     * - OnMemoryLogger: Listとしてログを格納（ビューに表示する場合に使用）
     * - FileLogger: ファイルにログを出力
     */
    val logChain = UtLoggerChain()

    /**
     * ログレベルを設定します。
     * デフォルトはINFO ... info/warn/errorを出力。debag/varbose は出力しない。
     * ログ出力モードなどのために、動的にログレベルを変えたいときは、
     * UtLogConfig.logLevel より、UtLog.logLevelProvider が優先される。
     * UtLog.logLevelProvider に null をセットすると、UtLogConfig.logLevel が参照される。
     * その場合は、
     */
    @JvmStatic
    var logLevel:Int = Log.INFO

    /**
     * デバッグモードかどうか
     * trueにすると、assertStrongly()で例外をスローします。
     * 現時点では、他の用途には使われません。
     */
    @JvmStatic
    var debug:Boolean = false
}