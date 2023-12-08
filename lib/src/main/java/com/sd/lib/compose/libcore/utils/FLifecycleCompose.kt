package com.sd.lib.compose.libcore.utils

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
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    callback: () -> Unit,
) {
    val callbackUpdate by rememberUpdatedState(callback)
    FLifecycleEvent(lifecycleOwner) { event ->
        if (event == Lifecycle.Event.ON_START) {
            callbackUpdate()
        }
    }
}

/**
 * 监听[Lifecycle.Event.ON_STOP]
 */
@Composable
fun FLifecycleOnStop(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    callback: () -> Unit,
) {
    val callbackUpdate by rememberUpdatedState(callback)
    FLifecycleEvent(lifecycleOwner) { event ->
        if (event == Lifecycle.Event.ON_STOP) {
            callbackUpdate()
        }
    }
}

@Composable
private fun FLifecycleEvent(
    lifecycleOwner: LifecycleOwner,
    callback: (Lifecycle.Event) -> Unit,
) {
    val callbackUpdate by rememberUpdatedState(callback)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            callbackUpdate(event)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}