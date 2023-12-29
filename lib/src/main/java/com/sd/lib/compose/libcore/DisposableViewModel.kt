package com.sd.lib.compose.libcore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
inline fun <reified VM : ViewModel> String.fDisposableVM(
    factory: @Composable (key: String) -> VM = { viewModel(key = it) },
): VM {
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val key = rememberSaveable { "com.sd.android.keyedViewModel.key:$this" }
    DisposableEffect(viewModelStoreOwner, key) {
        onDispose {
            viewModelStoreOwner.viewModelStore.vmRemove(key)
        }
    }
    return factory(key)
}

@PublishedApi
internal fun ViewModelStore.vmRemove(key: String): Boolean {
    return vmHolder().vmRemove(key)
}

/**
 * 根据[key]移除[ViewModel]
 */
private fun MutableMap<String, ViewModel>.vmRemove(key: String): Boolean {
    val viewModel = remove(key) ?: return false
    ViewModel::class.java.run {
        try {
            getDeclaredMethod("clear")
        } catch (e: Exception) {
            null
        } ?: error("clear method was not found in ${ViewModel::class.java.name}")
    }.let { method ->
        method.isAccessible = true
        method.invoke(viewModel)
        return true
    }
}

/**
 * 获取[ViewModelStore]内保存[ViewModel]的[Map]
 */
private fun ViewModelStore.vmHolder(): MutableMap<String, ViewModel> {
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
        @Suppress("UNCHECKED_CAST")
        field.get(this@vmHolder) as MutableMap<String, ViewModel>
    }
}