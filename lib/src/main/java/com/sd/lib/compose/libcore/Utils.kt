package com.sd.lib.compose.libcore

import android.os.Looper
import android.util.Log

internal fun libCheckMainThread() {
    check(Looper.getMainLooper() === Looper.myLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}

internal inline fun logMsg(block: () -> Any?) {
    Log.i("compose-libcore", block().toString())
}