package com.sd.lib.compose.libcore.core

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

abstract class FActiveViewModel<I> : FViewModel<I>() {
    @get:Synchronized
    private var _isVMActive: Boolean? = null
        set(value) {
            if (isDestroyed) return
            requireNotNull(value) { "Require not null value." }
            synchronized(this@FActiveViewModel) {
                if (field != value) {
                    field = value
                    _isPausedByLifecycle = false
                    notifyVMActiveChanged()
                }
            }
        }

    private var _lifecycle: WeakReference<Lifecycle>? = null
    private var _isPausedByLifecycle = false

    /** 当前VM是否处于激活状态，只有激活状态才会处理事件 */
    val isVMActive: Boolean get() = _isVMActive ?: false

    /**
     * 观察生命周期
     */
    fun setLifecycle(lifecycle: Lifecycle?) {
        synchronized(this@FActiveViewModel) {
            val old = _lifecycle?.get()
            if (old === lifecycle) return

            old?.removeObserver(_lifecycleObserver)

            if (lifecycle == null) {
                _lifecycle = null
                if (_isPausedByLifecycle) {
                    _isVMActive = true
                }
            } else {
                if (!isDestroyed) {
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
        if (isDestroyed) return
        synchronized(this@FActiveViewModel) {
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
        viewModelScope.launch(Dispatchers.Main) {
            onVMActiveChanged()
        }
    }

    /**
     * 当前VM激活状态变化（UI线程）
     */
    protected open fun onVMActiveChanged() {}

    /**
     * 生命周期观察者
     */
    private val _lifecycleObserver = LifecycleEventObserver { _, event ->
        if (isDestroyed) {
            setLifecycle(null)
            return@LifecycleEventObserver
        }

        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                synchronized(this@FActiveViewModel) {
                    if (isVMActive) {
                        _isVMActive = false
                        _isPausedByLifecycle = true
                    }
                }
            }

            Lifecycle.Event.ON_RESUME -> {
                synchronized(this@FActiveViewModel) {
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

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        setLifecycle(null)
    }
}
