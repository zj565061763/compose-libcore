package com.sd.demo.compose_libcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.ext.fKeyedViewModel
import com.sd.lib.compose.libcore.ext.fRemoveKeyedViewModelFartherFromIndex
import kotlinx.coroutines.flow.filter

class SampleKeyedViewModelActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Content()
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun Content() {
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val pagerState = rememberPagerState()

    HorizontalPager(
        state = pagerState,
        count = 20,
        modifier = Modifier.fillMaxSize(),
    ) { index ->

        val viewModel = viewModelStoreOwner.fKeyedViewModel(
            clazz = PageViewModel::class.java,
            key = index.toString(),
            index = index
        )

        PageView(
            index = index,
            viewModel = viewModel
        )
    }

    LaunchedEffect(pagerState, viewModelStoreOwner) {
        snapshotFlow { pagerState.isScrollInProgress }
            .filter { !it }
            .collect {
                viewModelStoreOwner.fRemoveKeyedViewModelFartherFromIndex(
                    clazz = PageViewModel::class.java,
                    index = pagerState.currentPage,
                    maxSize = 3,
                )
            }
    }
}

@Composable
private fun PageView(
    index: Int,
    viewModel: PageViewModel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val text = """
            $index
            $viewModel
        """.trimIndent()

        Text(text)
    }
}

class PageViewModel : ViewModel() {
    init {
        logMsg { "$this init" }
    }

    override fun onCleared() {
        super.onCleared()
        logMsg { "$this onCleared" }
    }

    override fun toString(): String {
        return "${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
    }
}