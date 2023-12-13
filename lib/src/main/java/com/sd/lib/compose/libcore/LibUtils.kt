package com.sd.lib.compose.libcore

import android.os.Looper

internal fun libCheckMainThread() {
    check(Looper.getMainLooper() === Looper.myLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}