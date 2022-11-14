package com.sd.demo.compose_libcore

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sd.demo.compose_libcore.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Content()
                }
            }
        }
    }
}

@Composable
fun Content() {
    Text(text = "Hello World!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        Content()
    }
}

fun logMsg(block: () -> String) {
    Log.i("libcore-demo", block())
}