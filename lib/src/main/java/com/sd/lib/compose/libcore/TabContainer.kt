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
        apply()
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
    private val _tabHolder: MutableMap<Any, TabInfo> = hashMapOf()
    private val _activeKeyHolder: MutableMap<Any, String> = mutableStateMapOf()

    override fun tab(
        key: Any,
        display: TabDisplay,
        content: @Composable () -> Unit,
    ) {
        val info = _tabHolder[key]
        if (info == null) {
            _tabHolder[key] = TabInfo(
                contentState = mutableStateOf(content),
                displayState = mutableStateOf(display),
            )
        } else {
            info.contentState.value = content
            info.displayState.value = display
        }
    }

    @Composable
    fun Content(key: Any) {
        LaunchedEffect(key) {
            if (_tabHolder.containsKey(key)) {
                _activeKeyHolder[key] = ""
            }
        }

        for (item in _activeKeyHolder.keys) {
            _tabHolder[item]?.let { info ->
                when (info.displayState.value) {
                    TabDisplay.Default -> {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    if (item == key) {
                                        this.scaleX = 1f
                                    } else {
                                        this.scaleX = 0f
                                    }
                                }
                        ) {
                            info.contentState.value.invoke()
                        }
                    }

                    TabDisplay.New -> {
                        if (item == key) {
                            info.contentState.value.invoke()
                        }
                    }
                }
            }
        }
    }
}

private class TabInfo(
    val contentState: MutableState<@Composable () -> Unit>,
    val displayState: MutableState<TabDisplay>,
)