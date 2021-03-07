package kz.zhombie.bazaar.ui.media.album

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.ui.media.SpacingItemDecoration
import kz.zhombie.bazaar.ui.model.UIAlbum

internal class AlbumsAdapterManager constructor(
    private val context: Context,
    private val recyclerView: RecyclerView,
) {

    private var albumsAdapter: AlbumsAdapter? = null

    fun create(onAlbumClicked: (uiAlbum: UIAlbum) -> Unit) {
        if (albumsAdapter == null) {
            albumsAdapter = AlbumsAdapter(Settings.getImageLoader()) {
                onAlbumClicked(it)
            }

            recyclerView.adapter = albumsAdapter

            val layoutManager = GridLayoutManager(
                context,
                2,
                GridLayoutManager.VERTICAL,
                false
            )

            recyclerView.layoutManager = layoutManager

            recyclerView.setHasFixedSize(true)

            recyclerView.itemAnimator = null

            recyclerView.addItemDecoration(
                SpacingItemDecoration(
                    context.resources.getDimensionPixelOffset(R.dimen.album_item_margin_left),
                    context.resources.getDimensionPixelOffset(R.dimen.album_item_margin_top),
                    context.resources.getDimensionPixelOffset(R.dimen.album_item_margin_right),
                    context.resources.getDimensionPixelOffset(R.dimen.album_item_margin_bottom)
                )
            )
        }
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

    fun submitList(albums: List<UIAlbum>) {
        albumsAdapter?.submitList(albums)
    }

    fun destroy() {
        albumsAdapter = null
    }

}