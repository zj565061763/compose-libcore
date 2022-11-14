package com.sd.demo.compose_libcore

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sd.lib.compose.dialogview.FDialogConfirm
import com.sd.lib.compose.dialogview.FDialogProgress
import com.sd.lib.compose.libcore.BaseViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseActivity : ComponentActivity() {
    /** 加载框 */
    private val _dialogProgress by lazy {
        FDialogProgress(this).apply {
            this.setOnDismissListener {
                _loadingCount.set(0)
                Log.i(this@BaseActivity.javaClass.simpleName, "loading count:$_loadingCount")
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
                if (viewModel is BaseViewModel<*>) {
                    initViewModel(viewModel)
                }
                return viewModel
            }
        }
    }

    private fun initViewModel(vm: BaseViewModel<*>) {
        initStateLoading(vm)
        initStateToast(vm)
        initStateClosePage(vm)
    }

    protected open fun initStateLoading(vm: BaseViewModel<*>) {
        lifecycleScope.launch {
            vm.baseUiState.stateLoading.collect { state ->
                if (state == null) {
                    if (_loadingCount.decrementAndGet() <= 0) {
                        _dialogProgress.dismiss()
                        _loadingCount.set(0)
                    }
                } else {
                    if (_loadingCount.incrementAndGet() > 0) {
                        _dialogProgress.apply {
                            this.text = state.msg
                            this.setCancelable(state.cancelable)
                        }.show()
                    }
                }
            }
        }
    }

    protected open fun initStateToast(vm: BaseViewModel<*>) {
        lifecycleScope.launch {
            vm.baseUiState.stateToast.collect { state ->
                val length = if (state.longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                Toast.makeText(this@BaseActivity, state.msg, length).show()
            }
        }
    }

    protected open fun initStateClosePage(vm: BaseViewModel<*>) {
        lifecycleScope.launch {
            vm.baseUiState.stateClosePage.collect { state ->
                if (state.confirmMsg.isEmpty()) {
                    finish()
                } else {
                    FDialogConfirm(this@BaseActivity).apply {
                        setCancelable(false)
                        this.content = state.confirmMsg
                        this.onClickConfirm = {
                            dismiss()
                            finish()
                        }
                    }.show()
                }
            }
        }
    }
}