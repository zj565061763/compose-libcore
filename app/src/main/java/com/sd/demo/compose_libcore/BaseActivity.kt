package com.sd.demo.compose_libcore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sd.lib.compose.dialogview.FDialogProgress
import com.sd.lib.compose.libcore.core.FViewModel
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseActivity : ComponentActivity() {
    /** 加载框 */
    private val _dialogProgress by lazy {
        FDialogProgress(this).apply {
            this.setOnDismissListener {
                _loadingCount.set(0)
            }
        }
    }

    /** 触发loading的次数 */
    private val _loadingCount = AtomicInteger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        val superFactory = super.getDefaultViewModelProviderFactory()
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val viewModel = superFactory.create(modelClass)
                if (viewModel is FViewModel<*>) {
                    initViewModel(viewModel)
                }
                return viewModel
            }
        }
    }

    protected open fun initViewModel(vm: FViewModel<*>) {

    }
}