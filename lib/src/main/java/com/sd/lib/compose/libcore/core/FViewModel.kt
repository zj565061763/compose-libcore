package com.sd.lib.compose.libcore.core

import androidx.lifecycle.*
import com.sd.lib.coroutine.FMutator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

abstract class FViewModel<I> : ViewModel() {
    @Volatile
    private var _isDestroyed = false

    private var _lifecycle: WeakReference<Lifecycle>? = null
    private var _isPausedByLifecycle = false

    private val _isRefreshing = MutableStateFlow(false)

    /** 是否正在刷新中 */
    val isRefreshing = _isRefreshing.asStateFlow()

    val vmMutator = FMutator()

    /** 设置当前VM是否处于激活状态，只有激活状态才会处理事件 */
    @Volatile
    var isActiveState: Boolean = true
        set(value) {
            if (_isDestroyed) return
            if (field != value) {
                field = value
                _isPausedByLifecycle = false
                onActiveStateChanged()
            }
        }

    /**
     * 外部触发意图
     */
    fun dispatch(intent: I) {
        if (isActiveState || (intent is IgnoreActiveStateIntent)) {
            viewModelScope.launch {
                if (isActiveState || (intent is IgnoreActiveStateIntent)) {
                    handleIntent(intent)
                }
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refreshData(
        notifyRefreshing: Boolean = true,
        delayTime: Long = 0,
    ) {
        if (!isActiveState) return
        viewModelScope.launch {
            if (isActiveState) {
                try {
                    vmMutator.mutate {
                        if (notifyRefreshing) {
                            _isRefreshing.value = true
                        }
                        delay(delayTime)
                        refreshDataImpl()
                    }
                } finally {
                    if (notifyRefreshing) {
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }

    /**
     * 观察生命周期
     */
    fun setLifecycle(lifecycle: Lifecycle?) {
        val old = _lifecycle?.get()
        if (old === lifecycle) return

        old?.removeObserver(_lifecycleObserver)

        if (lifecycle == null) {
            _lifecycle = null
            if (_isPausedByLifecycle) {
                isActiveState = true
            }
        } else {
            if (!_isDestroyed) {
                _lifecycle = WeakReference(lifecycle)
                lifecycle.addObserver(_lifecycleObserver)
            }
        }
    }

    /**
     * 处理意图
     */
    protected abstract suspend fun handleIntent(intent: I)

    /**
     * 刷新数据
     */
    protected abstract suspend fun refreshDataImpl()

    /**
     * 激活状态变化
     */
    protected open fun onActiveStateChanged() {}

    /**
     * 生命周期变化回调
     */
    protected open fun onLifecycleStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {}

    /**
     * 销毁回调
     */
    protected open fun onDestroy() {}

    /**
     * 生命周期观察者
     */
    private val _lifecycleObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (_isDestroyed) {
                source.lifecycle.removeObserver(this)
                return
            }
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (isActiveState) {
                        isActiveState = false
                        _isPausedByLifecycle = true
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (_isPausedByLifecycle) {
                        isActiveState = true
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    source.lifecycle.removeObserver(this)
                }
                else -> {}
            }
            onLifecycleStateChanged(source, event)
        }
    }

    final override fun onCleared() {
        super.onCleared()
        _lifecycle = null
        isActiveState = false
        _isDestroyed = true
        onDestroy()
    }
}

interface IgnoreActiveStateIntent