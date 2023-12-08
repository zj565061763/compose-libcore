package com.sd.demo.compose_libcore

import android.os.Bundle
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
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.utils.*
import java.util.concurrent.atomic.AtomicInteger

class SampleKeyedViewModelActivity : BaseActivity() {

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
    val pagerState = rememberPagerState { 20 }
    HorizontalPager(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        state = pagerState,
    ) { index ->
        PageView(
            index = index,
            viewModel = fDisposableViewModel(),
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
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(index.toString())
        Text(viewModel.toString())
    }
}

private class PageViewModel : ViewModel() {
    init {
        sCounter.incrementAndGet()
        logMsg { "$this init ${sCounter.get()}" }
    }

    override fun onCleared() {
        super.onCleared()
        sCounter.decrementAndGet()
        logMsg { "$this onCleared ${sCounter.get()}" }
    }

    override fun toString(): String {
        return "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    }

    companion object {
        private val sCounter = AtomicInteger(0)
    }
}