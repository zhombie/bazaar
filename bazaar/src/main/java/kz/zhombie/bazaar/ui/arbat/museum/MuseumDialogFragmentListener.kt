package kz.zhombie.bazaar.ui.arbat.museum

import android.view.View
import com.alexvasilkov.gestures.animation.ViewPosition

interface MuseumDialogFragmentListener {
    fun onTrackViewPosition(view: View)
    fun onTrackViewPosition(viewPosition: ViewPosition)

    fun setArtworkView(view: View): MuseumDialogFragment
}