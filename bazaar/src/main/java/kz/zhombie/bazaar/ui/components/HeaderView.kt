package kz.zhombie.bazaar.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputFilter
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
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
    private val titleButton: LinearLayout
    private lateinit var titleTextView: MaterialTextView
    private lateinit var subtitleTextView: MaterialTextView
    private lateinit var iconView: ShapeableImageView
    private val closeButton: MaterialButton

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        scrollIndicatorView = createScrollIndicatorView()
        addView(scrollIndicatorView)

        titleButton = createTitleButton()
        addView(titleButton)

        closeButton = createCloseButton()
        addView(closeButton)
    }

    private object Id {
        const val SCROLL_INDICATOR_VIEW = 1
        const val TITLE_BUTTON = 2
        const val TITLE_TEXT_VIEW = 3
        const val SUBTITLE_TEXT_VIEW = 4
        const val ICON_VIEW = 5
        const val CLOSE_BUTTON = 6
    }

    private fun createScrollIndicatorView(): ShapeableImageView {
        val imageView = ShapeableImageView(context)
//        imageView.id = ViewCompat.generateViewId()
        imageView.id = Id.SCROLL_INDICATOR_VIEW
        val layoutParams = LayoutParams(35F.dp2Px().roundToInt(), 3F.dp2Px().roundToInt())
        layoutParams.setMargins(0, 7.5F.dp2Px().roundToInt(), 0, 0)
        layoutParams.leftToLeft = ConstraintSet.PARENT_ID
        layoutParams.topToTop = ConstraintSet.PARENT_ID
        layoutParams.rightToRight = ConstraintSet.PARENT_ID
        layoutParams.bottomToTop = Id.TITLE_BUTTON
        imageView.layoutParams = layoutParams
        imageView.setImageResource(R.drawable.ic_scroll_indicator)
        return imageView
    }

    private fun createTitleButton(): LinearLayout {
        val titleButton = LinearLayout(context)
//        titleButton.id = ViewCompat.generateViewId()
        titleButton.id = Id.TITLE_BUTTON
        val titleButtonsLayoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT)
        titleButtonsLayoutParams.setMargins(10F.dp2Px().roundToInt(), 5F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt(), 0)
        titleButtonsLayoutParams.leftToLeft = ConstraintSet.PARENT_ID
        titleButtonsLayoutParams.topToTop = Id.SCROLL_INDICATOR_VIEW
        titleButtonsLayoutParams.rightToLeft = Id.CLOSE_BUTTON
        titleButton.layoutParams = titleButtonsLayoutParams
        titleButton.setBackgroundResource(R.drawable.ripple_rounded)
        titleButton.isClickable = true
        titleButton.isFocusable = true
        titleButton.gravity = Gravity.CENTER_VERTICAL
        titleButton.orientation = LinearLayout.HORIZONTAL
        titleButton.setPadding(15F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt(), 15F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt())

        val titleViewContainer = LinearLayout(context)
        titleViewContainer.id = ViewCompat.generateViewId()
        val titleViewContainerLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        titleViewContainer.layoutParams = titleViewContainerLayoutParams
        titleViewContainer.orientation = LinearLayout.VERTICAL

        titleTextView = MaterialTextView(context)
//        titleTextView.id = ViewCompat.generateViewId()
        titleTextView.id = Id.TITLE_TEXT_VIEW
        val titleViewLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        titleTextView.layoutParams = titleViewLayoutParams
        titleTextView.letterSpacing = 0F
        titleTextView.maxLines = 1
        titleTextView.filters = arrayOf(InputFilter.LengthFilter(18))
        titleTextView.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        titleTextView.setSingleLine()
        titleTextView.isAllCaps = false
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17F)
        titleTextView.setTextColor(Color.parseColor("#333333"))

        subtitleTextView = MaterialTextView(context)
//        subtitleTextView.id = ViewCompat.generateViewId()
        subtitleTextView.id = Id.SUBTITLE_TEXT_VIEW
        val subtitleViewLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        subtitleViewLayoutParams.setMargins(0, 1F.dp2Px().roundToInt(), 0, 0)
        subtitleTextView.layoutParams = subtitleViewLayoutParams
        subtitleTextView.letterSpacing = 0F
        subtitleTextView.maxLines = 1
        subtitleTextView.filters = arrayOf(InputFilter.LengthFilter(18))
        subtitleTextView.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        subtitleTextView.setSingleLine()
        subtitleTextView.isAllCaps = false
        subtitleTextView.includeFontPadding = false
        subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11F)
        subtitleTextView.text = "Текущий альбом"
        subtitleTextView.setTextColor(Color.parseColor("#8290A0"))

        titleViewContainer.addView(titleTextView)
        titleViewContainer.addView(subtitleTextView)

        iconView = ShapeableImageView(context)
//        iconView.id = ViewCompat.generateViewId()
        iconView.id = Id.ICON_VIEW
        val iconViewLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        iconViewLayoutParams.setMargins(15F.dp2Px().roundToInt(), 0, 15F.dp2Px().roundToInt(), 0)
        iconView.layoutParams = iconViewLayoutParams
        iconView.setImageResource(R.drawable.ic_dropdown_down)

        titleButton.addView(titleViewContainer)
        titleButton.addView(iconView)

        return titleButton
    }

    private fun createCloseButton(): MaterialButton {
        val closeButton = MaterialButton(context, null, R.style.Widget_MaterialComponents_Button_TextButton_Icon)
//        closeButton.id = ViewCompat.generateViewId()
        closeButton.id = Id.CLOSE_BUTTON
        val closeButtonLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        closeButtonLayoutParams.setMargins(5F.dp2Px().roundToInt(), 0, 5F.dp2Px().roundToInt(), 0)
        closeButtonLayoutParams.leftToRight = Id.TITLE_BUTTON
        closeButtonLayoutParams.topToTop = Id.TITLE_BUTTON
        closeButtonLayoutParams.rightToRight = ConstraintSet.PARENT_ID
        closeButtonLayoutParams.bottomToBottom = Id.TITLE_BUTTON
        closeButton.layoutParams = closeButtonLayoutParams
        closeButton.background = null
        closeButton.insetTop = 0
        closeButton.insetBottom = 0
        closeButton.minWidth = 50F.dp2Px().roundToInt()
        closeButton.minHeight = 50F.dp2Px().roundToInt()
        closeButton.setPadding(10F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt())
        closeButton.setIconResource(R.drawable.ic_close)
        closeButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
        closeButton.iconPadding = 0
        closeButton.iconTint = null
//        closeButton.setRippleColorResource(R.attr.colorControlHighlight)
        return closeButton
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun toggleIcon(isUp: Boolean) {
        if (isUp) {
            iconView.setImageResource(R.drawable.ic_dropdown_up)
        } else {
            iconView.setImageResource(R.drawable.ic_dropdown_down)
        }
    }

    fun setOnTitleButtonClickListener(callback: () -> Unit) {
        titleButton.setOnClickListener { callback() }
    }

    fun setOnCloseButtonClickListener(callback: () -> Unit) {
        closeButton.setOnClickListener { callback() }
    }
}