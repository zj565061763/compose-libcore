package com.sd.demo.compose_libcore

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.vm.FViewModel
import com.sd.lib.compose.libcore.vm.extActive

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content(
                    listActivity = remember {
                        listOf(
                            SampleLifecycleActivity::class.java,
                            SampleKeyedViewModelActivity::class.java,
                        )
                    },
                    onClickActivity = {
                        startActivity(Intent(this, it))
                    },
                )
            }
        }
    }
}

@Composable
private fun Content(
    vm: MainVM = viewModel(),
    listActivity: List<Class<out Activity>>,
    onClickActivity: (Class<out Activity>) -> Unit,
) {
    val onClickActivityUpdated by rememberUpdatedState(onClickActivity)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(
            listActivity,
            key = { it },
        ) { item ->
            Button(
                onClick = { onClickActivityUpdated(item) }
            ) {
                Text(text = item.simpleName)
            }
        }
    }
}

class MainVM : FViewModel<Unit>() {
    override suspend fun handleIntent(intent: Unit) {
    }

    override suspend fun refreshDataImpl() {
        logMsg { "MainVM refreshDataImpl" }
    }

    init {
        extActive().onActive {
            logMsg { "MainVM onActive" }
            refreshData()
        }

        extActive().onInactive {
            logMsg { "MainVM onInactive" }
        }
    }
}

inline fun logMsg(block: () -> Any?) {
    Log.i("libcore-demo", block().toString())
}