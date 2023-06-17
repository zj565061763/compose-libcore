package com.sd.lib.compose.libcore.vm

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class FViewModelExtActive : BaseViewModelExt() {
    @get:Synchronized
    private var _isVMActive: Boolean? = null
        set(value) {
            if (vm.isDestroyed) return
            requireNotNull(value) { "Require not null value." }
            synchronized(this@FViewModelExtActive) {
                if (field != value) {
                    field = value
                    _isPausedByLifecycle = false
                    notifyVMActiveChanged()
                }
            }
        }

    private var _lifecycle: WeakReference<Lifecycle>? = null
    private var _isPausedByLifecycle = false

    /** 当前VM是否处于激活状态 */
    val isVMActive: Boolean get() = _isVMActive ?: false

    /**
     * 设置[Lifecycle]
     */
    fun setLifecycle(lifecycle: Lifecycle?) {
        synchronized(this@FViewModelExtActive) {
            val old = _lifecycle?.get()
            if (old === lifecycle) return

            old?.removeObserver(_lifecycleObserver)

            if (lifecycle == null) {
                _lifecycle = null
                if (_isPausedByLifecycle) {
                    _isVMActive = true
                }
            } else {
                if (!vm.isDestroyed) {
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
        if (vm.isDestroyed) return
        synchronized(this@FViewModelExtActive) {
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
                synchronized(this@FViewModelExtActive) {
                    if (isVMActive) {
                        _isVMActive = false
                        _isPausedByLifecycle = true
                    }
                }
            }

            Lifecycle.Event.ON_RESUME -> {
                synchronized(this@FViewModelExtActive) {
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

    override fun onInit() {
    }

    override fun onDestroy() {
        setLifecycle(null)
    }
}
