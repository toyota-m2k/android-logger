package io.github.toyota32k.logger.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.toyota32k.logger.UtLog
import io.github.toyota32k.logger.UtLogConfig

class MainActivity : AppCompatActivity() {
    val logger = UtLog("Sample", null, this::class.java)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        UtLogConfig.logLevel = Log.DEBUG
        logger.verbose("Verbose(1) ... will be ignored")
        logger.debug("Debug(1)")
        logger.info("Info(1)")
        logger.warn("Warn(1)")
        logger.error("Error(1)")
        UtLogConfig.logLevel = Log.INFO
        logger.verbose("Verbose(2) ... will be ignored")
        logger.debug("Debug(2) ... will be ignored")
        logger.info("Info(2)")
        logger.warn("Warn(2)")
        logger.error("Error(2)")
    }
}