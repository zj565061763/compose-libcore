package com.sd.lib.compose.libcore.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FAppLifecycle {

    @JvmStatic
    val lifecycle get() = ProcessLifecycleOwner.get().lifecycle

    private val _mutableForegroundFlow = MutableStateFlow(isForeground())

    /**
     * App是否出于前台
     */
    val isForegroundFlow: StateFlow<Boolean> = _mutableForegroundFlow.asStateFlow()

    /**
     * 当前是否处于前台
     */
    @JvmStatic
    fun isForeground(): Boolean {
        return lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    init {
        val observer = LifecycleEventObserver { source, event ->
            _mutableForegroundFlow.value = isForeground()
        }
        lifecycle.addObserver(observer)
    }
}