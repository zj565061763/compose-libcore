package com.sd.demo.compose_libcore

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.compose_libcore.ui.theme.AppTheme
import com.sd.lib.compose.libcore.core.FViewModel
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content(
                    onClickSampleKeyedViewModel = {
                        startActivity(Intent(this, SampleKeyedViewModelActivity::class.java))
                    },
                )
            }
        }
    }
}

@Composable
private fun Content(
    onClickSampleKeyedViewModel: () -> Unit,
    vm: MainVM = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Button(
            onClick = onClickSampleKeyedViewModel
        ) {
            Text(text = "Sample KeyedViewModel")
        }
    }

    LaunchedEffect(vm) {
        vm.refreshData()
    }
}

class MainVM : FViewModel<Unit>() {
    override suspend fun handleIntent(intent: Unit) {
    }

    override suspend fun refreshDataImpl() {
        logMsg { "refreshDataImpl" }
    }

    override fun onVMActiveChanged() {
        super.onVMActiveChanged()
        logMsg { "onVMActiveChanged" }
    }

    init {
        viewModelScope.launch {
            isVMActiveFlow.collect {
                logMsg { "isVMActive $it" }
            }
        }
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("libcore-demo", block())
}