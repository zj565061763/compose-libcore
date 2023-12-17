package com.sd.lib.compose.libcore

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun TabContainer(
    key: Any,
    modifier: Modifier = Modifier,
    apply: TabContainerScope.() -> Unit,
) {
    val container = remember {
        TabContainerImpl()
    }.apply {
        startConfig()
        apply()
        stopConfig()
    }

    Box(modifier = modifier) {
        container.Content(key)
    }
}

enum class TabDisplay {
    Default,
    New,
}

interface TabContainerScope {
    fun tab(
        key: Any,
        display: TabDisplay = TabDisplay.Default,
        content: @Composable () -> Unit,
    )
}

private class TabContainerImpl : TabContainerScope {
    private var _startConfig = false
    private val _tabHolder: MutableMap<Any, TabInfo> = hashMapOf()
    private val _activeKeyHolder: MutableMap<Any, TabInfo> = mutableStateMapOf()

    fun startConfig() {
        check(!_startConfig) { "Config started." }
        _startConfig = true
        _tabHolder.clear()
    }

    fun stopConfig() {
        check(_startConfig) { "Config not started." }
        _startConfig = false
        _activeKeyHolder.iterator().run {
            while (hasNext()) {
                val item = next()
                if (_tabHolder.containsKey(item.key)) {
                    // key还在
                } else {
                    remove()
                }
            }
        }
    }

    override fun tab(
        key: Any,
        display: TabDisplay,
        content: @Composable () -> Unit,
    ) {
        check(_startConfig) { "Config not started." }
        _tabHolder[key] = TabInfo(
            contentState = mutableStateOf(content),
            displayState = mutableStateOf(display),
        )
    }

    @Composable
    fun Content(key: Any) {
        LaunchedEffect(key) {
            val info = checkNotNull(_tabHolder[key])
            val activeInfo = _activeKeyHolder[key]
            if (activeInfo == null) {
                _activeKeyHolder[key] = info
            } else {
                activeInfo.contentState.value = info.contentState.value
                activeInfo.displayState.value = info.displayState.value
            }
        }

        for (item in _activeKeyHolder) {
            TabContent(
                tabInfo = item.value,
                selected = item.key == key,
            )
        }
    }

    @Composable
    private fun TabContent(tabInfo: TabInfo, selected: Boolean) {
        when (tabInfo.displayState.value) {
            TabDisplay.Default -> {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            if (selected) {
                                this.scaleX = 1f
                            } else {
                                this.scaleX = 0f
                            }
                        }
                ) {
                    tabInfo.contentState.value.invoke()
                }
            }

            TabDisplay.New -> {
                if (selected) {
                    tabInfo.contentState.value.invoke()
                }
            }
        }
    }
}

private class TabInfo(
    val contentState: MutableState<@Composable () -> Unit>,
    val displayState: MutableState<TabDisplay>,
)