package kz.zhombie.bazaar.ui.media.visual

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.ui.components.recyclerview.SpacingItemDecoration
import kz.zhombie.bazaar.ui.model.UIMultimedia
import kz.zhombie.bazaar.ui.model.UIMedia

internal class VisualMediaAdapterManager constructor(
    private val context: Context,
    private val recyclerView: RecyclerView
) {

    private var visualMediaHeaderAdapter: VisualMediaHeaderAdapter? = null
    private var visualMediaAdapter: VisualMediaAdapter? = null
    private var concatAdapter: ConcatAdapter? = null

    fun create(
        imageLoader: ImageLoader,
        isCameraEnabled: Boolean,
        isExplorerEnabled: Boolean,
        visualMediaHeaderAdapterCallback: VisualMediaHeaderAdapter.Callback,
        visualMediaAdapterCallback: VisualMediaAdapter.Callback
    ) {
        visualMediaHeaderAdapter = VisualMediaHeaderAdapter(
            isCameraEnabled = isCameraEnabled,
            isExplorerEnabled = isExplorerEnabled,
            callback = visualMediaHeaderAdapterCallback
        )

        visualMediaAdapter = VisualMediaAdapter(imageLoader, visualMediaAdapterCallback)

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

        recyclerView.itemAnimator = null

        recyclerView.addItemDecoration(
            SpacingItemDecoration(
                context.resources.getDimensionPixelOffset(R.dimen.media_item_margin_left),
                context.resources.getDimensionPixelOffset(R.dimen.media_item_margin_top),
                context.resources.getDimensionPixelOffset(R.dimen.media_item_margin_right),
                context.resources.getDimensionPixelOffset(R.dimen.media_item_margin_bottom)
            )
        )
    }

    fun setCameraEnabled(isEnabled: Boolean) {
        visualMediaHeaderAdapter?.isCameraEnabled = isEnabled
    }

    fun setExplorerEnabled(isEnabled: Boolean) {
        visualMediaHeaderAdapter?.isExplorerEnabled = isEnabled
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

    fun submitList(uiMultimedia: List<UIMultimedia>) {
        visualMediaAdapter?.submitList(uiMultimedia.filterIsInstance<UIMedia>())
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