package kz.zhombie.bazaar.ui.components.recyclerview.animator

import android.view.View

internal fun View.clear() {
    apply {
        alpha = 1f
        scaleY = 1f
        scaleX = 1f
        translationY = 0f
        translationX = 0f
        rotation = 0f
        rotationY = 0f
        rotationX = 0f
        pivotY = measuredHeight / 2f
        pivotX = measuredWidth / 2f
        animate().setInterpolator(null).startDelay = 0
    }
}