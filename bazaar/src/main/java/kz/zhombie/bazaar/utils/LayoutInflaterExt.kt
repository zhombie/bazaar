package kz.zhombie.bazaar.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

internal fun ViewGroup.inflate(@LayoutRes resId: Int): View {
    return LayoutInflater
        .from(context)
        .inflate(resId, this, false)
}