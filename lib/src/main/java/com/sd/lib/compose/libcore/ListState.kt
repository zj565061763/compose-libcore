package com.sd.lib.compose.libcore

import com.sd.lib.result.FResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ListState<D> {
    /** 互斥锁 */
    private val _mutex = Mutex()

    /** 列表数据 */
    private val _list = mutableListOf<D>()

    private val _uiState = MutableStateFlow(ListUiState<D>())

    /** 列表ui数据 */
    val uiState = _uiState.asStateFlow()

    /**
     * 设置是否刷新中
     */
    fun setRefreshing(isRefreshing: Boolean) {
        _uiState.update { it.copy(isRefreshing = isRefreshing) }
    }

    /**
     * 设置是否加载更多中
     */
    fun setLoadingMore(isLoadingMore: Boolean) {
        _uiState.update { it.copy(isLoadingMore = isLoadingMore) }
    }

    /**
     * 设置是否还有更多数据
     */
    fun setHasMore(hasMore: Boolean) {
        _uiState.update { it.copy(hasMore = hasMore) }
    }

    /**
     * 设置数据结果
     */
    fun setResult(result: FResult<Unit>) {
        if (_list.isEmpty()) {
            _uiState.update { it.copy(result = result) }
        }
    }

    /**
     * 设置数据
     */
    suspend fun setData(list: List<D>) {
        setResult(FResult.success(Unit))
        modifyData { listData ->
            listData.clear()
            listData.addAll(list)
            true
        }
    }

    /**
     * 添加数据
     */
    suspend fun addData(
        list: List<D>,
        /** 是否全部去重，true-遍历整个列表，false-遇到符合条件的就结束 */
        distinctAll: Boolean = false,
        /** 去重条件 */
        distinct: ((oldItem: D, newItem: D) -> Boolean)? = null,
    ) {
        if (list.isEmpty()) return
        modifyData { listData ->
            if (distinct != null) {
                listData.removeWith(distinctAll) { oldItem ->
                    var result = false
                    for (newItem in list) {
                        if (distinct(oldItem, newItem)) {
                            result = true
                            break
                        }
                    }
                    result
                }
            }
            listData.addAll(list)
        }
    }

    /**
     * 更新数据，[all]表示是否遍历整个列表，
     * [modify]返回的对象如果为null，则删除原对象；如果为新对象，则替换原对象；如果为原对象，则保持不变。
     */
    suspend fun updateData(all: Boolean = false, modify: (D) -> D?) {
        modifyData { listData ->
            var result = false
            val listRemove = mutableListOf<Int>()
            for (index in listData.indices) {
                val item = listData[index]
                val newItem = modify(item)
                if (newItem == null) {
                    listRemove.add(index)
                    result = true
                } else if (newItem !== item) {
                    listData[index] = newItem
                    result = true
                }
                if (result && !all) break
            }
            listRemove.forEach { listData.removeAt(it) }
            result
        }
    }

    /**
     * 删除数据，[all]表示是否遍历整个列表，
     */
    suspend fun removeData(all: Boolean = false, predicate: (D) -> Boolean) {
        modifyData { listData ->
            listData.removeWith(all = all, predicate = predicate)
        }
    }

    /**
     * 修改数据
     */
    suspend fun modifyData(modify: (list: MutableList<D>) -> Boolean) {
        _mutex.withLock {
            withContext(Dispatchers.IO) {
                if (modify(_list)) _list.toList() else null
            }?.also { data ->
                _uiState.update { it.copy(data = data) }
            }
        }
    }
}

data class ListUiState<T>(
    /** 列表数据加载结果 */
    val result: FResult<Unit> = FResult.loading(),

    /** 列表数据 */
    val data: List<T> = listOf(),

    /** 是否正在刷新 */
    val isRefreshing: Boolean = false,

    /** 是否正在加载更多 */
    val isLoadingMore: Boolean = false,

    /** 是否还有更多数据 */
    val hasMore: Boolean = false,
)

/**
 * 根据条件移除元素
 */
private fun <T> MutableList<T>.removeWith(all: Boolean = false, predicate: (T) -> Boolean): Boolean {
    var result = false
    with(iterator()) {
        while (hasNext()) {
            if (predicate(next())) {
                remove()
                result = true
                if (!all) break
            }
        }
    }
    return result
}