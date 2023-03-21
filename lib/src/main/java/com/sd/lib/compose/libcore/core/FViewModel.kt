package com.sd.lib.compose.libcore.core

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.lib.coroutine.FMutator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

abstract class FViewModel<I> : ViewModel() {
    @Volatile
    private var _isDestroyed = false

    @Volatile
    private var _isActiveState = true
        set(value) {
            if (_isDestroyed) return
            synchronized(this@FViewModel) {
                if (field != value) {
                    field = value
                    _isPausedByLifecycle = false
                    notifyActivityStateChange()
                }
            }
        }

    private var _lifecycle: WeakReference<Lifecycle>? = null
    private var _isPausedByLifecycle = false
    private val _handler = Handler(Looper.getMainLooper())

    private val _isRefreshing = MutableStateFlow(false)

    /** 是否正在刷新中 */
    val isRefreshing = _isRefreshing.asStateFlow()

    val vmMutator = FMutator()

    /** 设置当前VM是否处于激活状态，只有激活状态才会处理事件 */
    val isActiveState: Boolean get() = _isActiveState

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
        synchronized(this@FViewModel) {
            val old = _lifecycle?.get()
            if (old === lifecycle) return

            old?.removeObserver(_lifecycleObserver)

            if (lifecycle == null) {
                _lifecycle = null
                if (_isPausedByLifecycle) {
                    _isActiveState = true
                }
            } else {
                if (!_isDestroyed) {
                    _lifecycle = WeakReference(lifecycle)
                    lifecycle.addObserver(_lifecycleObserver)
                }
            }
        }
    }

    /**
     * 设置是否处于激活状态
     */
    fun setActiveState(active: Boolean) {
        synchronized(this@FViewModel) {
            if (active) {
                if (!_isPausedByLifecycle) {
                    _isActiveState = true
                }
            } else {
                _isActiveState = false
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
     * 激活状态变化（UI线程）
     */
    protected open fun onActiveStateChanged() {}

    /**
     * 销毁回调，[onCleared]触发
     */
    protected open fun onDestroy() {}

    /**
     * 生命周期观察者
     */
    private val _lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                synchronized(this@FViewModel) {
                    if (_isActiveState) {
                        _isActiveState = false
                        _isPausedByLifecycle = true
                    }
                }
            }
            Lifecycle.Event.ON_START -> {
                synchronized(this@FViewModel) {
                    if (_isPausedByLifecycle) {
                        _isActiveState = true
                    }
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                setLifecycle(null)
            }
            else -> {}
        }
    }

    /**
     * 通知状态变化
     */
    private fun notifyActivityStateChange() {
        _handler.post {
            onActiveStateChanged()
        }
    }

    final override fun onCleared() {
        super.onCleared()
        setLifecycle(null)
        _isActiveState = false
        _isDestroyed = true
        onDestroy()
    }
}

interface IgnoreActiveStateIntent