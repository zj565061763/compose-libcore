package com.sd.lib.compose.libcore

import android.os.Looper
import androidx.annotation.MainThread
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
 * 创建[ComposeViewModelScope]，
 * 如果调用此方法的地方在组合中被移除，[ComposeViewModelScope]会清空所有保存的[ViewModel]
 */
@Composable
inline fun <reified VM : ViewModel> fRememberVMScope(): ComposeViewModelScope<VM> {
    val vmClass = VM::class.java
    val scope = remember(vmClass) { ViewModelScopeImpl(vmClass) }
    DisposableEffect(scope) {
        onDispose {
            scope.destroy()
        }
    }
    return scope
}

interface ComposeViewModelScope<VM : ViewModel> {
    /**
     * 根据[key]获取[ViewModel]
     */
    @Composable
    fun get(key: String): VM

    /**
     * 根据[key]获取[ViewModel]，如果[ViewModel]不存在，从[factory]创建
     */
    @Composable
    fun get(key: String, factory: (CreationExtras) -> VM): VM

    /**
     * 根据[key]获取[ViewModel]，每次调用此方法都会从[factory]中获取
     */
    @Composable
    fun create(key: String, factory: @Composable (CreateVMParams<VM>) -> VM): VM

    /**
     * 移除[key]对应的[ViewModel]
     */
    @MainThread
    fun remove(key: String)
}

/**
 * 创建[ViewModel]参数
 */
data class CreateVMParams<VM : ViewModel>(
    val vmClass: Class<VM>,
    val viewModelStoreOwner: ViewModelStoreOwner,
    val key: String,
)

@PublishedApi
internal class ViewModelScopeImpl<VM : ViewModel>(
    private val clazz: Class<VM>,
) : ComposeViewModelScope<VM>, ViewModelStoreOwner {

    private var _isDestroyed = false

    private val _viewModelStore = ViewModelStore()
    private val _vmMap: MutableMap<String, ViewModel> = _viewModelStore.vmMap()

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    @Composable
    override fun get(key: String): VM {
        return create(key) { params ->
            viewModel(
                modelClass = params.vmClass,
                viewModelStoreOwner = params.viewModelStoreOwner,
                key = params.key,
            )
        }
    }

    @Composable
    override fun get(key: String, factory: (CreationExtras) -> VM): VM {
        val factoryUpdated by rememberUpdatedState(factory)

        val defaultFactory = remember {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return factoryUpdated(CreationExtras.Empty) as T
                }

                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    @Suppress("UNCHECKED_CAST")
                    return factoryUpdated(extras) as T
                }
            }
        }

        return create(key) { params ->
            viewModel(
                modelClass = params.vmClass,
                viewModelStoreOwner = params.viewModelStoreOwner,
                key = params.key,
                factory = defaultFactory,
            )
        }
    }

    @Composable
    override fun create(key: String, factory: @Composable (CreateVMParams<VM>) -> VM): VM {
        if (_isDestroyed) error("Scope is destroyed.")

        val realOwner = this@ViewModelScopeImpl
        val localOwner = checkNotNull(LocalViewModelStoreOwner.current)

        val viewModelStoreOwner = remember(localOwner) {
            if (localOwner is HasDefaultViewModelProviderFactory) {
                ViewModelStoreOwnerHasDefault(
                    owner = realOwner,
                    factory = localOwner,
                )
            } else {
                realOwner
            }
        }

        val params = CreateVMParams(
            vmClass = clazz,
            viewModelStoreOwner = viewModelStoreOwner,
            key = key,
        )

        return factory(params).also { vm ->
            check(vm === _vmMap[key]) { "ViewModel was not found with key:$key." }
        }
    }

    override fun remove(key: String) {
        _vmMap.vmRemove(key)
    }

    /**
     * 销毁所有[ViewModel]
     */
    @MainThread
    fun destroy() {
        checkMainThread()
        _isDestroyed = true
        viewModelStore.clear()
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

/**
 * 根据[key]移除[ViewModel]
 */
@MainThread
private fun MutableMap<String, ViewModel>.vmRemove(key: String): Boolean {
    checkMainThread()
    val viewModel = remove(key) ?: return false
    ViewModel::class.java.run {
        try {
            getDeclaredMethod("clear")
        } catch (e: Throwable) {
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
private fun ViewModelStore.vmMap(): MutableMap<String, ViewModel> {
    return ViewModelStore::class.java.run {
        try {
            getDeclaredField("map")
        } catch (e: Throwable) {
            null
        } ?: try {
            getDeclaredField("mMap")
        } catch (e: Throwable) {
            null
        } ?: error("map field was not found in ${ViewModelStore::class.java.name}")
    }.let { field ->
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        field.get(this@vmMap) as MutableMap<String, ViewModel>
    }
}

private fun checkMainThread() {
    check(Looper.myLooper() === Looper.getMainLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}