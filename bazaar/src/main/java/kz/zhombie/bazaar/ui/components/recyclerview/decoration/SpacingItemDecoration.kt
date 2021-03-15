package kz.zhombie.bazaar.ui.components.recyclerview.decoration

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

internal class SpacingItemDecoration constructor(
    isDecoratorEnabled: Boolean = false,
    private val decoratorWidth: Float = 0F,
    @ColorInt decoratorColor: Int = 0,
    var decoratorLeftOffset: Float = 0F,
    private val isFirstItemDecoratorEnabled: Boolean = false,
    private val isLastItemDecoratorEnabled: Boolean = false,

    private val spacingLeft: Int = 0,
    private val spacingTop: Int = 0,
    private val spacingRight: Int = 0,
    private val spacingBottom: Int = 0
) : RecyclerView.ItemDecoration() {

    companion object {
        private val TAG = SpacingItemDecoration::class.java.simpleName
    }

    private var paint: Paint? = null

    init {
        if (isDecoratorEnabled) {
            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = decoratorColor
                strokeWidth = decoratorWidth
                style = Paint.Style.STROKE
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.set(spacingLeft, spacingTop, spacingRight, spacingBottom)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        paint?.let { paint ->
            if (parent.childCount == 0) return@let
            val start = if (isFirstItemDecoratorEnabled) 0 else 1
            val end = if (isLastItemDecoratorEnabled) parent.childCount else parent.childCount - 1
            for (i in start until end) {
                val child: View = parent.getChildAt(i) ?: return@let
                val layoutParams = child.layoutParams
                if (layoutParams is RecyclerView.LayoutParams) {
                    val left: Float = parent.paddingLeft.toFloat() + decoratorLeftOffset
                    val right: Float = (parent.width - parent.paddingRight).toFloat()
                    val y: Float = (child.top + child.height).toFloat()
                    c.drawLine(left, y, right, y, paint)
                }
            }
        }
    }

}