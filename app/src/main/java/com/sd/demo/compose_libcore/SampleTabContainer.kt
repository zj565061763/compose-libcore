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
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.TabContainer

class SampleTabContainer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

private enum class TabType {
    Home,
    Me,
}

@Composable
private fun Content() {
    /** 当前选中的Tab */
    var selectedTab by remember { mutableStateOf(TabType.Home) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabContainer(
            selectedKey = selectedTab,
            modifier = Modifier.weight(1f),
        ) {
            tab(TabType.Home) {
                TabContent(TabType.Home)
            }
            tab(TabType.Me) {
                TabContent(TabType.Me)
            }
        }
        BottomNavigation(selectedTab) { selectedTab = it }
    }
}

@Composable
private fun TabContent(
    tabType: TabType,
    modifier: Modifier = Modifier,
) {
    // 打印生命周期日志
    DisposableEffect(tabType) {
        logMsg { "tab:${tabType.name}" }
        onDispose { logMsg { "tab:${tabType.name} onDispose" } }
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = tabType.name)
    }
}

@Composable
private fun BottomNavigation(
    selectedTab: TabType,
    onClickTab: (TabType) -> Unit,
) {
    val tabs = remember { TabType.entries.toList() }
    NavigationBar {
        for (tab in tabs) {
            key(tab) {
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onClickTab(tab) },
                    icon = { Text(text = tab.name) },
                )
            }
        }
    }
}