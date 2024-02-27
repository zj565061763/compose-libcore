package com.sd.lib.compose.libcore.vm

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.lib.compose.libcore.libCheckMainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    /** 基于[Dispatchers.Default]并发为1的调度器 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val singleDispatcher by lazy { Dispatchers.Default.limitedParallelism(1) }

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
            dispatchAwait(intent)
        }
    }

    /**
     * 触发意图
     */
    suspend fun dispatchAwait(intent: I) {
        if (isDestroyed) return
        if (isActiveFlow.value || intent is IgnoreActiveIntent) {
            handleIntent(intent)
        }
    }

    /**
     * 处理意图，[viewModelScope]触发
     */
    protected abstract suspend fun handleIntent(intent: I)

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
        isDestroyed = true
        _isActiveFlow.value = false
        destroyPlugins()
        onDestroy()
    }

    //---------- plugins ----------

    /**
     * [DataPlugin]
     */
    fun dataPlugin(): DataPlugin = plugin(DataPlugin::class.java)

    //---------- plugin logic ----------

    private val _pluginHolder: MutableMap<Class<out Plugin>, Plugin> = hashMapOf()
    private val _pluginFactoryHolder: MutableMap<Class<out Plugin>, PluginFactory> = hashMapOf()

    /**
     * 获取插件，此方法必须在主线程调用
     */
    @MainThread
    protected fun <T : Plugin> plugin(clazz: Class<T>): T {
        libCheckMainThread()
        val cache = _pluginHolder[clazz]
        return if (cache != null) {
            @Suppress("UNCHECKED_CAST")
            cache as T
        } else {
            val factory = _pluginFactoryHolder[clazz] ?: DefaultPluginFactory
            factory.create(clazz).also {
                if (!isDestroyed) {
                    _pluginHolder[clazz] = it
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
        while (_pluginHolder.isNotEmpty()) {
            _pluginHolder.keys.toTypedArray().forEach { key ->
                _pluginHolder.remove(key)?.destroy()
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

    interface PluginFactory {
        fun <T : Plugin> create(clazz: Class<T>): T
    }
}

private object DefaultPluginFactory : FViewModel.PluginFactory {
    override fun <T : FViewModel.Plugin> create(clazz: Class<T>): T {
        return clazz.getDeclaredConstructor().newInstance()
    }
}

abstract class FViewModelPlugin : FViewModel.Plugin {
    @Volatile
    private var _vm: FViewModel<*>? = null
    private val vm = checkNotNull(_vm)

    protected val viewModelScope get() = vm.viewModelScope
    protected val isDestroyed get() = vm.isDestroyed
    protected val isActiveFlow get() = vm.isActiveFlow
    protected val singleDispatcher get() = vm.singleDispatcher

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