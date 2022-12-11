package com.sd.lib.compose.libcore.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

object FAppLifecycle {
    val lifecycle get() = ProcessLifecycleOwner.get().lifecycle

    /**
     * 当前是否处于前台
     */
    @JvmStatic
    fun isForeground(): Boolean {
        return lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }
}