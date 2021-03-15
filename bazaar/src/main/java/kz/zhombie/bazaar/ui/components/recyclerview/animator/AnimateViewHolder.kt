package kz.zhombie.bazaar.ui.components.recyclerview.animator

import android.animation.Animator
import androidx.recyclerview.widget.RecyclerView

internal interface AnimateViewHolder {
    fun preAnimateAddImpl(holder: RecyclerView.ViewHolder)
    fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder)
    fun animateAddImpl(holder: RecyclerView.ViewHolder, listener: Animator.AnimatorListener)
    fun animateRemoveImpl(holder: RecyclerView.ViewHolder, listener: Animator.AnimatorListener)
}