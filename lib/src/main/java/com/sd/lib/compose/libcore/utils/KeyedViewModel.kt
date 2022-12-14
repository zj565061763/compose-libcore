package com.sd.lib.compose.libcore.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import java.util.*

@Composable
inline fun <reified VM : ViewModel> fDisposableViewModel(): VM {
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val key = rememberSaveable { "disposableViewModel:${UUID.randomUUID()}" }

    DisposableEffect(viewModelStoreOwner, key) {
        onDispose {
            viewModelStoreOwner.fKeyedVMRemove(
                clazz = VM::class.java,
                key = key
            )
        }
    }

    return viewModelStoreOwner.fKeyedVM(
        clazz = VM::class.java,
        key = key
    )
}

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

fun ViewModelStoreOwner.fKeyedVMRemove(
    clazz: Class<out ViewModel>,
    key: String,
): Boolean {
    val container = getViewModel(FViewModelContainer::class.java)
    return container.removeKey(
        clazz = clazz,
        key = key,
        viewModelStoreOwner = this,
    )
}

fun ViewModelStoreOwner.fKeyedVMSize(clazz: Class<out ViewModel>): Int {
    val container = getViewModel(FViewModelContainer::class.java)
    return container.size(clazz)
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
        viewModelStoreOwner: ViewModelStoreOwner,
    ): Boolean {
        val viewModelInfo = _vmHolder[clazz] ?: return false
        val packKey = packKey(clazz, key)
        return viewModelInfo.remove(packKey).also {
            if (it) {
                viewModelStoreOwner.removeViewModel(packKey)

                if (viewModelInfo.size() <= 0) {
                    _vmHolder.remove(clazz)
                }
            }
        }
    }

    @Synchronized
    fun size(clazz: Class<out ViewModel>): Int {
        val viewModelInfo = _vmHolder[clazz] ?: return 0
        return viewModelInfo.size()
    }

    private class ViewModelInfo {
        private val _keyHolder = mutableSetOf<String>()

        fun bind(key: String, vm: ViewModel) {
            _keyHolder.add(key)
        }

        fun remove(key: String): Boolean {
            return _keyHolder.remove(key)
        }

        fun size(): Int {
            return _keyHolder.size
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
