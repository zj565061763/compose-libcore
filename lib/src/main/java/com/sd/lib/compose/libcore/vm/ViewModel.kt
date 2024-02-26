package com.sd.lib.compose.libcore.vm

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.lib.compose.libcore.libCheckMainThread
import com.sd.lib.coroutine.FMutator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface IgnoreActiveIntent

abstract class FViewModel<I>(
    /** 初始激活状态 */
    initialActive: Boolean = true,
) : ViewModel() {

    @Volatile
    var isDestroyed = false
        private set(value) {
            require(value) { "Require true value." }
            field = value
        }

    private var _isActiveFlow: MutableStateFlow<Boolean> = MutableStateFlow(initialActive)

    /** 是否处于激活状态，默认true */
    val isActiveFlow: StateFlow<Boolean> = _isActiveFlow.asStateFlow()

    private val _isRefreshingFlow = MutableStateFlow(false)

    /** 是否正在刷新中 */
    val isRefreshingFlow: StateFlow<Boolean> = _isRefreshingFlow.asStateFlow()

    /** 数据互斥修改器，[refreshData]中使用了这个对象 */
    protected val dataMutator = FMutator()

    /** 基于[Dispatchers.Default]并发为1的调度器 */
    @OptIn(ExperimentalCoroutinesApi::class)
    protected val singleDispatcher by lazy {
        Dispatchers.Default.limitedParallelism(1)
    }

    /**
     * 设置激活状态
     */
    fun setActive(active: Boolean) {
        viewModelScope.launch {
            if (_isActiveFlow.value != active) {
                _isActiveFlow.value = active
                if (active) onActive() else onInActive()
            }
        }
    }

    /**
     * 触发意图
     */
    fun dispatch(intent: I) {
        viewModelScope.launch {
            dispatchSuspend(intent)
        }
    }

    /**
     * 触发意图
     */
    suspend fun dispatchSuspend(intent: I) {
        if (isDestroyed) return
        if (isActiveFlow.value || intent is IgnoreActiveIntent) {
            handleIntent(intent)
        }
    }

    /**
     * [refreshDataSuspend]
     */
    fun refreshData(
        notifyRefreshing: Boolean = true,
        delayTime: Long = 0,
        ignoreActive: Boolean = false,
    ) {
        viewModelScope.launch {
            refreshDataSuspend(
                notifyRefreshing = notifyRefreshing,
                delayTime = delayTime,
                ignoreActive = ignoreActive,
            )
        }
    }

    /**
     * 刷新数据
     * @param notifyRefreshing 是否通知刷新状态[isRefreshingFlow]
     * @param delayTime 延迟多少毫秒后执行
     * @param ignoreActive 是否忽略激活状态[isActiveFlow]
     */
    suspend fun refreshDataSuspend(
        notifyRefreshing: Boolean = true,
        delayTime: Long = 0,
        ignoreActive: Boolean = false,
    ) {
        if (isDestroyed) return
        if (isActiveFlow.value || ignoreActive) {
            try {
                dataMutator.mutate {
                    if (notifyRefreshing) {
                        _isRefreshingFlow.value = true
                    }
                    delay(delayTime)
                    refreshDataImpl()
                }
            } finally {
                if (notifyRefreshing) {
                    _isRefreshingFlow.value = false
                }
            }
        }
    }

    /**
     * 处理意图，[viewModelScope]触发
     */
    protected abstract suspend fun handleIntent(intent: I)

    /**
     * 刷新数据，[viewModelScope]触发
     */
    protected abstract suspend fun refreshDataImpl()

    /**
     * 未激活 -> 激活，[viewModelScope]触发
     */
    protected open suspend fun onActive() {}

    /**
     * 激活 -> 未激活，[viewModelScope]触发
     */
    protected open suspend fun onInActive() {}

    /**
     * 销毁回调，[onCleared]触发
     */
    protected open fun onDestroy() {}

    final override fun onCleared() {
        super.onCleared()
        _isActiveFlow.value = false
        isDestroyed = true
        destroyPlugins()
        onDestroy()
    }

    //---------- plugin ----------

    @PublishedApi
    internal val plugins: MutableMap<Class<out Plugin>, Plugin> = hashMapOf()

    /**
     * 获取插件，如果插件不存在，则调用[factory]创建，此方法必须在主线程调用
     */
    @MainThread
    inline fun <reified T : Plugin> plugin(
        factory: () -> T = { T::class.java.getDeclaredConstructor().newInstance() },
    ): T {
        libCheckMainThread()
        val cache = plugins[T::class.java]
        return if (cache != null) {
            cache as T
        } else {
            factory().also {
                if (!isDestroyed) {
                    plugins[T::class.java] = it
                    it.init(this@FViewModel)
                }
            }
        }
    }

    /**
     * 销毁并清空插件
     */
    @MainThread
    private fun destroyPlugins() {
        libCheckMainThread()
        while (plugins.isNotEmpty()) {
            plugins.keys.toTypedArray().forEach { key ->
                plugins.remove(key)?.destroy()
            }
        }
    }

    interface Plugin {
        /**
         * 初始化（主线程）
         */
        @MainThread
        fun init(viewModel: FViewModel<*>)

        /**
         * 销毁（主线程）
         */
        @MainThread
        fun destroy()
    }
}

abstract class FViewModelPlugin : FViewModel.Plugin {
    @Volatile
    private var _vm: FViewModel<*>? = null
    protected val vm = checkNotNull(_vm)

    final override fun init(viewModel: FViewModel<*>) {
        libCheckMainThread()
        check(_vm == null) { "$this has been initialized." }
        _vm = viewModel
        onInit()
    }

    final override fun destroy() {
        libCheckMainThread()
        onDestroy()
    }

    /**
     * 初始化（主线程）
     */
    protected abstract fun onInit()

    /**
     * 销毁（主线程）
     */
    protected abstract fun onDestroy()
}