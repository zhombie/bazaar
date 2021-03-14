package kz.zhombie.bazaar.ui.media.audible

import android.content.Context
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.ui.components.recyclerview.SpacingItemDecoration
import kz.zhombie.bazaar.ui.model.UIMultimedia

internal class AudiosAdapterManager  constructor(
    private val context: Context,
    private val recyclerView: RecyclerView
) {

    private var audiosHeaderAdapter: AudiosHeaderAdapter? = null
    private var audiosAdapter: AudiosAdapter? = null
    private var concatAdapter: ConcatAdapter? = null

    private var itemDecoration: SpacingItemDecoration? = null

    fun create(
        imageLoader: ImageLoader,
        isExplorerEnabled: Boolean,
        audiosHeaderAdapterCallback: AudiosHeaderAdapter.Callback,
        audiosAdapterCallback: AudiosAdapter.Callback
    ) {
        audiosHeaderAdapter = AudiosHeaderAdapter(isExplorerEnabled, audiosHeaderAdapterCallback)
        audiosAdapter = AudiosAdapter(imageLoader, audiosAdapterCallback) { leftOffset ->
            itemDecoration?.decoratorLeftOffset = leftOffset
        }

        concatAdapter = ConcatAdapter(audiosHeaderAdapter, audiosAdapter)
        recyclerView.adapter = concatAdapter

        val layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
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

        itemDecoration = SpacingItemDecoration(
            isDecoratorEnabled = true,
            decoratorWidth = context.resources.getDimension(R.dimen.media_item_list_decorator_width),
            decoratorColor = ContextCompat.getColor(context, R.color.gray),
            spacingLeft = context.resources.getDimensionPixelOffset(R.dimen.media_item_list_margin_left),
            spacingTop = context.resources.getDimensionPixelOffset(R.dimen.media_item_list_margin_top),
            spacingRight = context.resources.getDimensionPixelOffset(R.dimen.media_item_list_margin_right),
            spacingBottom = context.resources.getDimensionPixelOffset(R.dimen.media_item_list_margin_bottom)
        )

        itemDecoration?.let { itemDecoration ->
            recyclerView.addItemDecoration(itemDecoration)
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

    fun submitList(uiMultimedia: List<UIMultimedia>) {
        audiosAdapter?.submitList(uiMultimedia)
    }

    fun scrollToTop() {
        recyclerView.scrollToPosition(0)
    }

    fun destroy() {
        audiosHeaderAdapter?.let { concatAdapter?.removeAdapter(it) }
        audiosAdapter?.let { concatAdapter?.removeAdapter(it) }
        audiosHeaderAdapter = null
        audiosAdapter = null
        concatAdapter = null
    }

}