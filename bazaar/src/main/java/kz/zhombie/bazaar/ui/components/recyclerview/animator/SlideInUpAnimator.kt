package kz.zhombie.bazaar.ui.components.recyclerview.animator

import android.view.animation.Interpolator
import androidx.recyclerview.widget.RecyclerView

// Source: https://github.com/wasabeef/recyclerview-animators
internal open class SlideInUpAnimator : BaseItemAnimator {
    constructor()
    constructor(interpolator: Interpolator) {
        this.interpolator = interpolator
    }

    override fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().apply {
            translationY(holder.itemView.height.toFloat())
            alpha(0f)
            duration = removeDuration
            interpolator = interpolator
            setListener(DefaultRemoveAnimatorListener(holder))
            startDelay = getRemoveDelay(holder)
        }.start()
    }

    override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder) {
        holder.itemView.translationY = holder.itemView.height.toFloat()
        holder.itemView.alpha = 0f
    }

    override fun animateAddImpl(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().apply {
            translationY(0f)
            alpha(1f)
            duration = addDuration
            interpolator = interpolator
            setListener(DefaultAddAnimatorListener(holder))
            startDelay = getAddDelay(holder)
        }.start()
    }
}
