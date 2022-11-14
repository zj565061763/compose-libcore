package com.sd.lib.compose.libcore.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BaseUiState(coroutineScope: CoroutineScope) {
    private val _coroutineScope = coroutineScope

    private val _stateLoading = MutableSharedFlow<StateLoading?>()
    private val _stateToast = MutableSharedFlow<StateToast>()
    private val _stateClosePage = MutableStateFlow(StateClosePage())

    /** 加载框 */
    val stateLoading = _stateLoading.asSharedFlow()

    /** toast */
    val stateToast = _stateToast.asSharedFlow()

    /** 是否关闭页面 */
    val stateClosePage = _stateClosePage.asStateFlow()

    /**
     * 显示加载框
     */
    fun showLoading(
        msg: String = "",
        cancelable: Boolean = true,
    ) {
        _coroutineScope.launch {
            _stateLoading.emit(StateLoading(msg = msg, cancelable = cancelable))
        }
    }

    /**
     * 隐藏加载框
     */
    fun hideLoading() {
        _coroutineScope.launch {
            _stateLoading.emit(null)
        }
    }

    /**
     * 显示Toast
     */
    fun showToast(
        msg: String,
        longDuration: Boolean = false,
    ) {
        _coroutineScope.launch {
            _stateToast.emit(StateToast(msg, longDuration))
        }
    }

    /**
     * 关闭页面
     */
    fun closePage(confirmMsg: String = "确认要退出吗？") {
        _stateClosePage.value = StateClosePage(confirmMsg)
    }
}

data class StateLoading(
    /** 提示消息 */
    val msg: String = "",
    /** 是否可以取消 */
    val cancelable: Boolean = true,
)

data class StateToast(
    /** 提示消息 */
    val msg: String,
    val longDuration: Boolean = false,
)

data class StateClosePage(
    /** 是否关闭页面确认消息 */
    val confirmMsg: String = "",
)