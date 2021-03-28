package kz.zhombie.bazaar.ui.arbat.cinema

import android.view.View
import com.alexvasilkov.gestures.animation.ViewPosition

interface CinemaDialogFragmentListener {
    fun onTrackViewPosition(view: View)
    fun onTrackViewPosition(viewPosition: ViewPosition)

    fun setScreenView(view: View): CinemaDialogFragment
}