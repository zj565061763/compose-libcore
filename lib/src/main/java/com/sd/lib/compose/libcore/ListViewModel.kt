package com.sd.lib.compose.libcore

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

abstract class ListViewModel<I, D> : BaseViewModel<I>() {
    protected val listState = ListState<D>()

    val listUiState = listState.uiState

    /**
     * 加载更多数据
     */
    fun loadMoreData() {
        if (!isActiveState) return
        if (listUiState.value.isLoadingMore) return

        viewModelScope.launch {
            if (!isActiveState) return@launch
            if (listUiState.value.isLoadingMore) return@launch
            try {
                listState.setLoadingMore(true)
                mutator.mutate {
                    loadMoreDataImpl()
                }
            } finally {
                listState.setLoadingMore(false)
            }
        }
    }

    /**
     * 加载更多数据
     */
    protected abstract suspend fun loadMoreDataImpl()

    init {
        viewModelScope.launch {
            isRefreshing.collect {
                listState.setRefreshing(it)
            }
        }
    }
}