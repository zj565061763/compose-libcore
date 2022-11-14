package com.sd.lib.compose.libcore.ext

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.math.absoluteValue

fun <VM : ViewModel> ViewModelStoreOwner.fKeyedVM(
    clazz: Class<VM>,
    key: String,
): VM {
    val container = getViewModel(FViewModelContainer::class.java)
    return container.addKey(
        clazz = clazz,
        key = key,
        viewModelStoreOwner = this,
    )
}

fun ViewModelStoreOwner.fIndexKeyedVM(
    vm: ViewModel,
    index: Int
) {
    val container = getViewModel(FIndexedViewModelContainer::class.java)
    container.index(
        vm = vm,
        index = index,
        viewModelStoreOwner = this
    )
}

fun ViewModelStoreOwner.fIndexKeyedVMRemoveFartherFromIndex(
    clazz: Class<out ViewModel>,
    index: Int,
    maxSize: Int,
) {
    val container = getViewModel(FIndexedViewModelContainer::class.java)
    container.removeFartherFromIndex(
        clazz = clazz,
        index = index,
        maxSize = maxSize,
        viewModelStoreOwner = this,
    )
}

internal class FViewModelContainer : ViewModel() {
    private val _vmHolder: MutableMap<Class<out ViewModel>, ViewModelInfo> = mutableMapOf()

    @Synchronized
    fun <VM : ViewModel> addKey(
        clazz: Class<VM>,
        key: String,
        viewModelStoreOwner: ViewModelStoreOwner,
    ): VM {
        val packKey = packKey(clazz, key)

        val viewModel = viewModelStoreOwner.getViewModel(
            javaClass = clazz,
            key = packKey,
        )

        val viewModelInfo = _vmHolder[clazz] ?: ViewModelInfo().also {
            _vmHolder[clazz] = it
        }

        viewModelInfo.bind(packKey, viewModel)
        return viewModel
    }

    @Synchronized
    fun removeKey(
        clazz: Class<out ViewModel>,
        key: String,
        checkOtherContainer: Boolean,
        viewModelStoreOwner: ViewModelStoreOwner,
    ): Boolean {
        val viewModelInfo = _vmHolder[clazz] ?: return false
        val packKey = packKey(clazz, key)
        return viewModelInfo.remove(packKey).also {
            if (it) {
                viewModelStoreOwner.removeViewModel(packKey)

                if (viewModelInfo.isEmpty()) {
                    _vmHolder.remove(clazz)
                }

                if (checkOtherContainer) {
                    viewModelStoreOwner
                        .getViewModel(FIndexedViewModelContainer::class.java)
                        .removeKey(clazz, key)
                }
            }
        }
    }

    @Synchronized
    fun getKey(vm: ViewModel): String {
        val clazz = vm.javaClass
        val viewModelInfo = _vmHolder[clazz] ?: return ""
        val key = viewModelInfo.getKey(vm)
        return unpackKey(clazz, key)
    }

    private class ViewModelInfo {
        private val _keyVMHolder: MutableMap<String, ViewModel> = mutableMapOf()
        private val _vmKeyHolder: MutableMap<ViewModel, String> = mutableMapOf()

        fun bind(key: String, vm: ViewModel) {
            _keyVMHolder.put(key, vm)?.also { oldVM ->
                _vmKeyHolder.remove(oldVM)
            }
            _vmKeyHolder[vm] = key
            check(_keyVMHolder.size == _vmKeyHolder.size)
        }

        fun remove(key: String): Boolean {
            return _keyVMHolder.remove(key)?.also { oldVM ->
                _vmKeyHolder.remove(oldVM)
                check(_keyVMHolder.size == _vmKeyHolder.size)
            } != null
        }

        fun getKey(vm: ViewModel): String {
            return _vmKeyHolder[vm] ?: ""
        }

        fun isEmpty(): Boolean {
            check(_keyVMHolder.size == _vmKeyHolder.size)
            return _keyVMHolder.isEmpty()
        }
    }

    companion object {
        private const val KeyPrefix = "com.sd.android.keyedViewModel"

        private fun packKey(clazz: Class<out ViewModel>, key: String): String {
            require(key.isNotEmpty()) { "key is empty" }
            require(!key.startsWith(KeyPrefix)) { "key start with $KeyPrefix" }
            val prefix = "${KeyPrefix}:${clazz.name}:"
            return prefix + key
        }

        private fun unpackKey(clazz: Class<out ViewModel>, key: String): String {
            require(key.isNotEmpty()) { "key is empty" }
            require(key.startsWith(KeyPrefix)) { "key should start with $KeyPrefix" }
            val prefix = "${KeyPrefix}:${clazz.name}:"
            return key.removePrefix(prefix)
        }
    }
}


internal class FIndexedViewModelContainer : ViewModel() {
    private val _indexHolder: MutableMap<Class<out ViewModel>, MutableMap<Int, String>> = mutableMapOf()

    @Synchronized
    fun index(
        vm: ViewModel,
        index: Int,
        viewModelStoreOwner: ViewModelStoreOwner,
    ) {
        val container = viewModelStoreOwner.getViewModel(FViewModelContainer::class.java)
        val key = container.getKey(vm)
        if (key.isEmpty()) return

        val clazz = vm.javaClass
        val holder = _indexHolder[clazz] ?: mutableMapOf<Int, String>().also {
            _indexHolder[clazz] = it
        }
        holder[index] = key
    }

    @Synchronized
    fun removeKey(
        clazz: Class<out ViewModel>,
        key: String,
    ) {
        val holder = _indexHolder[clazz] ?: return

        val it = holder.iterator()
        while (it.hasNext()) {
            val item = it.next()
            if (item.value == key) {
                it.remove()
            }
        }
    }

    @Synchronized
    fun removeFartherFromIndex(
        clazz: Class<out ViewModel>,
        index: Int,
        maxSize: Int,
        viewModelStoreOwner: ViewModelStoreOwner,
    ) {
        require(maxSize > 0) { "require maxSize > 0" }

        val holder = _indexHolder[clazz] ?: return
        if (holder.size <= maxSize) return

        val listDirtyKey = mutableListOf<String>()

        // 按距离分组
        val distanceGroup = holder.keys.groupBy { (index - it).absoluteValue }

        // 按距离排序，降序
        val listDistance = distanceGroup.keys.sortedDescending()

        for (distance in listDistance) {
            if (distance == 0) break

            val listIndex = checkNotNull(distanceGroup[distance])
            for (item in listIndex) {
                val key = checkNotNull(holder.remove(item))
                listDirtyKey.add(key)

                if (holder.size <= maxSize) break
            }

            if (holder.size <= maxSize) break
        }

        val container = viewModelStoreOwner.getViewModel(FViewModelContainer::class.java)
        listDirtyKey.forEach { dirtyKey ->
            container.removeKey(
                clazz = clazz,
                key = dirtyKey,
                checkOtherContainer = false,
                viewModelStoreOwner = viewModelStoreOwner,
            )
        }

        if (holder.isEmpty()) {
            _indexHolder.remove(clazz)
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
