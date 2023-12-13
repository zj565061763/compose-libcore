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

    private var _isActiveFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)
    /** 是否处于激活状态，默认true */
    val isActiveFlow: StateFlow<Boolean> = _isActiveFlow.asStateFlow()

    private val _isRefreshingFlow = MutableStateFlow(false)
    /** 是否正在刷新中 */
    val isRefreshingFlow: StateFlow<Boolean> = _isRefreshingFlow.asStateFlow()

    private val _extHolder: MutableMap<Class<out FViewModelExt>, FViewModelExt> = hashMapOf()

    /** 互斥修改器 */
    protected val vmMutator = FMutator()

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
     * 外部触发意图
     */
    fun dispatch(intent: I) {
        viewModelScope.launch {
            handleIntent(intent)
        }
    }

    /**
     * 外部触发意图
     */
    suspend fun intent(intent: I) {
        if (isDestroyed) return
        if (isActiveFlow.value) {
            handleIntent(intent)
        }
    }

    /**
     * 刷新数据
     * @param notifyRefreshing 是否通知刷新状态[isRefreshingFlow]
     * @param delayTime 延迟多少毫秒后执行
     */
    @JvmOverloads
    fun refreshData(
        notifyRefreshing: Boolean = true,
        delayTime: Long = 0,
    ) {
        viewModelScope.launch {
            if (isActiveFlow.value) {
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
    }

    /**
     * 获取扩展对象，此方法必须在主线程调用
     */
    @Suppress("UNCHECKED_CAST")
    @MainThread
    fun <T : FViewModelExt> getExt(clazz: Class<T>): T {
        libCheckMainThread()
        val cache = _extHolder[clazz]
        return if (cache != null) {
            cache as T
        } else {
            clazz.getDeclaredConstructor().newInstance().also { ext ->
                if (!isDestroyed) {
                    _extHolder[clazz] = ext
                    ext.init(this@FViewModel)
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
    protected open fun onActive() {}

    /**
     * 激活 -> 未激活，[viewModelScope]触发
     */
    protected open fun onInActive() {}

    /**
     * 销毁回调，[onCleared]触发
     */
    protected open fun onDestroy() {}

    final override fun onCleared() {
        super.onCleared()
        _isActiveFlow.value = false
        isDestroyed = true
        destroyExt()
        onDestroy()
    }

    /**
     * 销毁并清空扩展对象
     */
    @MainThread
    private fun destroyExt() {
        libCheckMainThread()
        while (_extHolder.isNotEmpty()) {
            _extHolder.keys.toMutableList().forEach { key ->
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
    @Volatile
    private var _viewModel: FViewModel<*>? = null
    protected val viewModel by ::_viewModel

    final override fun init(viewModel: FViewModel<*>) {
        libCheckMainThread()
        check(_viewModel == null) { "$this has been initialized." }
        _viewModel = viewModel
        onInit()
    }

    final override fun destroy() {
        libCheckMainThread()
        /** 提前置为null，不允许[onDestroy]里面继续访问[viewModel] */
        _viewModel = null
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