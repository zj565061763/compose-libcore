package com.sd.lib.compose.libcore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FHorizontalPager(
    modifier: Modifier = Modifier,
    state: PagerState,
    // add
    activeTag: (index: Int) -> String = { it.toString() },
    // add
    activeIndex: (index: Int) -> Boolean = { it == state.settledPage },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondBoundsPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: SnapFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
        Orientation.Horizontal
    ),
    pageContent: @Composable FPagerScope.(page: Int) -> Unit
) {
    val scope = remember(state) { FPagerScopeImpl(state) }

    HorizontalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSpacing = pageSpacing,
        verticalAlignment = verticalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = null,
        pageNestedScrollConnection = pageNestedScrollConnection,
    ) { index ->
        scope.index = index
        FActive(
            active = activeIndex(index),
            tag = activeTag(index),
        ) {
            scope.pageContent(index)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FVerticalPager(
    modifier: Modifier = Modifier,
    state: PagerState,
    // add
    activeTag: (index: Int) -> String = { it.toString() },
    // add
    activeIndex: (index: Int) -> Boolean = { it == state.settledPage },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondBoundsPageCount: Int = 0,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: SnapFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
        Orientation.Vertical
    ),
    pageContent: @Composable FPagerScope.(page: Int) -> Unit
) {
    val scope = remember(state) { FPagerScopeImpl(state) }

    VerticalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSpacing = pageSpacing,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = null,
        pageNestedScrollConnection = pageNestedScrollConnection,
    ) { index ->
        FActive(
            active = activeIndex(index),
            tag = activeTag(index)
        ) {
            scope.pageContent(index)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
interface FPagerScope {
    val pagerState: PagerState

    @Composable
    fun LaunchSettledPage(
        vararg keys: Any?,
        block: suspend CoroutineScope.() -> Unit,
    )
}

@OptIn(ExperimentalFoundationApi::class)
private class FPagerScopeImpl(
    override val pagerState: PagerState
) : FPagerScope {

    var index by mutableIntStateOf(0)

    @Composable
    override fun LaunchSettledPage(
        vararg keys: Any?,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        val blockUpdated by rememberUpdatedState(block)
        val settledPage = pagerState.settledPage
        if (index == settledPage) {
            LaunchedEffect(keys = keys) {
                blockUpdated()
            }
        }
    }
}