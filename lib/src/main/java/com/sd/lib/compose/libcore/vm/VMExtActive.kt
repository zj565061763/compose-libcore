package com.sd.lib.compose.libcore.vm

import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

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
     * 设置激活状态，处理业务逻辑的时候调用
     */
    fun setActive(active: Boolean)

    /**
     * 设置激活状态，由外部UI生命周期触发，业务逻辑不能调用此方法
     */
    fun setLifecycleActive(active: Boolean)

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
            requireNotNull(value) { "Require not null value." }
            synchronized(this@InternalVMExtActive) {
                if (field != value) {
                    field = value
                    _isPausedByLifecycle = false
                    updateActiveFlow()
                }
            }
        }

    private var _isPausedByLifecycle = false
    private var _isActiveFlow: MutableStateFlow<Boolean?> = MutableStateFlow(_isActive)

    override val isActiveFlow: StateFlow<Boolean?> = _isActiveFlow.asStateFlow()

    override fun setActive(active: Boolean) {
        viewModel ?: return
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

    override fun setLifecycleActive(active: Boolean) {
        viewModel ?: return
        synchronized(this@InternalVMExtActive) {
            if (active) {
                if (_isPausedByLifecycle || _isActive == null) {
                    _isActive = true
                }
            } else {
                if (_isActive == true) {
                    _isActive = false
                    _isPausedByLifecycle = true
                }
            }
        }
    }

    override fun onActive(callback: suspend () -> Unit) {
        val vm = viewModel ?: return
        vm.viewModelScope.launch {
            isActiveFlow
                .filter { it == true }
                .collect { callback() }
        }
    }

    override fun onInactive(callback: suspend () -> Unit) {
        val vm = viewModel ?: return
        vm.viewModelScope.launch {
            isActiveFlow
                .filter { it == false }
                .collect { callback() }
        }
    }

    private fun updateActiveFlow() {
        val vm = viewModel ?: return
        vm.viewModelScope.launch(Dispatchers.Main) {
            _isActiveFlow.value = _isActive
        }
    }

    override fun onInit() {
    }

    override fun onDestroy() {
    }
}
