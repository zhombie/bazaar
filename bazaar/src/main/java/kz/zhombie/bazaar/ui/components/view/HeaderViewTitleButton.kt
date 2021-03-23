package kz.zhombie.bazaar.ui.components.view

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.utils.dp2Px
import kotlin.math.roundToInt

class HeaderViewTitleButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val titleTextView: MaterialTextView
    private val subtitleTextView: MaterialTextView

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setBackgroundResource(R.drawable.bazaar_ripple_rounded)
        isClickable = true
        isFocusable = true
        orientation = VERTICAL
        setPadding(15F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt(), 15F.dp2Px().roundToInt(), 10F.dp2Px().roundToInt())

        titleTextView = createTitleTextView()
        addView(titleTextView)

        subtitleTextView = createSubtitleTextView()
        addView(subtitleTextView)
    }

    private object ViewId {
        val TITLE_TEXT_VIEW = hashCode() + 1
        val SUBTITLE_TEXT_VIEW = hashCode() + 2
    }

    private fun createTitleTextView(): MaterialTextView {
        val textView = MaterialTextView(context)
        textView.id = ViewId.TITLE_TEXT_VIEW
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        textView.layoutParams = layoutParams
        textView.letterSpacing = 0F
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bazaar_ic_chevron_down, 0)
        textView.compoundDrawablePadding = 10F.dp2Px().roundToInt()
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.setSingleLine()
        textView.maxLines = 1
        textView.isAllCaps = false
        textView.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textView.setTextColor(ContextCompat.getColor(context, R.color.bazaar_black))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17F)
        return textView
    }

    private fun createSubtitleTextView(): MaterialTextView {
        val textView = MaterialTextView(context)
        textView.id = ViewId.SUBTITLE_TEXT_VIEW
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 1F.dp2Px().roundToInt(), 0, 0)
        textView.layoutParams = layoutParams
        textView.letterSpacing = 0F
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.setSingleLine()
        textView.maxLines = 1
        textView.includeFontPadding = false
        textView.isAllCaps = false
        textView.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textView.setTextColor(ContextCompat.getColor(context, R.color.bazaar_metal))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11F)
        textView.text = "Текущий альбом"
        return textView
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun toggleIcon(isUp: Boolean) {
        if (isUp) {
            titleTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bazaar_ic_chevron_up, 0)
        } else {
            titleTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bazaar_ic_chevron_down, 0)
        }
    }

}