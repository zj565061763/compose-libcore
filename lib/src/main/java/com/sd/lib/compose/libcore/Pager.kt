package com.sd.lib.compose.libcore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppHorizontalPager(
    modifier: Modifier = Modifier,
    state: PagerState,
    // add
    selectedPage: Int? = null,
    // add
    onPageChange: ((Int) -> Unit)? = null,
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
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    // 滚动到选中到位置
    if (selectedPage != null) {
        state.fScrollToPage(page = selectedPage)
    }

    // 监听位置变化
    if (onPageChange != null) {
        state.fCurrentPage(onChange = onPageChange)
    }

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
        FActive(
            active = activeIndex(index),
            tag = activeTag(index),
        ) {
            pageContent(index)
        }
    }
}