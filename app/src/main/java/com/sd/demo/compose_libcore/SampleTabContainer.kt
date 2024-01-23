package com.sd.demo.compose_libcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.TabContainer

class SampleTabContainer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                ContentView()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

private val Tabs = TabType.entries.toList()

private enum class TabType {
    Home,
    Me,
}

@Composable
private fun ContentView(
    modifier: Modifier = Modifier,
) {
    /** 当前选中的Tab */
    var selectedTab by remember { mutableStateOf(TabType.Home) }

    Column(modifier = modifier.fillMaxSize()) {
        TabContainer(
            selectedKey = selectedTab,
            modifier = Modifier.weight(1f),
        ) {
            tab(TabType.Home) {
                TabHome()
            }
            tab(TabType.Me) {
                TabMe()
            }
        }
        NavigationBar {
            for (tab in Tabs) {
                key(tab) {
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(text = tab.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabHome() {
    DisposableEffect(Unit) {
        logMsg { "TabHome" }
        onDispose {
            logMsg { "TabHome onDispose" }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Home",
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun TabMe() {
    DisposableEffect(Unit) {
        logMsg { "TabMe" }
        onDispose {
            logMsg { "TabMe onDispose" }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Me",
            fontSize = 18.sp,
        )
    }
}

