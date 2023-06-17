package com.sd.lib.compose.libcore.vm

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

interface VMExtActive {
    /**
     * 是否处于激活状态
     */
    val isExtActive: Boolean

    /**
     * 设置激活状态
     */
    fun setExtActive(active: Boolean)

    /**
     * 设置[Lifecycle]
     */
    fun setLifecycle(lifecycle: Lifecycle?)
}

internal class InternalVMExtActive : BaseViewModelExt(), VMExtActive {
    @get:Synchronized
    private var _isExtActive: Boolean? = null
        set(value) {
            if (vm.isDestroyed) return
            requireNotNull(value) { "Require not null value." }
            synchronized(this@InternalVMExtActive) {
                if (field != value) {
                    field = value
                    _isPausedByLifecycle = false
                    notifyVMActiveChanged()
                }
            }
        }

    private var _lifecycle: WeakReference<Lifecycle>? = null
    private var _isPausedByLifecycle = false

    override val isExtActive: Boolean
        get() = _isExtActive ?: false

    override fun setLifecycle(lifecycle: Lifecycle?) {
        synchronized(this@InternalVMExtActive) {
            val old = _lifecycle?.get()
            if (old === lifecycle) return

            old?.removeObserver(_lifecycleObserver)

            if (lifecycle == null) {
                _lifecycle = null
                if (_isPausedByLifecycle) {
                    _isExtActive = true
                }
            } else {
                if (!vm.isDestroyed) {
                    _lifecycle = WeakReference(lifecycle)
                    lifecycle.addObserver(_lifecycleObserver)
                }
            }
        }
    }

    override fun setExtActive(active: Boolean) {
        if (vm.isDestroyed) return
        synchronized(this@InternalVMExtActive) {
            if (active) {
                if (!_isPausedByLifecycle) {
                    _isExtActive = true
                }
            } else {
                _isExtActive = false
            }
        }
    }

    private fun notifyVMActiveChanged() {
        vm.viewModelScope.launch(Dispatchers.Main) {
            // TODO notify
        }
    }

    /**
     * 生命周期观察者
     */
    private val _lifecycleObserver = LifecycleEventObserver { _, event ->
        if (vm.isDestroyed) {
            setLifecycle(null)
            return@LifecycleEventObserver
        }

        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                synchronized(this@InternalVMExtActive) {
                    if (isExtActive) {
                        _isExtActive = false
                        _isPausedByLifecycle = true
                    }
                }
            }

            Lifecycle.Event.ON_RESUME -> {
                synchronized(this@InternalVMExtActive) {
                    if (_isPausedByLifecycle || _isExtActive == null) {
                        _isExtActive = true
                    }
                }
            }

            Lifecycle.Event.ON_DESTROY -> {
                setLifecycle(null)
            }

            else -> {}
        }
    }

    override fun onInit() {
    }

    override fun onDestroy() {
        setLifecycle(null)
    }
}
