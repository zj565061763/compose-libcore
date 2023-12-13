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
fun FLifecycleOnStart(
    vararg keys: Any?,
    callback: () -> Unit,
) {
    FLifecycleTargetEvent(
        targetEvent = Lifecycle.Event.ON_START,
        keys = keys,
        callback = callback,
    )
}

/**
 * 监听[Lifecycle.Event.ON_STOP]
 */
@Composable
fun FLifecycleOnStop(
    vararg keys: Any?,
    callback: () -> Unit,
) {
    FLifecycleTargetEvent(
        targetEvent = Lifecycle.Event.ON_STOP,
        keys = keys,
        callback = callback,
    )
}

@Composable
private fun FLifecycleTargetEvent(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    targetEvent: Lifecycle.Event,
    vararg keys: Any?,
    callback: () -> Unit,
) {
    val callbackUpdate by rememberUpdatedState(callback)
    DisposableEffect(lifecycleOwner, targetEvent, *keys) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == targetEvent) {
                callbackUpdate()
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}