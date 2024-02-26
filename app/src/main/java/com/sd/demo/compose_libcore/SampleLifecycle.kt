package com.sd.demo.compose_libcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.FAppLifecycle

class SampleLifecycle : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content()
            }
        }
    }
}

@Composable
private fun Content() {
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        logMsg { "ON_START" }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        logMsg { "ON_STOP" }
    }

    LaunchedEffect(Unit) {
        FAppLifecycle.isForegroundFlow.collect { isForeground ->
            logMsg { "isForeground:${isForeground}" }
        }
    }
}