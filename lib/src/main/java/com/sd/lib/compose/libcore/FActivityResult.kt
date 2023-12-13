package com.sd.lib.compose.libcore

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class FActivityResult(activity: Activity) {
    private val _activity: ComponentActivity
    private val _launcherHolder: MutableMap<String, ActivityResultLauncher<*>> = Collections.synchronizedMap(hashMapOf())

    private val _uuid = UUID.randomUUID().toString()
    private val _nextLocalRequestCode = AtomicInteger()

    fun registerForActivityResult(callback: ActivityResultCallback<ActivityResult>): ActivityResultLauncher<Intent> {
        return register(ActivityResultContracts.StartActivityForResult(), callback)
    }

    fun <I, O> register(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
        if (_activity.isFinishing) {
            return emptyActivityResultLauncher(contract)
        }

        val key = "${_uuid}#${_nextLocalRequestCode.getAndIncrement()}"
        val realCallback = ActivityResultCallback<O> {
            _launcherHolder.remove(key)
            callback.onActivityResult(it)
        }

        return with(_activity.activityResultRegistry) {
            register(key, contract, realCallback).also {
                _launcherHolder[key] = it
            }
        }
    }

    private fun unregister() {
        while (true) {
            if (_launcherHolder.isEmpty()) return
            _launcherHolder.toMap().forEach { item ->
                item.value.unregister()
                _launcherHolder.remove(item.key)
            }
        }
    }

    private val _lifecycleEventObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (Lifecycle.Event.ON_DESTROY == event) {
                source.lifecycle.removeObserver(this)
                unregister()
            }
        }
    }

    init {
        require(activity is ComponentActivity) { "activity should be instance of ${ComponentActivity::class.java}" }
        _activity = activity
        _activity.lifecycle.run {
            if (Lifecycle.State.DESTROYED != currentState) {
                addObserver(_lifecycleEventObserver)
            }
        }
    }
}

private fun <I, O> emptyActivityResultLauncher(contract: ActivityResultContract<I, O>): ActivityResultLauncher<I> {
    return object : ActivityResultLauncher<I>() {
        override fun launch(input: I, options: ActivityOptionsCompat?) {
        }

        override fun unregister() {
        }

        override fun getContract(): ActivityResultContract<I, *> {
            return contract
        }
    }
}