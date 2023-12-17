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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.sd.lib.compose.libcore.TabContainer

class SampleTabContainer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContentView()
        }
    }
}

@Composable
private fun ContentView(
    modifier: Modifier = Modifier,
) {
    val listTab = remember { TabType.entries.toList() }

    var selectedTab by remember { mutableStateOf(TabType.Home) }

    Column(modifier = modifier.fillMaxSize()) {
        TabContainer(
            key = selectedTab,
            modifier = modifier.weight(1f),
        ) {
            listTab.forEach { tabType ->
                tab(tabType) {
                    TabView(tabType)
                }
            }
        }

        NavigationBar {
            listTab.forEach { tabType ->
                NavigationBarItem(
                    selected = selectedTab == tabType,
                    onClick = { selectedTab = tabType },
                    icon = { Text(text = tabType.name) },
                )
            }
        }
    }
}

@Composable
private fun TabView(
    tagType: TabType,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(tagType) {
        logMsg { tagType.name }
        onDispose {
            logMsg { "${tagType.name} onDispose" }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tagType.name,
            fontSize = 18.sp,
        )
    }
}

private enum class TabType {
    Home,
    Live,
    Video,
    Me,
}