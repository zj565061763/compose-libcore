package com.sd.lib.compose.libcore

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun TabContainer(
    modifier: Modifier = Modifier,
    selectedKey: Any,
    apply: TabContainerScope.() -> Unit,
) {
    val container = remember {
        TabContainerImpl()
    }.apply {
        startConfig()
        apply()
    }

    Box(modifier = modifier) {
        container.Content(selectedKey)
    }
}

typealias TabDisplay = @Composable (content: @Composable () -> Unit, selected: Boolean) -> Unit

interface TabContainerScope {
    fun tab(
        key: Any,
        display: TabDisplay? = null,
        content: @Composable () -> Unit,
    )
}

private class TabContainerImpl : TabContainerScope {
    private val _store: MutableMap<Any, TabInfo> = hashMapOf()
    private val _activeTabs: MutableMap<Any, TabState> = mutableStateMapOf()

    private var _config = false
    private val _keys: MutableSet<Any> = hashSetOf()

    fun startConfig() {
        _config = true
        _keys.clear()
        _keys.addAll(_store.keys)
    }

    private fun checkConfig() {
        if (!_config) return
        _config = false

        _keys.forEach { key ->
            _store.remove(key)
            _activeTabs.remove(key)
        }

        _activeTabs.forEach { active ->
            val info = checkNotNull(_store[active.key])
            active.value.apply {
                this.display.value = info.display
                this.content.value = info.content
            }
        }
    }

    override fun tab(
        key: Any,
        display: TabDisplay?,
        content: @Composable () -> Unit,
    ) {
        check(_config) { "Config not started." }
        _keys.remove(key)

        val info = _store[key]
        if (info == null) {
            _store[key] = TabInfo(display = display, content = content)
        } else {
            info.display = display
            info.content = content
        }
    }

    @Composable
    fun Content(selectedKey: Any) {
        SideEffect {
            checkConfig()
        }

        LaunchedEffect(selectedKey) {
            if (!_activeTabs.containsKey(selectedKey)) {
                val info = checkNotNull(_store[selectedKey]) { "Key $selectedKey was not found." }
                _activeTabs[selectedKey] = TabState(
                    display = mutableStateOf(info.display),
                    content = mutableStateOf(info.content),
                )
            }
        }

        for ((key, state) in _activeTabs) {
            val selected = key == selectedKey
            val display = state.display.value ?: DefaultDisplay
            key(key) {
                display(state.content.value, selected)
            }
        }
    }
}

private val DefaultDisplay: TabDisplay = { content: @Composable () -> Unit, selected: Boolean ->
    Box(
        modifier = Modifier.graphicsLayer {
            if (selected) {
                this.scaleX = 1f
            } else {
                this.scaleX = 0f
            }
        }
    ) {
        content()
    }
}

private class TabInfo(
    var display: TabDisplay?,
    var content: @Composable () -> Unit,
)

private class TabState(
    val display: MutableState<TabDisplay?>,
    val content: MutableState<@Composable () -> Unit>,
)