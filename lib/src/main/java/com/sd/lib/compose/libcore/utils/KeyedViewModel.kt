package com.sd.lib.compose.libcore.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import java.util.UUID

@Composable
inline fun <reified VM : ViewModel> fDisposableViewModel(): VM {
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val key = rememberSaveable(viewModelStoreOwner) { "disposableViewModel:${UUID.randomUUID()}" }
    val packKey = rememberSaveable(key) { packKey(VM::class.java, key) }

    DisposableEffect(viewModelStoreOwner, packKey) {
        onDispose {
            viewModelStoreOwner.vmRemove(packKey)
        }
    }

    return viewModelStoreOwner.vmGet(
        javaClass = VM::class.java,
        key = packKey,
    )
}

private const val KEY_PREFIX = "com.sd.android.keyedViewModel"

@PublishedApi
internal fun packKey(clazz: Class<out ViewModel>, key: String): String {
    require(key.isNotEmpty()) { "key is empty" }
    require(!key.startsWith(KEY_PREFIX)) { "key start with $KEY_PREFIX" }
    return "${KEY_PREFIX}:${clazz.name}:${key}"
}

@PublishedApi
internal fun <VM : ViewModel> ViewModelStoreOwner.vmGet(
    javaClass: Class<VM>,
    key: String? = null,
    factory: ViewModelProvider.Factory? = null,
    extras: CreationExtras = if (this is HasDefaultViewModelProviderFactory) {
        this.defaultViewModelCreationExtras
    } else {
        CreationExtras.Empty
    },
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

@PublishedApi
internal fun ViewModelStoreOwner.vmRemove(key: String?) {
    if (key.isNullOrEmpty()) return
    vmHolder().remove(key)?.let { viewModel ->
        ViewModel::class.java.run {
            try {
                getDeclaredMethod("clear")
            } catch (e: Exception) {
                null
            } ?: error("clear method was not found in ${ViewModel::class.java.name}")
        }.let { method ->
            method.isAccessible = true
            method.invoke(viewModel)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun ViewModelStoreOwner.vmHolder(): MutableMap<String, ViewModel> {
    return ViewModelStore::class.java.run {
        try {
            getDeclaredField("map")
        } catch (e: Exception) {
            null
        } ?: try {
            getDeclaredField("mMap")
        } catch (e: Exception) {
            null
        } ?: error("map field was not found in ${ViewModelStore::class.java.name}")
    }.let { field ->
        field.isAccessible = true
        field.get(viewModelStore) as MutableMap<String, ViewModel>
    }
}