package kz.zhombie.bazaar.ui.components.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.ShapeAppearanceModel
import kz.zhombie.bazaar.R

class CheckBoxButton @JvmOverloads internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        backgroundTintList = ContextCompat.getColorStateList(context, R.color.bg_button_gray)

        gravity = Gravity.CENTER

        includeFontPadding = false

        insetTop = 0
        insetBottom = 0

        text = null

        setCheckedDrawable()
        iconGravity = ICON_GRAVITY_TEXT_END
        iconPadding = 0
        iconTint = null

        rippleColor = ContextCompat.getColorStateList(context, R.color.ripple)

        shapeAppearanceModel = ShapeAppearanceModel
            .builder(context, R.style.CircularShapeAppearance, 0)
            .build()
    }

    fun setCheckedDrawable() {
        setIconResource(R.drawable.ic_checked)
    }

    fun setUncheckedDrawable() {
        setIconResource(R.drawable.ic_unchecked)
    }

    fun setShownState() {
        scaleX = 1.0F
        scaleY = 1.0F
    }

    fun setHiddenState() {
        scaleX = 0.0F
        scaleY = 0.0F
    }

    fun show(animate: Boolean = false) {
        if (animate) {
            animate()
                .setDuration(100L)
                .scaleX(1.0F)
                .scaleY(1.0F)
                .withStartAction {
                    visibility = View.VISIBLE
                }
                .start()
        } else {
            visibility = View.VISIBLE
        }
    }

    fun hide(animate: Boolean = false) {
        if (animate) {
            animate()
                .setDuration(100L)
                .scaleX(0.0F)
                .scaleY(0.0F)
                .withEndAction {
                    visibility = View.INVISIBLE
                }
                .start()
        } else {
            visibility = View.INVISIBLE
        }
    }

}