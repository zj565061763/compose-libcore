package com.sd.lib.compose.libcore

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
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
    private val _tabHolder: MutableMap<Any, TabInfoState> = hashMapOf()
    private val _activeHolder: MutableMap<Any, TabInfoState> = mutableStateMapOf()

    fun startConfig() {
        check(!_startConfig) { "Config started." }
        _startConfig = true
        _tabHolder.clear()
    }

    fun stopConfig() {
        check(_startConfig) { "Config not started." }
        _startConfig = false
        _activeHolder.iterator().run {
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
        _tabHolder[key] = TabInfoState(
            content = mutableStateOf(content),
            display = mutableStateOf(display),
        )
    }

    @Composable
    fun Content(key: Any) {
        LaunchedEffect(key) {
            val info = checkNotNull(_tabHolder[key])
            val activeInfo = _activeHolder[key]
            if (activeInfo == null) {
                _activeHolder[key] = info
            } else {
                activeInfo.content.value = info.content.value
                activeInfo.display.value = info.display.value
            }
        }

        for (item in _activeHolder) {
            key(item.key) {
                TabContent(
                    tabInfo = item.value,
                    selected = item.key == key,
                )
            }
        }
    }

    @Composable
    private fun TabContent(tabInfo: TabInfoState, selected: Boolean) {
        when (tabInfo.display.value) {
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
                    tabInfo.content.value.invoke()
                }
            }

            TabDisplay.New -> {
                if (selected) {
                    tabInfo.content.value.invoke()
                }
            }
        }
    }
}

private class TabInfoState(
    val content: MutableState<@Composable () -> Unit>,
    val display: MutableState<TabDisplay>,
)