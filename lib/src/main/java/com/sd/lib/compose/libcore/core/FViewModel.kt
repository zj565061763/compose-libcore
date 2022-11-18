package com.sd.lib.compose.libcore.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.lib.coroutine.FMutator
import com.sd.lib.coroutine.FScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class FViewModel<I> : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)

    /** 是否正在刷新 */
    val isRefreshing = _isRefreshing.asStateFlow()

    val vmMutator = FMutator()
    val vmScope = FScope(viewModelScope)

    /** 设置当前VM是否处于激活状态，只有激活状态才会处理事件 */
    @Volatile
    var isActiveState: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                onActiveStateChanged()
            }
        }

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
            if (!isActiveState) return@launch
            try {
                if (notifyRefreshing) {
                    _isRefreshing.value = true
                }
                vmMutator.mutate {
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

    /**
     * 处理意图
     */
    protected abstract suspend fun handleIntent(intent: I)

    /**
     * 刷新数据
     */
    protected abstract suspend fun refreshDataImpl()

    /**
     * 激活状态变化
     */
    protected open fun onActiveStateChanged() {}
}

interface IgnoreActiveStateIntent