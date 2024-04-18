package com.sd.lib.compose.libcore

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object AppLifecycle {
    private val _startedFlow = MutableStateFlow(fAppIsStarted)

    val startedFlow: StateFlow<Boolean>
        get() = _startedFlow.asStateFlow()

    init {
        fAppLifecycle.addObserver(
            LifecycleEventObserver { _, _ ->
                _startedFlow.value = fAppIsStarted
            }
        )
    }
}

/** App生命周期 */
val fAppLifecycle: Lifecycle
    get() = ProcessLifecycleOwner.get().lifecycle

/** App生命周期绑定的[CoroutineScope] */
val fAppLifecycleScope: CoroutineScope
    get() = ProcessLifecycleOwner.get().lifecycleScope

/** App生命周期是否至少处于[Lifecycle.State.STARTED]状态 */
val fAppIsStarted: Boolean
    get() = fAppLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

/** 监听App生命周期是否至少处于[Lifecycle.State.STARTED]状态 */
val fAppIsStartedFlow: StateFlow<Boolean>
    get() = AppLifecycle.startedFlow