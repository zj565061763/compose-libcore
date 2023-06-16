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
    private var _isVMActive: Boolean? = null
        set(value) {
            if (_isDestroyed) return
            requireNotNull(value) { "Require not null value." }
            synchronized(this@FViewModel) {
                if (field != value) {
                    field = value
                    _isPausedByLifecycle = false
                    notifyVMActiveChanged()
                }
            }
            _isVMActiveFlow.value = value
        }

    private var _lifecycle: WeakReference<Lifecycle>? = null
    private var _isPausedByLifecycle = false

    @Volatile
    private var _refreshDataWhenActive = false

    private val _isRefreshingFlow = MutableStateFlow(false)
    private val _isVMActiveFlow = MutableStateFlow(isVMActive)

    /** 是否正在刷新中 */
    val isRefreshingFlow: StateFlow<Boolean> = _isRefreshingFlow.asStateFlow()

    /** 当前VM是否处于激活状态，只有激活状态才会处理事件 */
    val isVMActiveFlow: StateFlow<Boolean> = _isVMActiveFlow.asStateFlow()

    /** 当前VM是否处于激活状态，只有激活状态才会处理事件 */
    val isVMActive: Boolean get() = _isVMActive ?: false

    val vmMutator = FMutator()

    /**
     * 外部触发意图
     */
    fun dispatch(intent: I) {
        if (isVMActive || (intent is IgnoreVMActiveIntent)) {
            viewModelScope.launch {
                if (isVMActive || (intent is IgnoreVMActiveIntent)) {
                    handleIntent(intent)
                }
            }
        }
    }

    /**
     * 刷新数据
     * @param notifyRefreshing 是否通知刷新状态[isRefreshingFlow]
     * @param delayTime 延迟多少毫秒后执行
     * @param onCancel 如果当前VM未激活则会触发此回调
     * @param onRefresh 成功发起了刷新数据，在[refreshDataImpl]之前触发
     */
    fun refreshData(
        notifyRefreshing: Boolean = true,
        delayTime: Long = 0,
        onCancel: (() -> Unit)? = null,
        onRefresh: (() -> Unit)? = null,
    ) {
        if (!isVMActive) {
            onCancel?.invoke()
            return
        }
        viewModelScope.launch {
            if (!isVMActive) {
                onCancel?.invoke()
                return@launch
            }
            try {
                vmMutator.mutate {
                    if (notifyRefreshing) {
                        _isRefreshingFlow.value = true
                    }
                    _refreshDataWhenActive = false
                    delay(delayTime)
                    onRefresh?.invoke()
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
     * 如果当前VM处于激活状态则触发[refreshData]，
     * 如果当前VM处于未激活状态则等到激活状态后触发[refreshData]
     */
    fun refreshDataWhenActive() {
        refreshData(
            onCancel = {
                _refreshDataWhenActive = true
            },
        )
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

    private fun notifyVMActiveChanged() {
        if (_refreshDataWhenActive) {
            refreshData()
        }
        Handler(Looper.getMainLooper()).post {
            onVMActiveChanged()
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
                    if (isVMActive) {
                        _isVMActive = false
                        _isPausedByLifecycle = true
                    }
                }
            }

            Lifecycle.Event.ON_RESUME -> {
                synchronized(this@FViewModel) {
                    if (_isPausedByLifecycle || _isVMActive == null) {
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