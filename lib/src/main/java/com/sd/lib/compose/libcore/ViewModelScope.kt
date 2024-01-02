package com.sd.lib.compose.libcore

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

    /**
     * 移除[key]对应的[ViewModel]
     */
    fun removeViewModel(key: String)
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

    @Suppress("UNCHECKED_CAST")
    @Composable
    override fun getViewModel(key: String, factory: (CreationExtras) -> VM): VM {
        val factoryUpdated by rememberUpdatedState(factory)

        val defaultFactory = remember {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return factoryUpdated(CreationExtras.Empty) as T
                }

                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
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
        }
    }

    override fun removeViewModel(key: String) {
        _vmHolder.vmRemove(key)
    }

    /**
     * 销毁所有[ViewModel]
     */
    fun destroy() {
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