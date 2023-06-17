package com.sd.lib.compose.libcore.vm

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

interface VMExtActive {
    /**
     * 是否处于激活状态
     */
    val isActiveFlow: StateFlow<Boolean>

    /**
     * 设置激活状态
     */
    fun setActive(active: Boolean)

    /**
     * 设置[Lifecycle]
     */
    fun setLifecycle(lifecycle: Lifecycle?)

    /**
     * 收集激活状态变化
     */
    fun collectActive(collector: FlowCollector<Boolean>)
}

internal class InternalVMExtActive : BaseViewModelExt(), VMExtActive {
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
                        _isActiveFlow.value = _isActive ?: false
                    }
                }
            }
        }

    private var _lifecycle: WeakReference<Lifecycle>? = null
    private var _isPausedByLifecycle = false

    private var _isActiveFlow = MutableStateFlow(_isActive ?: false)

    override val isActiveFlow: StateFlow<Boolean> = _isActiveFlow.asStateFlow()

    override fun setActive(active: Boolean) {
        if (vm.isDestroyed) return
        synchronized(this@InternalVMExtActive) {
            if (active) {
                if (!_isPausedByLifecycle) {
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

    override fun collectActive(collector: FlowCollector<Boolean>) {
        vm.viewModelScope.launch {
            isActiveFlow.collect(collector)
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
