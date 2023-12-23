package com.sd.lib.compose.libcore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 监听[Lifecycle.Event.ON_START]
 */
@Composable
fun FLifecycleOnStart(callback: () -> Unit) {
    FLifecycleTargetEvent(
        targetEvent = Lifecycle.Event.ON_START,
        callback = callback,
    )
}

/**
 * 监听[Lifecycle.Event.ON_STOP]
 */
@Composable
fun FLifecycleOnStop(callback: () -> Unit) {
    FLifecycleTargetEvent(
        targetEvent = Lifecycle.Event.ON_STOP,
        callback = callback,
    )
}

@Composable
private fun FLifecycleTargetEvent(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    targetEvent: Lifecycle.Event,
    callback: () -> Unit,
) {
    val callbackUpdated by rememberUpdatedState(callback)
    DisposableEffect(lifecycleOwner, targetEvent) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == targetEvent) {
                callbackUpdated()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}