package kz.zhombie.bazaar.ui.components.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.utils.dp2Px
import kotlin.math.roundToInt

internal class HeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val scrollIndicatorView: ShapeableImageView
    private val titleButton: HeaderViewTitleButton
    private val closeButton: MaterialButton

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        scrollIndicatorView = createScrollIndicatorView()
        addView(scrollIndicatorView)

        titleButton = createTitleButton()
        addView(titleButton)

        closeButton = createCloseButton()
        addView(closeButton)

        val divider = createDivider()
        addView(divider)
    }

    private object ViewId {
        val SCROLL_INDICATOR_VIEW = hashCode() + 1
        val TITLE_BUTTON = hashCode() + 2
        val CLOSE_BUTTON = hashCode() + 3
        val DIVIDER = hashCode() + 4
    }

    private fun createScrollIndicatorView(): ShapeableImageView {
        val imageView = ShapeableImageView(context)
        imageView.id = ViewId.SCROLL_INDICATOR_VIEW
        val layoutParams = LayoutParams(35F.dp2Px().roundToInt(), 3F.dp2Px().roundToInt())
        layoutParams.setMargins(0, 7.5F.dp2Px().roundToInt(), 0, 0)
        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
        layoutParams.topToTop = ConstraintSet.PARENT_ID
        layoutParams.rightToRight = ConstraintSet.PARENT_ID
        layoutParams.bottomToTop = ViewId.TITLE_BUTTON
        imageView.layoutParams = layoutParams
        imageView.setImageResource(R.drawable.ic_scroll_indicator)
        return imageView
    }

    private fun createTitleButton(): HeaderViewTitleButton {
        val titleButton = HeaderViewTitleButton(context)
        titleButton.id = ViewId.TITLE_BUTTON
        val titleButtonsLayoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT)
        titleButtonsLayoutParams.setMargins(10F.dp2Px().roundToInt(), 5F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt(), 0)
        titleButtonsLayoutParams.leftToLeft = ConstraintSet.PARENT_ID
        titleButtonsLayoutParams.topToTop = ViewId.SCROLL_INDICATOR_VIEW
        titleButtonsLayoutParams.rightToLeft = ViewId.CLOSE_BUTTON
        titleButton.layoutParams = titleButtonsLayoutParams
        return titleButton
    }

    private fun createCloseButton(): MaterialButton {
        val closeButton = MaterialButton(context, null, R.style.Widget_MaterialComponents_Button_TextButton_Icon)
        closeButton.id = ViewId.CLOSE_BUTTON
        val closeButtonLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        closeButtonLayoutParams.setMargins(5F.dp2Px().roundToInt(), 0, 5F.dp2Px().roundToInt(), 0)
        closeButtonLayoutParams.leftToRight = ViewId.TITLE_BUTTON
        closeButtonLayoutParams.topToTop = ViewId.TITLE_BUTTON
        closeButtonLayoutParams.rightToRight = ConstraintSet.PARENT_ID
        closeButtonLayoutParams.bottomToBottom = ViewId.TITLE_BUTTON
        closeButton.layoutParams = closeButtonLayoutParams
        closeButton.backgroundTintList = ContextCompat.getColorStateList(context, R.color.bg_button_gray)
        closeButton.insetTop = 0
        closeButton.insetBottom = 0
        closeButton.minWidth = 50F.dp2Px().roundToInt()
        closeButton.minHeight = 50F.dp2Px().roundToInt()
        closeButton.setPadding(10F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt())
        closeButton.setIconResource(R.drawable.ic_close)
        closeButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
        closeButton.iconPadding = 0
        closeButton.iconTint = null
        closeButton.shapeAppearanceModel = ShapeAppearanceModel
            .builder(context, R.style.CircularShapeAppearance, 0)
            .build()
        return closeButton
    }

    private fun createDivider(): View {
        val view = View(context)
        view.id = ViewId.DIVIDER
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0.75F.dp2Px().roundToInt())
        layoutParams.setMargins(0, 5F.dp2Px().roundToInt(), 0, 0)
        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
        layoutParams.topToBottom = ViewId.TITLE_BUTTON
        layoutParams.rightToRight = ConstraintSet.PARENT_ID
        view.layoutParams = layoutParams
        view.setBackgroundColor(Color.parseColor("#B6BDC6"))
        return view
    }

    fun setTitle(title: String) {
        titleButton.setTitle(title)
    }

    fun toggleIcon(isUp: Boolean) {
        titleButton.toggleIcon(isUp)
    }

    fun setOnTitleButtonClickListener(callback: () -> Unit) {
        titleButton.setOnClickListener { callback() }
    }

    fun setOnCloseButtonClickListener(callback: () -> Unit) {
        closeButton.setOnClickListener { callback() }
    }
}