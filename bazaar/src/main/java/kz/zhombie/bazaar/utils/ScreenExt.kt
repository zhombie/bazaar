package kz.zhombie.bazaar.utils

import android.content.res.Resources
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


fun Int.dp2Px(): Float = toFloat().dp2Px()

fun Float.dp2Px(): Float = this * Resources.getSystem().displayMetrics.density

fun Int.px2Dp(): Float = toFloat().px2Dp()

fun Float.px2Dp(): Float = this / Resources.getSystem().displayMetrics.density
