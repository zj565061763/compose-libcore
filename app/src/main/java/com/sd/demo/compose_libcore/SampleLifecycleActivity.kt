package com.sd.demo.compose_libcore

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.utils.FAppLifecycle
import com.sd.lib.compose.libcore.utils.FLifecycleOnStart
import com.sd.lib.compose.libcore.utils.FLifecycleOnStop

class SampleLifecycleActivity : BaseActivity() {

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
    FLifecycleOnStart {
        logMsg { "onStart" }
    }

    FLifecycleOnStop {
        logMsg { "onStop" }
    }

    LaunchedEffect(Unit) {
        FAppLifecycle.isForegroundFlow.collect { isForeground ->
            logMsg { "isForeground:${isForeground}" }
        }
    }
}