package com.sd.lib.compose.libcore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class FListHolder<T> {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _dataDispatcher = Dispatchers.Default.limitedParallelism(1)

    /** 列表数据 */
    private val _list = mutableListOf<T>()

    private val _dataFlow = MutableStateFlow(emptyList<T>())

    /** 数据流 */
    val dataFlow = _dataFlow.asStateFlow()

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
        modify { listData ->
            listData.addAll(list)
        }
    }

    /**
     * 添加数据并去重，删除原数据中重复的数据
     */
    suspend fun addAllDistinct(
        list: List<T>,
        /** 去重条件 */
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
        withContext(_dataDispatcher) {
            val oldSize = _list.size
            if (block(_list) || oldSize != _list.size) {
                _list.toList()
            } else {
                null
            }
        }?.also { data ->
            _dataFlow.value = data
        }
    }
}

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