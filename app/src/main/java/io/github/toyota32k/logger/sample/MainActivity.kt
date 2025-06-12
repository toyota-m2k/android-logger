package io.github.toyota32k.logger.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import io.github.toyota32k.logger.UtLog
import io.github.toyota32k.logger.UtLogConfig
import io.github.toyota32k.logger.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class MainActivity : AppCompatActivity() {
    companion object {
        val logger = UtLog("Sample", null, this::class.java)
    }
    class MainViewModel: ViewModel() {
        // Test-1
        // 親：lifecycleScope
        // 子：CoroutineScope(lifecycleScope.coroutineContext)
        // 親子が同じコンテキストを共有しているので、子をキャンセルすると親もキャンセルされる。当然、親をキャンセルすると子もキャンセルされる。
        fun normalJob(lifecycleOwner: LifecycleOwner) {
            val stopperScope = CoroutineScope(lifecycleOwner.lifecycleScope.coroutineContext+SupervisorJob())   // これを入れておかないと、lifecycleScope がキャンセルされて他のテストができなくなる。
            val parentScope = CoroutineScope(stopperScope.coroutineContext)
            val childScope = CoroutineScope(parentScope.coroutineContext)
            parentScope.launch {
                for(i in 1..10) {
                    delay(1.seconds)
                    logger.info("parent: $i")
                }
            }
            childScope.launch {
                for (i in 1..10) {
                    delay(0.5.seconds)
                    logger.info("child: $i")
                }
                childScope.cancel()
                // childをキャンセルすると、parentもキャンセルされる
                // この場合、lifecycleOwner.lifecycleScope.coroutineContext がキャンセルされてしまうので、１回しか実行できない
            }
        }

        // Test-2
        // 親：lifecycleScope
        // 子：lifecycleScope + SuperviserJob
        // 親と子が独立したコンテキスト(Job)を持つので、子をキャンセルしても親はキャンセルされないし、親をキャンセルしても子はキャンセルされない（Test-4)。
        fun supervisorJobOnChild(lifecycleOwner: LifecycleOwner) {
            val parentScope = CoroutineScope(lifecycleOwner.lifecycleScope.coroutineContext)
            val childScope = CoroutineScope(parentScope.coroutineContext+SupervisorJob())
            parentScope.launch {
                for(i in 1..10) {
                    delay(1.seconds)
                    logger.info("parent: $i")
                }
            }
            childScope.launch {
                for (i in 1..10) {
                    delay(0.5.seconds)
                    logger.info("child: $i")
                }
                childScope.cancel()
            }
        }

        // Test-3
        // 親：lifecycleScope + SuperviserJob
        // 子：parentScope.coroutineContext
        // 親と子が同じコンテキスト (Job) を持つので、子をキャンセルすると親もキャンセルされるし、親をキャンセルすると子もキャンセルされる(Test-5)。
        // Test-1 と違うのは、親（lifecycleScope） SuperviserJob を追加しているので、これをキャンセルしても、lifecycleScope はキャンセルされない点。
        // Test-1 は１回実行すると、アプリを再起動しないと、lifecycleScope が利用不能になってしまうが、Test-3/5 ではそのようなことは起きない。
        fun supervisorJobOnParent(lifecycleOwner: LifecycleOwner) {
            val parentScope = CoroutineScope(lifecycleOwner.lifecycleScope.coroutineContext+SupervisorJob())
            val childScope = CoroutineScope(parentScope.coroutineContext)
            parentScope.launch {
                for(i in 1..10) {
                    delay(1.seconds)
                    logger.info("parent: $i")
                }
            }
            childScope.launch {
                for (i in 1..10) {
                    delay(0.5.seconds)
                    logger.info("child: $i")
                }
                childScope.cancel()
            }
        }

        // Test-4
        fun supervisorJobOnChildCancelParent(lifecycleOwner: LifecycleOwner) {
            val stopperScope = CoroutineScope(lifecycleOwner.lifecycleScope.coroutineContext+SupervisorJob())   // これを入れておかないと、lifecycleScope がキャンセルされて他のテストができなくなる。
            val parentScope = CoroutineScope(stopperScope.coroutineContext)
            val childScope = CoroutineScope(parentScope.coroutineContext+SupervisorJob())
            parentScope.launch {
                for(i in 1..10) {
                    delay(0.5.seconds)
                    logger.info("parent: $i")
                }
                // child が SuperviserJob を持っているので、親をキャンセルしても子はキャンセルされない
                parentScope.cancel()
            }
            childScope.launch {
                for (i in 1..10) {
                    delay(1.seconds)
                    logger.info("child: $i")
                }
            }
        }
        // Test-5
        fun supervisorJobOnParentCancelParent(lifecycleOwner: LifecycleOwner) {
            val parentScope = CoroutineScope(lifecycleOwner.lifecycleScope.coroutineContext+SupervisorJob())
            val childScope = CoroutineScope(parentScope.coroutineContext)
            parentScope.launch {
                for(i in 1..10) {
                    delay(0.5.seconds)
                    logger.info("parent: $i")
                }
                // 子がSuperviserJobを持っていないので、親をキャンセルすると子もキャンセルされる。
                parentScope.cancel()
            }
            childScope.launch {
                for (i in 1..10) {
                    delay(1.seconds)
                    logger.info("child: $i")
                }
            }
        }

        // Test-6
        // 子スコープに、親スコープのJobを継承する子Job を持たせる。
        // 親Jobをキャンセルすると子Jobもキャンセルされるが、子Jobをキャンセルしても親Jobに影響しない。
        // この性質を利用するので、子スコープをキャンセルしても親スコープはキャンセルされない(Test-6) が、
        // 親スコープをキャンセルすると子スコープもキャンセルされる(Test-7)。
        fun jobOnChildCancelChild(lifecycleOwner: LifecycleOwner) {
            val stopperScope = CoroutineScope(lifecycleOwner.lifecycleScope.coroutineContext+SupervisorJob())   // これを入れておかないと、lifecycleScope がキャンセルされて他のテストができなくなる。
            val parentScope = CoroutineScope(stopperScope.coroutineContext)
            val childScope = CoroutineScope(parentScope.coroutineContext+Job(parentScope.coroutineContext[Job]))
            parentScope.launch {
                for(i in 1..10) {
                    delay(1.seconds)
                    logger.info("parent: $i")
                }
            }
            childScope.launch {
                for (i in 1..10) {
                    delay(0.5.seconds)
                    logger.info("child: $i")
                }
                // Jobのおかげで、子スコープのみキャンセルされる。
                childScope.cancel()
            }
        }
        // Test-7
        fun jobOnChildCancelParent(lifecycleOwner: LifecycleOwner) {
            val stopperScope = CoroutineScope(lifecycleOwner.lifecycleScope.coroutineContext+SupervisorJob())   // これを入れておかないと、lifecycleScope がキャンセルされて他のテストができなくなる。
            val parentScope = CoroutineScope(stopperScope.coroutineContext)
            val childScope = CoroutineScope(parentScope.coroutineContext+Job(parentScope.coroutineContext[Job]))
            parentScope.launch {
                for(i in 1..10) {
                    delay(0.5.seconds)
                    logger.info("parent: $i")
                }
                // 子スコープが親スコープのJobを継承しているので、子スコープもキャンセルされる。
                parentScope.cancel()
            }
            childScope.launch {
                for (i in 1..10) {
                    delay(1.seconds)
                    logger.info("child: $i")
                }
            }
        }

    }
    lateinit var controls: ActivityMainBinding
    val viewModel:MainViewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        logger.chronos(msg="initializing views", level=Log.INFO) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            controls = ActivityMainBinding.inflate(layoutInflater)
            setContentView(controls.root)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            controls.buttonTest1.setOnClickListener {
                viewModel.normalJob(this)
            }
            controls.buttonTest2.setOnClickListener {
                viewModel.supervisorJobOnChild(this)
            }
            controls.buttonTest3.setOnClickListener {
                viewModel.supervisorJobOnParent(this)
            }
            controls.buttonTest4.setOnClickListener {
                viewModel.supervisorJobOnChildCancelParent(this)
            }
            controls.buttonTest5.setOnClickListener {
                viewModel.supervisorJobOnParentCancelParent(this)
            }
            controls.buttonTest6.setOnClickListener {
                viewModel.jobOnChildCancelChild(this)
            }
            controls.buttonTest7.setOnClickListener {
                viewModel.jobOnChildCancelParent(this)
            }
        }
        logger.scopeCheck("test-scope", Log.INFO) {
            UtLogConfig.logLevel = Log.DEBUG
            logger.verbose { "Verbose(1) ... will be ignored" }
            logger.debug { "Debug(1)" }
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
}