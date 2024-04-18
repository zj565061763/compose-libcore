package com.sd.lib.compose.libcore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

private val LocalActive = compositionLocalOf<Boolean?> { null }

/**
 * 当前位置是否处于激活状态
 */
@Composable
fun fActive(): Boolean {
    return checkNotNull(LocalActive.current) { "Not in FActive scope." }
}

/**
 * 根据[Lifecycle]决定[content]是否处于激活状态，当状态大于等于[minActiveState]时处于激活状态
 */
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

/**
 * 根据[active]决定[content]是否处于激活状态
 */
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

/**
 * 至少激活过一次，才会显示[content]
 */
@Composable
fun FActiveAtLeastOnce(
    content: @Composable () -> Unit,
) {
    var hasActive by remember { mutableStateOf(false) }

    val fActive = fActive()
    LaunchedEffect(fActive) {
        if (fActive) {
            hasActive = true
        }
    }

    if (hasActive) {
        content()
    }
}

