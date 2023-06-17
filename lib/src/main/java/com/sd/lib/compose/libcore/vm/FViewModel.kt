package com.sd.lib.compose.libcore.vm

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.lib.compose.libcore.utils.libCheckMainThread
import com.sd.lib.coroutine.FMutator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class FViewModel<I> : ViewModel() {
    @Volatile
    var isDestroyed = false
        private set(value) {
            require(value) { "Require true value." }
            field = value
        }

    private val _extHolder: MutableMap<Class<out FViewModelExt>, FViewModelExt> = hashMapOf()

    private val _isRefreshingFlow = MutableStateFlow(false)

    /** 是否正在刷新中 */
    val isRefreshingFlow: StateFlow<Boolean> = _isRefreshingFlow.asStateFlow()

    val vmMutator = FMutator()

    /**
     * 外部触发意图
     */
    fun dispatch(intent: I) {
        viewModelScope.launch {
            handleIntent(intent)
        }
    }

    /**
     * 刷新数据
     * @param notifyRefreshing 是否通知刷新状态[isRefreshingFlow]
     * @param delayTime 延迟多少毫秒后执行
     */
    fun refreshData(
        notifyRefreshing: Boolean = true,
        delayTime: Long = 0,
    ) {
        viewModelScope.launch {
            try {
                vmMutator.mutate {
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
     * 获取扩展对象，此方法必须在主线程调用
     */
    @MainThread
    fun <T : FViewModelExt> getExt(clazz: Class<T>): T {
        libCheckMainThread()
        val cache = _extHolder[clazz]
        if (cache != null) return cache as T
        return createExt(clazz).also { ext ->
            if (!isDestroyed) {
                _extHolder[clazz] = ext
                ext.init(this@FViewModel)
            }
        }
    }

    /**
     * 创建扩展对象
     */
    protected open fun <T : FViewModelExt> createExt(clazz: Class<T>): T {
        return clazz.newInstance()
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
     * 销毁回调，[onCleared]触发
     */
    protected open fun onDestroy() {}

    final override fun onCleared() {
        super.onCleared()
        destroyExt()
        isDestroyed = true
        onDestroy()
    }

    /**
     * 销毁并清空扩展对象
     */
    @MainThread
    private fun destroyExt() {
        libCheckMainThread()
        while (_extHolder.isNotEmpty()) {
            _extHolder.keys.toList().forEach { key ->
                _extHolder.remove(key)?.destroy()
            }
        }
    }
}

interface FViewModelExt {
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

abstract class BaseViewModelExt : FViewModelExt {
    private lateinit var _vm: FViewModel<*>

    protected val vm: FViewModel<*> get() = _vm

    final override fun init(viewModel: FViewModel<*>) {
        libCheckMainThread()
        if (this::_vm.isInitialized) error("$this has been initialized.")
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