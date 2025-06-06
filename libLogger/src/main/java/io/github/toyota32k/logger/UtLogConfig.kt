package io.github.toyota32k.logger

import android.util.Log

/**
 * ロガーの設定
 */
object UtLogConfig {
    /**
     * 外部ログ出力先を設定します。
     *
     * null（デフォルト）の場合は、LogCat に出力します。
     * エラーログをファイルに保存する、ログ画面に表示する、などの目的で利用できます。
     */
    @JvmStatic
    var externalLogger: IUtExternalLogger? = null

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