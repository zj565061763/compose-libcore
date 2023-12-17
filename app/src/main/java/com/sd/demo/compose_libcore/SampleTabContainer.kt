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
    val listTab = remember {
        listOf(TabType.Home, TabType.Me)
    }

    var selectedTab by remember { mutableStateOf(TabType.Home) }

    Column(modifier = modifier.fillMaxSize()) {
        TabContainer(
            key = selectedTab,
            modifier = modifier.weight(1f),
        ) {
            tab(TabType.Home) {
                TabHome()
            }

            tab(TabType.Me) {
                TabMe()
            }
        }

        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == TabType.Home,
                onClick = { selectedTab = TabType.Home },
                icon = { Text(text = "Home") },
                modifier = Modifier.weight(1f),
            )
            NavigationBarItem(
                selected = selectedTab == TabType.Me,
                onClick = { selectedTab = TabType.Me },
                icon = { Text(text = "Me") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabHome(
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) {
        logMsg { "TabHome" }
        onDispose {
            logMsg { "TabHome onDispose" }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Home",
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun TabMe(
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) {
        logMsg { "TabMe" }
        onDispose {
            logMsg { "TabMe onDispose" }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Me",
            fontSize = 18.sp,
        )
    }
}

private enum class TabType {
    Home,
    Me,
}