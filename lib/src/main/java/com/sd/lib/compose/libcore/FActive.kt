package com.sd.lib.compose.libcore

import android.util.Log
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
private val LocalTag = compositionLocalOf<String?> { null }

@Composable
fun FActiveLifecycle(
    tag: String = "Lifecycle",
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    content: @Composable () -> Unit,
) {

    var lifecycleActive by remember(lifecycleOwner, minActiveState) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(minActiveState))
    }

    DisposableEffect(lifecycleOwner, minActiveState) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleActive = lifecycle.currentState.isAtLeast(minActiveState)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    FActive(
        active = lifecycleActive,
        tag = tag,
        content = content,
    )
}

@Composable
fun FActive(
    active: Boolean,
    tag: String = "",
    content: @Composable () -> Unit,
) {
    val localTag = LocalTag.current

    val finalTag = remember(localTag, tag) {
        if (localTag == null) {
            "(${tag})"
        } else {
            "${localTag}_(${tag})"
        }
    }

    val localActive = LocalActive.current
    val finalActive = if (localActive == null) active else active && localActive

    LaunchedEffect(finalActive) {
        logMsg { "$finalTag $finalActive" }
    }

    CompositionLocalProvider(
        LocalActive provides finalActive,
        LocalTag provides finalTag,
    ) {
        content()
    }
}

@Composable
fun fActive(): Boolean {
    return checkNotNull(LocalActive.current) { "Not in FActive scope." }
}

private inline fun logMsg(block: () -> String) {
    Log.i("FActive", block())
}