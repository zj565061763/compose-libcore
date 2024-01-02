package com.sd.lib.compose.libcore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

open class FListHolder<T> {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _dataDispatcher = Dispatchers.Default.limitedParallelism(1)

    /** 列表数据 */
    private val _list = mutableListOf<T>()

    private val _dataFlow = MutableStateFlow(listOf<T>())

    /** 数据流 */
    val dataFlow = _dataFlow.asStateFlow()

    /**
     * 设置数据
     */
    open suspend fun set(list: List<T>) {
        modify { listData ->
            listData.clear()
            listData.addAll(list)
        }
    }

    /**
     * 清空数据
     */
    open suspend fun clear() {
        modify { listData ->
            val oldSize = listData.size
            listData.clear()
            oldSize > 0
        }
    }

    /**
     * 添加数据
     */
    open suspend fun addData(
        list: List<T>,
        /** 是否全部去重，true-遍历整个列表，false-遇到符合条件的就结束 */
        distinctAll: Boolean = false,
        /** 去重条件 */
        distinct: ((oldItem: T, newItem: T) -> Boolean)? = null,
    ) {
        if (list.isEmpty()) return
        if (distinctAll && distinct == null) throw IllegalArgumentException("Did you forget the distinct parameter?")

        modify { listData ->
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
    open suspend fun updateData(
        all: Boolean = false,
        modify: (T) -> T?,
    ) {
        modify { listData ->
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
     * 如果[block]返回的对象 !== 原对象，则替换并结束遍历
     */
    open suspend fun replaceFirst(block: (T) -> T) {
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
    open suspend fun replaceAll(block: (T) -> T) {
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
    open suspend fun removeFirst(predicate: (T) -> Boolean) {
        modify { listData ->
            listData.removeFirst(predicate)
        }
    }

    /**
     * 删除所有[predicate]为true的数据
     */
    open suspend fun removeAll(predicate: (T) -> Boolean) {
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
private fun <T> MutableList<T>.removeWith(
    all: Boolean = false,
    predicate: (T) -> Boolean,
): Boolean {
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