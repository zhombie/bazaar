package kz.zhombie.bazaar.ui.media.gallery

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.ImageLoader
import kz.zhombie.bazaar.ui.media.SpacingItemDecoration
import kz.zhombie.bazaar.ui.model.UIMedia

internal class GalleryAdapterManager constructor(
    private val context: Context,
    private val recyclerView: RecyclerView
) {

    private var galleryHeaderAdapter: GalleryHeaderAdapter? = null
    private var galleryAdapter: GalleryAdapter? = null
    private var concatAdapter: ConcatAdapter? = null

    fun create(imageLoader: ImageLoader, callback: GalleryAdapter.Callback) {
        galleryHeaderAdapter = GalleryHeaderAdapter()
        galleryAdapter = GalleryAdapter(imageLoader, callback)
        concatAdapter = ConcatAdapter(galleryHeaderAdapter, galleryAdapter)
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

    fun submitList(uiMedia: List<UIMedia>) {
        galleryAdapter?.submitList(uiMedia)
    }

    fun scrollToTop() {
        recyclerView.scrollToPosition(0)
    }

    fun destroy() {
        galleryHeaderAdapter?.let { concatAdapter?.removeAdapter(it) }
        galleryAdapter?.let { concatAdapter?.removeAdapter(it) }
        galleryHeaderAdapter = null
        galleryAdapter = null
        concatAdapter = null
    }

}