package com.sd.demo.compose_libcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.ComposeViewModelScope
import com.sd.lib.compose.libcore.fRememberVMScope
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

class SampleViewModelScope : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                var showContent by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(15_000)
                    showContent = false
                    logMsg { "Content is removed." }
                }

                if (showContent) {
                    Content()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content() {
    /**
     * 创建[ComposeViewModelScope]，根据参数[maxSize]指定最多保存几个[ViewModel]，
     * 如果超过指定的个数，根据LRU算法移除不常用的[ViewModel]，
     * 如果调用此方法的地方在组合中被移除，[ComposeViewModelScope]会清空所有保存的[ViewModel]
     */
    val vmScope = fRememberVMScope<PageViewModel>(maxSize = 5)

    HorizontalPager(
        state = rememberPagerState { 50 },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) { index ->

        // 实际开发中应该使用ID来当作key，例如实体对象的ID
        val key = index.toString()

        val viewModel = vmScope.getViewModel(
            // 根据[key]获取[ViewModel]
            key = key,

            // 如果[ViewModel]不存在，从[factory]创建
            factory = { PageViewModel(index) }
        )

        PageView(
            index = index,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun PageView(
    index: Int,
    viewModel: PageViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(index.toString())
        Text(viewModel.toString())
    }
}

internal class PageViewModel(
    private val index: Int,
) : ViewModel() {

    init {
        sCounter.incrementAndGet()
        logMsg { "$this init" }
    }

    override fun onCleared() {
        super.onCleared()
        sCounter.decrementAndGet()
        logMsg { "$this onCleared size:${sCounter.get()}" }
    }

    override fun toString(): String {
        return "($index) ${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    }

    companion object {
        private val sCounter = AtomicInteger(0)
    }
}