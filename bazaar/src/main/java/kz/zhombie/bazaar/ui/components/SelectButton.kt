package kz.zhombie.bazaar.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import androidx.core.text.buildSpannedString
import com.google.android.material.button.MaterialButton
import kz.zhombie.bazaar.R

internal class SelectButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {

    fun setText(title: String, subtitle: String) {
        text = buildSpannedString {
            append(title + "\n" + subtitle)

            setSpan(ForegroundColorSpan(Color.parseColor("#333333")), 0, title.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, title.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            setSpan(RelativeSizeSpan(0.7F), title.length, title.length + subtitle.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(Color.parseColor("#8290A0")), title.length, title.length + subtitle.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.NORMAL), title.length, subtitle.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

}