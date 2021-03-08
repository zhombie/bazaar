package kz.zhombie.bazaar.core.logging

import android.util.Log

internal object Logger {

    private const val TAG = "Logger"

    fun d(tag: String = TAG, message: String) {
        Log.d(tag, message)
    }

    fun w(tag: String = TAG, message: String) {
        Log.w(tag, message)
    }

}