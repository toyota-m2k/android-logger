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
     * デフォルトはINFO ... info/warn/errorを出力。debag/varbose は出力されない。
     */
    @JvmStatic
    var logLevel:Int = Log.INFO

    /**
     * 動的にログレベルを変更するためのログレベル取得関数を設定します。
     * この関数は logLevel の指定より優先されます。nullを設定すると、logLevel を参照します。
     */
    @JvmStatic
    var logLevelProvider:(()->Int)? = null

    /**
     * デバッグモードかどうか
     * trueにすると、assertStrongly()で例外をスローします。
     * 現時点では、他の用途には使われません。
     */
    @JvmStatic
    var debug:Boolean = false
}