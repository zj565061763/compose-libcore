package com.sd.lib.compose.libcore.vm

import com.sd.lib.coroutine.FMutator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 数据加载
 */
class DataPlugin @JvmOverloads constructor(
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
        canLoad: suspend () -> Boolean = { true },
        onLoad: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            loadAwait(
                notifyLoading = notifyLoading,
                ignoreActive = ignoreActive,
                canLoad = canLoad,
                onLoad = onLoad,
            )
        }
    }

    /**
     * 加载数据
     * @param notifyLoading 是否通知[isLoadingFlow]
     * @param ignoreActive 是否忽略激活状态[isActiveFlow]
     * @param canLoad 返回是否可以触发加载
     * @param onLoad 触发加载
     */
    suspend fun loadAwait(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
        canLoad: suspend () -> Boolean = { true },
        onLoad: suspend () -> Unit,
    ) {
        if (isDestroyed) return
        if (isActiveFlow.value || ignoreActive) {
            if (canLoad()) {
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
    }

    override fun onInit() {
    }

    override fun onDestroy() {
    }
}