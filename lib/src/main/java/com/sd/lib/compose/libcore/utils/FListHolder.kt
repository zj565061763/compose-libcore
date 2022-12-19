package com.sd.lib.compose.libcore.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

open class FListHolder<D> {
    /** 互斥锁 */
    private val _mutex = Mutex()

    /** 列表数据 */
    private val _list = mutableListOf<D>()

    private val _data = MutableStateFlow(listOf<D>())

    /** 数据流 */
    val data = _data.asStateFlow()

    /**
     * 设置数据
     */
    open suspend fun setData(list: List<D>) {
        modifyData { listData ->
            listData.clear()
            listData.addAll(list)
            true
        }
    }

    /**
     * 添加数据
     */
    open suspend fun addData(
        list: List<D>,
        /** 是否全部去重，true-遍历整个列表，false-遇到符合条件的就结束 */
        distinctAll: Boolean = false,
        /** 去重条件 */
        distinct: ((oldItem: D, newItem: D) -> Boolean)? = null,
    ) {
        if (list.isEmpty()) return
        if (distinctAll && distinct == null) throw IllegalArgumentException("Did you forget the distinct parameter?")

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
    open suspend fun updateData(
        all: Boolean = false,
        modify: (D) -> D?,
    ) {
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
    open suspend fun removeData(
        all: Boolean = false,
        predicate: (D) -> Boolean,
    ) {
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
                _data.value = data
            }
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