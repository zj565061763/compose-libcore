package com.sd.lib.compose.libcore.vm

import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope
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
     * 是否出于激活状态，null表示初始状态
     */
    val isActive: Boolean?

    /**
     * 设置激活状态（业务逻辑）
     */
    fun setActive(active: Boolean)

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
    private var _isActiveFlow: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    override val isActiveFlow: StateFlow<Boolean?> = _isActiveFlow.asStateFlow()

    override val isActive: Boolean? get() = isActiveFlow.value

    override fun setActive(active: Boolean) {
        viewModel ?: return
        _isActiveFlow.value = active
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

    override fun onInit() {
    }

    override fun onDestroy() {
    }
}
