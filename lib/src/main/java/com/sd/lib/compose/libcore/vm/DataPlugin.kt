package com.sd.lib.compose.libcore.vm

import com.sd.lib.coroutine.FMutator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataPlugin(
    /** 数据互斥修改器 */
    private val mutator: FMutator = FMutator()
) : FViewModelPlugin() {

    private val _isLoadingFlow = MutableStateFlow(false)

    /** 是否正在加载中 */
    val isLoadingFlow: StateFlow<Boolean> = _isLoadingFlow.asStateFlow()

    /**
     * 加载数据[loadAwait]
     */
    fun load(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
        onLoad: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            loadAwait(
                notifyLoading = notifyLoading,
                ignoreActive = ignoreActive,
                onLoad = onLoad,
            )
        }
    }

    /**
     * 加载数据
     * @param notifyLoading 是否通知状态[isLoadingFlow]
     * @param ignoreActive 是否忽略激活状态[isActiveFlow]
     * @param onLoad 触发加载
     */
    suspend fun loadAwait(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
        onLoad: suspend () -> Unit,
    ) {
        if (isDestroyed) return
        if (isActiveFlow.value || ignoreActive) {
            try {
                mutator.mutate {
                    if (notifyLoading) {
                        _isLoadingFlow.value = true
                    }
                    onLoad()
                }
            } finally {
                if (notifyLoading) {
                    _isLoadingFlow.value = false
                }
            }
        }
    }

    override fun onInit() {
    }

    override fun onDestroy() {
    }
}