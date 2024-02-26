package com.sd.lib.compose.libcore.vm

import com.sd.lib.compose.libcore.FListHolder
import com.sd.lib.coroutine.FMutator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 分页管理
 */
class PagePlugin<T> : FViewModelPlugin() {
    /** 刷新数据的页码，默认1 */
    private val _refreshPage = 1

    /** 数据互斥修改器 */
    private val _mutator = FMutator()

    /** 刷新数据 */
    private val _refreshPlugin = DataPlugin(_mutator)

    /** 加载更多数据 */
    private val _loadMorePlugin = DataPlugin(_mutator)

    private val _state = MutableStateFlow(State<T>(currentPage = _refreshPage - 1))

    /** 状态 */
    val state = _state.asStateFlow()

    /** 列表数据 */
    private val _listHolder = object : FListHolder<T>() {
        override fun onModify(oldSize: Int, newSize: Int) {
            super.onModify(oldSize, newSize)
            if (oldSize == 0) {
                // 将列表结果设置为成功
                _state.update {
                    it.copy(result = Result.success(Unit))
                }
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh(
        notifyRefreshing: Boolean = true,
        ignoreActive: Boolean = false,
        canLoad: suspend () -> Boolean = { true },
        onLoadSuccess: suspend LoadScope<T>.(PageData<T>) -> Unit = { listHolder.set(it.data) },
        onLoad: suspend LoadScope<T>.() -> Result<PageData<T>>,
    ) {
        _refreshPlugin.load(
            notifyLoading = notifyRefreshing,
            ignoreActive = ignoreActive,
            canLoad = canLoad,
            onLoad = {
                val page = _refreshPage
                with(LoadScopeImpl(page, _listHolder)) {
                    val result = onLoad()
                    handleLoadResult(result, page)
                    result.onSuccess { onLoadSuccess(it) }
                }
            },
        )
    }

    /**
     * 加载更多数据
     */
    fun loadMore(
        canLoad: suspend () -> Boolean = { !_loadMorePlugin.isLoadingFlow.value },
        onLoadSuccess: suspend LoadScope<T>.(PageData<T>) -> Unit = { listHolder.addAll(it.data) },
        onLoad: suspend LoadScope<T>.() -> Result<PageData<T>>,
    ) {
        _loadMorePlugin.load(
            notifyLoading = true,
            ignoreActive = false,
            canLoad = canLoad,
            onLoad = {
                val page = state.value.currentPage + 1
                with(LoadScopeImpl(page, _listHolder)) {
                    val result = onLoad()
                    handleLoadResult(result, page)
                    result.onSuccess { onLoadSuccess(it) }
                }
            },
        )
    }

    /**
     * 处理加载结果
     */
    private fun handleLoadResult(
        result: Result<PageData<T>>,
        page: Int,
    ) {
        result.onSuccess { pageData ->
            val newPage = if (page == _refreshPage) {
                // refresh
                _refreshPage
            } else {
                // loadMore
                val hasData = pageData.data.isNotEmpty()
                if (hasData) page + 1 else page
            }

            _state.update {
                it.copy(
                    currentPage = newPage,
                    hasMore = pageData.hasMore,
                )
            }
        }

        result.onFailure { throwable ->
            if (_listHolder.dataFlow.value.isEmpty()) {
                // 将列表结果设置为失败
                _state.update {
                    it.copy(result = Result.failure(throwable))
                }
            }
        }
    }

    override fun onInit() {
        viewModelScope.launch {
            _refreshPlugin.isLoadingFlow.collect { loading ->
                _state.update {
                    it.copy(isRefreshing = loading)
                }
            }
        }

        viewModelScope.launch {
            _loadMorePlugin.isLoadingFlow.collect { loading ->
                _state.update {
                    it.copy(isLoadingMore = loading)
                }
            }
        }

        viewModelScope.launch {
            _listHolder.dataFlow.collect { data ->
                _state.update {
                    it.copy(data = data)
                }
            }
        }
    }

    override fun onDestroy() {
    }

    data class State<T>(
        /** 列表数据加载结果 */
        val result: Result<Unit>? = null,

        /** 列表数据 */
        val data: List<T> = listOf(),

        /** 是否正在刷新 */
        val isRefreshing: Boolean = false,

        /** 是否正在加载更多 */
        val isLoadingMore: Boolean = false,

        /** 当前页码 */
        val currentPage: Int = 0,

        /** 是否还有更多数据 */
        val hasMore: Boolean = false,
    )

    data class PageData<T>(
        /** 列表数据 */
        val data: List<T>,

        /** 是否还有更多数据 */
        val hasMore: Boolean = false,
    )

    interface LoadScope<T> {
        val page: Int
        val listHolder: FListHolder<T>
    }

    private class LoadScopeImpl<T>(
        override val page: Int,
        override val listHolder: FListHolder<T>,
    ) : LoadScope<T>
}