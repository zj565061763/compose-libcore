package com.sd.lib.compose.libcore

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 创建[ComposeViewModelScope]，根据参数[maxSize]指定最多保存几个[ViewModel]，
 * 如果超过指定的个数，根据LRU算法移除不常用的[ViewModel]，
 * 如果调用此方法的地方在组合中被移除，[ComposeViewModelScope]会清空所有保存的[ViewModel]
 */
@Composable
inline fun <reified VM : ViewModel> fRememberVMScope(maxSize: Int = 5): ComposeViewModelScope<VM> {
    val vmClass = VM::class.java
    val scope = remember(vmClass) { ViewModelScopeImpl(vmClass) }.apply {
        this.setMaxSize(maxSize)
    }
    DisposableEffect(scope) {
        onDispose {
            scope.destroy()
        }
    }
    return scope
}

/**
 * 创建[ComposeViewModelScope]，根据参数[maxSize]指定最多保存几个[ViewModel]，
 * 如果超过指定的个数，根据LRU算法移除不常用的[ViewModel]，
 */
@Composable
inline fun <reified VM : ViewModel> fVMScope(maxSize: Int = 5): ComposeViewModelScope<VM> {
    val viewModel = viewModel<VMScopeViewModel>()
    return viewModel.getScope(VM::class.java).apply {
        this.setMaxSize(maxSize)
    }
}

interface ComposeViewModelScope<VM : ViewModel> {
    /**
     * 根据[key]获取[ViewModel]
     */
    @Composable
    fun getViewModel(key: String): VM

    /**
     * 根据[key]获取[ViewModel]，如果[ViewModel]不存在，从[factory]创建
     */
    @Composable
    fun getViewModel(key: String, factory: (CreationExtras) -> VM): VM

    /**
     * 根据[key]获取[ViewModel]，每次调用此方法都会从[factory]中获取
     */
    @Composable
    fun createViewModel(key: String, factory: @Composable (CreateVMParams<VM>) -> VM): VM
}

/**
 * 创建[ViewModel]参数
 */
data class CreateVMParams<VM : ViewModel>(
    val viewModelStoreOwner: ViewModelStoreOwner,
    val key: String,
    val vmClass: Class<VM>,
)

@PublishedApi
internal class ViewModelScopeImpl<VM : ViewModel>(
    private val clazz: Class<VM>,
) : ComposeViewModelScope<VM>, ViewModelStoreOwner {

    private var _isDestroyed = false

    override val viewModelStore: ViewModelStore = ViewModelStore()
    private val _vmHolder: MutableMap<String, ViewModel> = viewModelStore.vmHolder()

    private val _lruCache = object : LruCache<String, Int>(Int.MAX_VALUE) {
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Int?, newValue: Int?) {
            if (evicted && !_isDestroyed) {
                _vmHolder.vmRemove(key)
            }
        }
    }

    @Composable
    override fun getViewModel(key: String): VM {
        return createViewModel(key) { params ->
            viewModel(
                viewModelStoreOwner = params.viewModelStoreOwner,
                key = params.key,
                modelClass = params.vmClass,
            )
        }
    }

    @Composable
    override fun getViewModel(key: String, factory: (CreationExtras) -> VM): VM {
        val factoryUpdated by rememberUpdatedState(factory)

        val defaultFactory = remember {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return factoryUpdated(CreationExtras.Empty) as T
                }

                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return factoryUpdated(extras) as T
                }
            }
        }

        return createViewModel(key) { params ->
            viewModel(
                viewModelStoreOwner = params.viewModelStoreOwner,
                key = params.key,
                factory = defaultFactory,
                modelClass = params.vmClass,
            )
        }
    }

    @Composable
    override fun createViewModel(key: String, factory: @Composable (CreateVMParams<VM>) -> VM): VM {
        if (_isDestroyed) error("Scope is destroyed.")

        @Suppress("NAME_SHADOWING")
        val key = "com.sd.android.keyedViewModel:${key}"
        val defaultOwner = checkNotNull(LocalViewModelStoreOwner.current)

        val viewModelStoreOwner = remember(defaultOwner) {
            if (defaultOwner is HasDefaultViewModelProviderFactory) {
                ViewModelStoreOwnerHasDefault(
                    owner = this@ViewModelScopeImpl,
                    factory = defaultOwner,
                )
            } else {
                this@ViewModelScopeImpl
            }
        }

        val params = CreateVMParams(
            viewModelStoreOwner = viewModelStoreOwner,
            key = key,
            vmClass = clazz,
        )

        return factory(params).also { vm ->
            check(vm === _vmHolder[key]) { "ViewModel was not found with key:$key." }
            _lruCache.put(key, 0)
        }
    }

    /**
     * 最多保存几个[ViewModel]
     */
    fun setMaxSize(maxSize: Int) {
        if (_isDestroyed) return
        if (_lruCache.maxSize() != maxSize) {
            _lruCache.resize(maxSize)
        }
    }

    /**
     * 销毁所有[ViewModel]
     */
    fun destroy() {
        _isDestroyed = true
        viewModelStore.clear()
        _lruCache.evictAll()
    }

    private class ViewModelStoreOwnerHasDefault(
        private val owner: ViewModelStoreOwner,
        private val factory: HasDefaultViewModelProviderFactory,
    ) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

        override val defaultViewModelProviderFactory: ViewModelProvider.Factory
            get() = factory.defaultViewModelProviderFactory

        override val defaultViewModelCreationExtras: CreationExtras
            get() = factory.defaultViewModelCreationExtras

        override val viewModelStore: ViewModelStore
            get() = owner.viewModelStore
    }
}

@PublishedApi
internal class VMScopeViewModel : ViewModel() {
    private val _scopeHolder: MutableMap<Class<out ViewModel>, ViewModelScopeImpl<out ViewModel>> = hashMapOf()

    fun <VM : ViewModel> getScope(vmClass: Class<VM>): ViewModelScopeImpl<VM> {
        val scope = _scopeHolder.getOrPut(vmClass) { ViewModelScopeImpl(vmClass) }
        @Suppress("UNCHECKED_CAST")
        return scope as ViewModelScopeImpl<VM>
    }

    override fun onCleared() {
        super.onCleared()
        _scopeHolder.forEach { it.value.destroy() }
        _scopeHolder.clear()
    }
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