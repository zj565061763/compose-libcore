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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.utils.*
import kotlinx.coroutines.flow.filter

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
    val pagerState = rememberPagerState()

    HorizontalPager(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        pageCount = 20,
        state = pagerState,
    ) { index ->
        PageView(
            index = index,
            viewModel = fDisposableViewModel(),
        )
    }

    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    LaunchedEffect(pagerState, viewModelStoreOwner) {
        snapshotFlow { pagerState.isScrollInProgress }
            .filter { !it }
            .collect {
                logMsg { "size ${viewModelStoreOwner.fKeyedVMSize(PageViewModel::class.java)}" }
            }
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