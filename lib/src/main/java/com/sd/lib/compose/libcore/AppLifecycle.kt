package com.sd.lib.compose.libcore

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object AppLifecycle {
    private val _startedFlow = MutableStateFlow(fAppIsStarted)

    val startedFlow get() = _startedFlow.asStateFlow()

    init {
        fAppLifecycle.addObserver(
            LifecycleEventObserver { _, _ ->
                _startedFlow.value = fAppIsStarted
            }
        )
    }
}

val fAppLifecycle: Lifecycle get() = ProcessLifecycleOwner.get().lifecycle

val fAppIsStarted: Boolean get() = fAppLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
val fAppIsStartedFlow: StateFlow<Boolean> get() = AppLifecycle.startedFlow