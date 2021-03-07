package kz.zhombie.bazaar.utils

import android.util.DisplayMetrics
import android.view.View
import androidx.core.view.ViewCompat

// Calculates window height for fullscreen use
internal val View.windowHeight: Int
    get() {
        val displayMetrics = DisplayMetrics()
        ViewCompat.getDisplay(this)?.getRealMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }