package kz.zhombie.bazaar.core.logging

import android.util.Log
import kz.zhombie.bazaar.Settings

internal object Logger {

    private const val TAG = "Logger"

    fun d(tag: String = TAG, message: String) {
        if (Settings.isLoggingEnabled()) {
            Log.d(tag, message)
        }
    }

    fun w(tag: String = TAG, message: String) {
        if (Settings.isLoggingEnabled()) {
            Log.w(tag, message)
        }
    }

}