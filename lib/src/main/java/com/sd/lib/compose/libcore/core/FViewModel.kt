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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

abstract class FViewModel<I> : ViewModel() {
    @Volatile
    private var _isDestroyed = false
        set(value) {
            require(value) { "Require true value." }
            field = value
        }

    @get:Synchronized
    private var _isVMActive = true
        set(value) {
            if (_isDestroyed) return
            synchronized(this@FViewModel) {
                if (field != value) {
                    field = value
                    _isPausedByLifecycle = false
                    Handler(Looper.getMainLooper()).post { onVMActiveChanged() }
                }
            }
            _isVMActiveFlow.value = value
        }

    private var _lifecycle: WeakReference<Lifecycle>? = null
    private var _isPausedByLifecycle = false

    private val _isRefreshingFlow = MutableStateFlow(false)
    private val _isVMActiveFlow = MutableStateFlow(_isVMActive)

    /** 是否正在刷新中 */
    val isRefreshingFlow: StateFlow<Boolean> = _isRefreshingFlow.asStateFlow()

    /** 当前VM是否处于激活状态，只有激活状态才会处理事件 */
    val isVMActiveFlow: StateFlow<Boolean> = _isVMActiveFlow.asStateFlow()

    /** 当前VM是否处于激活状态，只有激活状态才会处理事件 */
    val isVMActive: Boolean get() = _isVMActive

    val vmMutator = FMutator()

    /**
     * 外部触发意图
     */
    fun dispatch(intent: I) {
        if (_isVMActive || (intent is IgnoreVMActiveIntent)) {
            viewModelScope.launch {
                if (_isVMActive || (intent is IgnoreVMActiveIntent)) {
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
        if (!_isVMActive) return
        viewModelScope.launch {
            if (_isVMActive) {
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
                    _isVMActive = true
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
     * 设置当前VM是否处于激活状态
     */
    fun setVMActive(active: Boolean) {
        if (_isDestroyed) return
        synchronized(this@FViewModel) {
            if (active) {
                if (!_isPausedByLifecycle) {
                    _isVMActive = true
                }
            } else {
                _isVMActive = false
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
     * 当前VM激活状态变化（UI线程）
     */
    protected open fun onVMActiveChanged() {}

    /**
     * 销毁回调，[onCleared]触发
     */
    protected open fun onDestroy() {}

    /**
     * 生命周期观察者
     */
    private val _lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                synchronized(this@FViewModel) {
                    if (_isVMActive) {
                        _isVMActive = false
                        _isPausedByLifecycle = true
                    }
                }
            }

            Lifecycle.Event.ON_RESUME -> {
                synchronized(this@FViewModel) {
                    if (_isPausedByLifecycle) {
                        _isVMActive = true
                    }
                }
            }

            Lifecycle.Event.ON_DESTROY -> {
                setLifecycle(null)
            }

            else -> {}
        }
    }

    final override fun onCleared() {
        super.onCleared()
        setLifecycle(null)
        _isVMActive = false
        _isDestroyed = true
        onDestroy()
    }
}

interface IgnoreVMActiveIntent