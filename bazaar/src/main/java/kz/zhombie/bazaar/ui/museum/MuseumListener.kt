package kz.zhombie.bazaar.ui.museum

import android.view.View
import com.alexvasilkov.gestures.animation.ViewPosition

interface MuseumListener {
    fun onTrackViewPosition(view: View)
    fun onTrackViewPosition(viewPosition: ViewPosition)

    fun setArtworkView(view: View): MuseumDialogFragment
}