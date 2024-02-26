package com.sd.lib.compose.libcore

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

open class FListHolder<T>(
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
) {
    private val _list: MutableList<T> = mutableListOf()
    private val _dataFlow: MutableStateFlow<List<T>> = MutableStateFlow(emptyList())

    /** 数据流 */
    val dataFlow: StateFlow<List<T>> = _dataFlow.asStateFlow()

    /**
     * 设置数据
     */
    suspend fun set(list: List<T>) {
        modify { listData ->
            listData.clear()
            listData.addAll(list)
            true
        }
    }

    /**
     * 清空数据
     */
    suspend fun clear() {
        modify { listData ->
            listData.clear()
            true
        }
    }

    /**
     * 添加数据
     */
    suspend fun addAll(list: List<T>) {
        if (list.isEmpty()) return
        modify { listData ->
            listData.addAll(list)
        }
    }

    /**
     * 添加数据并去重，删除[FListHolder]中重复的数据
     */
    suspend fun addAllDistinct(
        list: List<T>,
        /** 去重条件，返回true表示数据重复 */
        distinct: (oldItem: T, newItem: T) -> Boolean,
    ) {
        if (list.isEmpty()) return
        modify { listData ->
            listData.removeAll { oldItem ->
                var result = false
                for (newItem in list) {
                    if (distinct(oldItem, newItem)) {
                        result = true
                        break
                    }
                }
                result
            }
            listData.addAll(list)
        }
    }

    /**
     * 添加数据并去重，删除[list]中重复的数据
     */
    suspend fun addAllDistinctInput(
        list: List<T>,
        /** 去重条件，返回true表示数据重复 */
        distinct: (oldItem: T, newItem: T) -> Boolean,
    ) {
        if (list.isEmpty()) return
        modify { listData ->
            val mutableList = list.toMutableList()
            mutableList.removeAll { newItem ->
                var result = false
                for (oldItem in listData) {
                    if (distinct(oldItem, newItem)) {
                        result = true
                        break
                    }
                }
                result
            }
            listData.addAll(mutableList)
        }
    }

    /**
     * 如果[block]返回的对象 !== 原对象，则替换并结束遍历
     */
    suspend fun replaceFirst(block: (T) -> T) {
        modify { listData ->
            var result = false
            for (index in listData.indices) {
                val item = listData[index]
                val newItem = block(item)
                if (newItem !== item) {
                    listData[index] = newItem
                    result = true
                    break
                }
            }
            result
        }
    }

    /**
     * [block]返回的对象替换原对象
     */
    suspend fun replaceAll(block: (T) -> T) {
        modify { listData ->
            var result = false
            for (index in listData.indices) {
                val item = listData[index]
                val newItem = block(item)
                if (newItem !== item) {
                    listData[index] = newItem
                    result = true
                }
            }
            result
        }
    }

    /**
     * 删除第一个[predicate]为true的数据
     */
    suspend fun removeFirst(predicate: (T) -> Boolean) {
        modify { listData ->
            listData.removeFirst(predicate)
        }
    }

    /**
     * 删除所有[predicate]为true的数据
     */
    suspend fun removeAll(predicate: (T) -> Boolean) {
        modify { listData ->
            listData.removeAll(predicate)
        }
    }

    /**
     * 修改数据
     */
    suspend fun modify(block: (list: MutableList<T>) -> Boolean) {
        withContext(dispatcher) {
            val oldSize = _list.size
            if (block(_list) || oldSize != _list.size) {
                ModifyResult(list = _list.toList(), oldSize = oldSize)
            } else {
                null
            }
        }?.also { result ->
            _dataFlow.value = result.list
            onModify(
                oldSize = result.oldSize,
                newSize = result.list.size,
            )
        }
    }

    protected open fun onModify(oldSize: Int, newSize: Int) {}
}

private data class ModifyResult<T>(
    val list: List<T>,
    val oldSize: Int,
)

/**
 * 根据条件移除元素
 */
private fun <T> MutableList<T>.removeFirst(
    predicate: (T) -> Boolean,
): Boolean {
    with(iterator()) {
        while (hasNext()) {
            if (predicate(next())) {
                remove()
                return true
            }
        }
    }
    return false
}