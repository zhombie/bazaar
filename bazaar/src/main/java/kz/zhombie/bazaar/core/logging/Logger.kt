package kz.zhombie.bazaar.core.logging

import android.util.Log
import kz.zhombie.bazaar.Bazaar

internal object Logger {
    fun debug(tag: String = Bazaar.TAG, message: String) {
        if (Bazaar.isLoggingEnabled()) {
            Log.d(tag, message)
        }
    }

    fun warn(tag: String = Bazaar.TAG, message: String) {
        if (Bazaar.isLoggingEnabled()) {
            Log.w(tag, message)
        }
    }
}