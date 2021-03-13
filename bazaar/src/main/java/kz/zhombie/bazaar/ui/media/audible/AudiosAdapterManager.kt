package kz.zhombie.bazaar.ui.media.audible

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.ui.components.recyclerview.SpacingItemDecoration
import kz.zhombie.bazaar.ui.model.UIMultimedia

internal class AudiosAdapterManager  constructor(
    private val context: Context,
    private val recyclerView: RecyclerView
) {

    private var audiosAdapter: AudiosAdapter? = null

    fun create(
        imageLoader: ImageLoader
    ) {
        audiosAdapter = AudiosAdapter(imageLoader)

        recyclerView.adapter = audiosAdapter

        val layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
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

    fun submitList(uiMultimedia: List<UIMultimedia>) {
        audiosAdapter?.submitList(uiMultimedia)
    }

    fun scrollToTop() {
        recyclerView.scrollToPosition(0)
    }

    fun destroy() {
        audiosAdapter = null
    }

}