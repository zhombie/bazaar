package kz.zhombie.bazaar.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

internal class SpacingItemDecoration constructor(
    private val spacingLeft: Int = 0,
    private val spacingTop: Int = 0,
    private val spacingRight: Int = 0,
    private val spacingBottom: Int = 0
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        when (parent.layoutManager?.getPosition(view)) {
            0 -> {
                // Ignored
            }
            else -> {
                outRect.set(spacingLeft, spacingTop, spacingRight, spacingBottom)
            }
        }
    }

}