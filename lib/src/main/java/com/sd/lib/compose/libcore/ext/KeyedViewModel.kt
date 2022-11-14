package com.sd.lib.compose.libcore.ext

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.math.abs

fun <VM : ViewModel> ViewModelStoreOwner.fKeyedViewModel(
    clazz: Class<VM>,
    key: String,
    index: Int? = null,
): VM {
    val transformKey = FViewModelContainer.transformKey(clazz, key)
    return getViewModel(clazz, transformKey).also {
        getViewModel(FViewModelContainer::class.java).addKey(
            clazz = clazz,
            key = key,
            index = index,
            viewModelStoreOwner = this,
        )
    }
}

fun <VM : ViewModel> ViewModelStoreOwner.fRemoveKeyedViewModel(
    clazz: Class<VM>,
    key: String,
) {
    getViewModel(FViewModelContainer::class.java).removeKey(
        clazz = clazz,
        key = key,
    )
}

fun <VM : ViewModel> ViewModelStoreOwner.fRemoveKeyedViewModelFartherFromIndex(
    clazz: Class<VM>,
    index: Int,
    maxSize: Int,
) {
    getViewModel(FViewModelContainer::class.java).removeKeyFartherFromIndex(
        clazz = clazz,
        index = index,
        maxSize = maxSize,
    )
}

class FViewModelContainer : ViewModel() {
    /** 保存每个key的信息 */
    private val _keyHolder: MutableMap<Class<out ViewModel>, MutableMap<String, KeyInfo>> = mutableMapOf()

    /** 保存每个index对应的key */
    private val _indexHolder: MutableMap<Class<out ViewModel>, MutableMap<Int, String>> = mutableMapOf()

    private var _viewModelStoreOwner: ViewModelStoreOwner? = null

    /**
     * 添加key
     */
    @Synchronized
    fun addKey(
        clazz: Class<out ViewModel>,
        key: String,
        index: Int? = null,
        viewModelStoreOwner: ViewModelStoreOwner,
    ) {
        val key = transformKey(clazz, key)
        _viewModelStoreOwner = viewModelStoreOwner

        val keyInfoHolder = _keyHolder[clazz] ?: mutableMapOf<String, KeyInfo>().also {
            _keyHolder[clazz] = it
        }

        val oldKeyInfo = keyInfoHolder[key]
        if (index == null && oldKeyInfo?.index != null) {
            // 不能用null覆盖非null
            return
        }

        if (oldKeyInfo == null || oldKeyInfo.index != index) {
            if (index != null) {
                val indexInfoHolder = _indexHolder[clazz] ?: mutableMapOf<Int, String>().also {
                    _indexHolder[clazz] = it
                }

                val oldKey = indexInfoHolder.put(index, key)
                if (oldKey != null && oldKey != key) {
                    // index被新的key覆盖，移除旧的key和对应的ViewModel
                    keyInfoHolder.remove(oldKey)
                    viewModelStoreOwner.removeViewModel(oldKey)
                }
            }

            // 保存信息
            keyInfoHolder[key] = KeyInfo(key = key, index = index)
        }
    }

    @Synchronized
    fun removeKey(
        clazz: Class<out ViewModel>,
        key: String,
    ) {
        val viewModelStoreOwner = _viewModelStoreOwner ?: return
        val keyInfoHolder = _keyHolder[clazz] ?: return

        val key = transformKey(clazz, key)
        val info = keyInfoHolder.remove(key) ?: return

        if (info.index != null) {
            _indexHolder[clazz]?.let { holder ->
                holder.remove(info.index)
                if (holder.isEmpty()) _indexHolder.remove(clazz)
            }
        }

        viewModelStoreOwner.removeViewModel(key)

        if (keyInfoHolder.isEmpty()) {
            _keyHolder.remove(clazz)
        }
    }

    /**
     * 移除离[index]较远的key
     */
    @Synchronized
    fun removeKeyFartherFromIndex(
        clazz: Class<out ViewModel>,
        index: Int,
        maxSize: Int,
    ) {
        require(maxSize > 0) { "require maxSize > 0" }
        val keyInfoHolder = _keyHolder[clazz] ?: return

        val overSize = keyInfoHolder.size - maxSize
        if (overSize <= 0) return

        val viewModelStoreOwner = _viewModelStoreOwner ?: return
        val listDirtyKey = mutableListOf<String>()

        // 按距离分组
        val distanceGroup = keyInfoHolder.values.groupBy { abs(index - (it.index ?: 0)) }

        // 按距离排序，降序
        val listDistance = distanceGroup.keys.sortedDescending()

        for (distance in listDistance) {
            if (distance == 0) break

            val listInfo = checkNotNull(distanceGroup[distance])
            for (info in listInfo) {
                keyInfoHolder.remove(info.key)
                listDirtyKey.add(info.key)

                if (info.index != null) {
                    _indexHolder[clazz]?.let { holder ->
                        holder.remove(info.index)
                        if (holder.isEmpty()) _indexHolder.remove(clazz)
                    }
                }

                if (keyInfoHolder.size <= maxSize) break
            }

            if (keyInfoHolder.size <= maxSize) break
        }

        listDirtyKey.forEach { dirtyKey ->
            viewModelStoreOwner.removeViewModel(dirtyKey)
        }

        if (keyInfoHolder.isEmpty()) {
            _keyHolder.remove(clazz)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _viewModelStoreOwner = null
    }

    private data class KeyInfo(
        val key: String,
        val index: Int?
    )

    companion object {
        private const val KeyPrefix = "com.sd.android.keyedViewModel"

        internal fun transformKey(clazz: Class<out ViewModel>, key: String): String {
            require(key.isNotEmpty()) { "key is empty" }
            require(!key.startsWith(KeyPrefix)) { "key start with $KeyPrefix" }
            return "${KeyPrefix}:${clazz.name}:${key}"
        }
    }
}


private fun ViewModelStoreOwner.removeViewModel(key: String?) {
    if (key.isNullOrEmpty()) return

    val map = ViewModelStore::class.java.getDeclaredField("mMap").apply {
        this.isAccessible = true
    }.get(viewModelStore) as HashMap<String, ViewModel>

    map.remove(key)?.let { viewModel ->
        val method = ViewModel::class.java.getDeclaredMethod("clear").apply {
            this.isAccessible = true
        }
        method.invoke(viewModel)
    }
}

private fun <VM : ViewModel> ViewModelStoreOwner.getViewModel(
    javaClass: Class<VM>,
    key: String? = null,
    factory: ViewModelProvider.Factory? = null,
    extras: CreationExtras = if (this is HasDefaultViewModelProviderFactory) {
        this.defaultViewModelCreationExtras
    } else {
        CreationExtras.Empty
    }
): VM {
    val provider = if (factory != null) {
        ViewModelProvider(this.viewModelStore, factory, extras)
    } else if (this is HasDefaultViewModelProviderFactory) {
        ViewModelProvider(this.viewModelStore, this.defaultViewModelProviderFactory, extras)
    } else {
        ViewModelProvider(this)
    }
    return if (key != null) {
        provider[key, javaClass]
    } else {
        provider[javaClass]
    }
}
