package com.sd.demo.compose_libcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.FActive
import com.sd.lib.compose.libcore.FActiveLaunchedEffect
import com.sd.lib.compose.libcore.FActiveLifecycle
import com.sd.lib.compose.libcore.fActive

class SampleActive : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                FActiveLifecycle {
                    Content()
                }
            }
        }
    }
}

@Composable
private fun Content() {

    FActiveLaunchedEffect {
        logMsg { "FLaunchActive active:${it}" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        ActiveBox(text = "1") {
            ActiveBox(text = "2") {
                ActiveBox(text = "3") {
                    ActiveBox(text = "4") {
                        ActiveBox()
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBox(
    modifier: Modifier = Modifier,
    text: String = "",
    content: (@Composable () -> Unit)? = null,
) {
    var checked by remember { mutableStateOf(false) }

    WrapperBox(
        modifier = modifier,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(text = text)
                if (content != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(checked = checked, onCheckedChange = { checked = it })
                }
            }
        },
        content = {
            if (content != null) {
                FActive(active = checked) {
                    content()
                }
            }
        },
    )
}

@Composable
private fun WrapperBox(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .border(2.dp, if (fActive()) Color.Green else Color.Gray)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        title()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Content()
    }
}