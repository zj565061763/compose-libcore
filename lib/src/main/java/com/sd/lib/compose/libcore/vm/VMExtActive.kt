package com.sd.lib.compose.libcore.vm

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

@MainThread
fun FViewModel<*>.extActive(): VMExtActive {
    return getExt(InternalVMExtActive::class.java)
}

interface VMExtActive {
    /**
     * 是否处于激活状态，null表示初始状态
     */
    val isActiveFlow: StateFlow<Boolean?>

    /**
     * 设置激活状态
     */
    fun setActive(active: Boolean)

    /**
     * 设置[Lifecycle]
     */
    fun setLifecycle(lifecycle: Lifecycle?)

    /**
     * 每次状态变为激活时触发[callback]
     */
    fun onActive(callback: suspend () -> Unit)

    /**
     * 每次状态变为未激活时触发[callback]
     */
    fun onInactive(callback: suspend () -> Unit)
}

private class InternalVMExtActive : BaseViewModelExt(), VMExtActive {
    @get:Synchronized
    private var _isActive: Boolean? = null
        set(value) {
            if (vm.isDestroyed) return
            requireNotNull(value) { "Require not null value." }
            synchronized(this@InternalVMExtActive) {
                if (field != value) {
                    field = value
                    _isPausedByLifecycle = false
                    vm.viewModelScope.launch(Dispatchers.Main) {
                        _isActiveFlow.value = _isActive
                    }
                }
            }
        }

    private var _lifecycle: WeakReference<Lifecycle>? = null
    private var _isPausedByLifecycle = false

    private var _isActiveFlow: MutableStateFlow<Boolean?> = MutableStateFlow(_isActive)

    override val isActiveFlow: StateFlow<Boolean?> = _isActiveFlow.asStateFlow()

    override fun setActive(active: Boolean) {
        if (vm.isDestroyed) return
        synchronized(this@InternalVMExtActive) {
            if (active) {
                if (_isPausedByLifecycle) {
                    // ignore
                } else {
                    _isActive = true
                }
            } else {
                _isActive = false
            }
        }
    }

    override fun setLifecycle(lifecycle: Lifecycle?) {
        synchronized(this@InternalVMExtActive) {
            val old = _lifecycle?.get()
            if (old === lifecycle) return

            old?.removeObserver(_lifecycleObserver)

            if (lifecycle == null) {
                _lifecycle = null
                if (_isPausedByLifecycle) {
                    _isActive = true
                }
            } else {
                if (!vm.isDestroyed) {
                    _lifecycle = WeakReference(lifecycle)
                    lifecycle.addObserver(_lifecycleObserver)
                }
            }
        }
    }

    override fun onActive(callback: suspend () -> Unit) {
        vm.viewModelScope.launch {
            isActiveFlow
                .filter { it == true }
                .collect { callback() }
        }
    }

    override fun onInactive(callback: suspend () -> Unit) {
        vm.viewModelScope.launch {
            isActiveFlow
                .filter { it == false }
                .collect { callback() }
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
                    if (_isActive == true) {
                        _isActive = false
                        _isPausedByLifecycle = true
                    }
                }
            }

            Lifecycle.Event.ON_RESUME -> {
                synchronized(this@InternalVMExtActive) {
                    if (_isPausedByLifecycle || _isActive == null) {
                        _isActive = true
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
