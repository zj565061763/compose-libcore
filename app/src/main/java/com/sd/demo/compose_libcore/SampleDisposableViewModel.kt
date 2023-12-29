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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.fDisposableVM
import java.util.concurrent.atomic.AtomicInteger

class SampleDisposableViewModel : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content() {
    HorizontalPager(
        state = rememberPagerState { 50 },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) { index ->

        // 实际开发中应该使用ID来当作key，例如实体对象的ID
        val key = index.toString()

        val viewModel = key.fDisposableVM<PageViewModel> {
            viewModel(
                key = it,
                factory = viewModelFactory {
                    initializer { PageViewModel(index) }
                }
            )
        }

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