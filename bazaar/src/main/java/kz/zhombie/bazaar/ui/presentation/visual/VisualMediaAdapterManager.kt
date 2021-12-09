package kz.zhombie.bazaar.ui.presentation.visual

import android.content.Context
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.ui.components.recyclerview.animator.SlideInUpAnimator
import kz.zhombie.bazaar.ui.components.recyclerview.decoration.SpacingItemDecoration
import kz.zhombie.bazaar.ui.model.UIContent
import kz.zhombie.bazaar.ui.model.UIMedia

internal class VisualMediaAdapterManager constructor(
    private val context: Context,
    private val recyclerView: RecyclerView
) {

    private var visualMediaHeaderAdapter: VisualMediaHeaderAdapter? = null
    private var visualMediaAdapter: VisualMediaAdapter? = null
    private var concatAdapter: ConcatAdapter? = null

    fun create(
        isCameraEnabled: Boolean,
        isChooseFromLibraryEnabled: Boolean,
        visualMediaHeaderAdapterCallback: VisualMediaHeaderAdapter.Callback,
        visualMediaAdapterCallback: VisualMediaAdapter.Callback
    ) {
        visualMediaHeaderAdapter = VisualMediaHeaderAdapter(
            isCameraEnabled = isCameraEnabled,
            isChooseFromLibraryEnabled = isChooseFromLibraryEnabled,
            callback = visualMediaHeaderAdapterCallback
        )

        visualMediaAdapter = VisualMediaAdapter(visualMediaAdapterCallback)

        concatAdapter = ConcatAdapter(visualMediaHeaderAdapter, visualMediaAdapter)
        recyclerView.adapter = concatAdapter

        val layoutManager = GridLayoutManager(
            context,
            3,
            GridLayoutManager.VERTICAL,
            false
        )

        recyclerView.layoutManager = layoutManager

        recyclerView.setHasFixedSize(true)

        recyclerView.itemAnimator = SlideInUpAnimator(OvershootInterpolator(1f)).apply {
            addDuration = 125L
            changeDuration = 125L
            moveDuration = 125L
            removeDuration = 125L
        }

        recyclerView.addItemDecoration(
            SpacingItemDecoration(
                spacingLeft = context.resources.getDimensionPixelOffset(R.dimen.bazaar_media_item_grid_margin_left),
                spacingTop = context.resources.getDimensionPixelOffset(R.dimen.bazaar_media_item_grid_margin_top),
                spacingRight = context.resources.getDimensionPixelOffset(R.dimen.bazaar_media_item_grid_margin_right),
                spacingBottom = context.resources.getDimensionPixelOffset(R.dimen.bazaar_media_item_grid_margin_bottom)
            )
        )
    }

    fun setCameraEnabled(isEnabled: Boolean) {
        visualMediaHeaderAdapter?.isCameraEnabled = isEnabled
    }

    fun setChooseFromLibraryEnabled(isEnabled: Boolean) {
        visualMediaHeaderAdapter?.isChooseFromLibraryEnabled = isEnabled
    }

    fun show() {
        recyclerView.visibility = View.VISIBLE
    }

    fun hide() {
        recyclerView.visibility = View.GONE
    }

    fun setPadding(
        extraPaddingLeft: Int = 0,
        extraPaddingTop: Int = 0,
        extraPaddingRight: Int = 0,
        extraPaddingBottom: Int = 0
    ) {
        recyclerView.setPadding(
            recyclerView.paddingLeft + extraPaddingLeft,
            recyclerView.paddingTop + extraPaddingTop,
            recyclerView.paddingRight + extraPaddingRight,
            recyclerView.paddingBottom + extraPaddingBottom
        )
    }

    fun submitList(uiContents: List<UIContent>) {
        visualMediaAdapter?.submitList(uiContents.filterIsInstance<UIMedia>())
    }

    fun scrollToTop() {
        recyclerView.scrollToPosition(0)
    }

    fun destroy() {
        visualMediaHeaderAdapter?.let { concatAdapter?.removeAdapter(it) }
        visualMediaAdapter?.let { concatAdapter?.removeAdapter(it) }
        visualMediaHeaderAdapter = null
        visualMediaAdapter = null
        concatAdapter = null
    }

}