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
    fun get(key: String): VM

    /**
     * 根据[key]获取[ViewModel]，如果[ViewModel]不存在，从[factory]创建
     */
    @Composable
    fun get(key: String, factory: CreationExtras.() -> VM): VM

    /**
     * 根据[key]获取[ViewModel]，每次调用此方法都会从[factory]中获取
     */
    @Composable
    fun create(key: String, factory: @Composable CreateVMParams<VM>.() -> VM): VM

    /**
     * 移除[key]对应的[ViewModel]
     */
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
        set(value) {
            require(value) { "Require true value." }
            field = value
        }

    override val viewModelStore: ViewModelStore = ViewModelStore()
    private val _vmHolder: MutableMap<String, ViewModel> = viewModelStore.vmHolder()

    @Composable
    override fun get(key: String): VM {
        return create(key) {
            viewModel(
                modelClass = this.vmClass,
                viewModelStoreOwner = this.viewModelStoreOwner,
                key = this.key,
            )
        }
    }

    @Composable
    override fun get(key: String, factory: CreationExtras.() -> VM): VM {
        val factoryUpdated by rememberUpdatedState(factory)

        val defaultFactory = remember {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return with(CreationExtras.Empty) {
                        @Suppress("UNCHECKED_CAST")
                        factoryUpdated() as T
                    }
                }

                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return with(extras) {
                        @Suppress("UNCHECKED_CAST")
                        factoryUpdated() as T
                    }
                }
            }
        }

        return create(key) {
            viewModel(
                modelClass = this.vmClass,
                viewModelStoreOwner = this.viewModelStoreOwner,
                key = this.key,
                factory = defaultFactory,
            )
        }
    }

    @Composable
    override fun create(key: String, factory: @Composable CreateVMParams<VM>.() -> VM): VM {
        if (_isDestroyed) error("Scope is destroyed.")

        @Suppress("NAME_SHADOWING")
        val key = packKey(key)
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
            vmClass = clazz,
            viewModelStoreOwner = viewModelStoreOwner,
            key = key,
        )

        return params.factory().also { vm ->
            check(vm === _vmHolder[key]) { "ViewModel was not found with key:$key." }
        }
    }

    override fun remove(key: String) {
        _vmHolder.vmRemove(packKey(key))
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

private fun packKey(key: String): String {
    return "com.sd.keyedViewModel:${key}"
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