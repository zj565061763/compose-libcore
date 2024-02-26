package com.sd.lib.compose.libcore

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FAppLifecycle {

    @JvmStatic
    val lifecycle get() = ProcessLifecycleOwner.get().lifecycle

    private val _isForegroundFlow = MutableStateFlow(isForeground())

    /** App是否处于前台 */
    val isForegroundFlow: StateFlow<Boolean> = _isForegroundFlow.asStateFlow()

    /**
     * App是否处于前台
     */
    @JvmStatic
    fun isForeground(): Boolean = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    init {
        val observer = LifecycleEventObserver { _, _ ->
            _isForegroundFlow.value = isForeground()
        }
        lifecycle.addObserver(observer)
    }
}