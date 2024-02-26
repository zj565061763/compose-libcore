package com.sd.lib.compose.libcore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope

private val LocalActive = compositionLocalOf<Boolean?> { null }

@Composable
fun FActiveLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    content: @Composable () -> Unit,
) {
    var lifecycleActive by remember(lifecycleOwner, minActiveState) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(minActiveState))
    }

    DisposableEffect(lifecycleOwner, minActiveState) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleActive = lifecycleOwner.lifecycle.currentState.isAtLeast(minActiveState)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    FActive(
        active = lifecycleActive,
        content = content,
    )
}

@Composable
fun FActive(
    active: Boolean,
    content: @Composable () -> Unit,
) {
    val localActive = LocalActive.current
    val finalActive = if (localActive == null) active else active && localActive

    CompositionLocalProvider(LocalActive provides finalActive) {
        content()
    }
}

@Composable
fun FActiveLaunchedEffect(
    vararg keys: Any?,
    block: suspend CoroutineScope.(active: Boolean) -> Unit,
) {
    val blockUpdated by rememberUpdatedState(block)
    val active = fActive()
    LaunchedEffect(active, *keys) {
        blockUpdated(active)
    }
}

@Composable
fun fActive(): Boolean {
    return checkNotNull(LocalActive.current) { "Not in FActive scope." }
}