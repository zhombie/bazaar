package kz.zhombie.bazaar.ui.presentation.document

import android.content.Context
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.ui.components.recyclerview.animator.SlideInUpAnimator
import kz.zhombie.bazaar.ui.components.recyclerview.decoration.SpacingItemDecoration
import kz.zhombie.bazaar.ui.model.UIMultimedia

internal class DocumentsAdapterManager constructor(
    private val context: Context,
    private val recyclerView: RecyclerView
) {

    private var documentsHeaderAdapter: DocumentsHeaderAdapter? = null
    private var documentsAdapter: DocumentsAdapter? = null
    private var concatAdapter: ConcatAdapter? = null

    private var itemDecoration: SpacingItemDecoration? = null

    fun create(
        isChooseFromLibraryEnabled: Boolean,
        documentsHeaderAdapterCallback: DocumentsHeaderAdapter.Callback,
        documentsAdapterCallback: DocumentsAdapter.Callback
    ) {
        documentsHeaderAdapter = DocumentsHeaderAdapter(isChooseFromLibraryEnabled, documentsHeaderAdapterCallback)
        documentsAdapter = DocumentsAdapter(documentsAdapterCallback)

        concatAdapter = ConcatAdapter(documentsHeaderAdapter, documentsAdapter)
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
            decoratorWidth = context.resources.getDimension(R.dimen.bazaar_media_item_list_decorator_width),
            decoratorColor = ContextCompat.getColor(context, R.color.bazaar_gray),
            isFirstItemDecoratorEnabled = false,
            isLastItemDecoratorEnabled = false,
            spacingLeft = context.resources.getDimensionPixelOffset(R.dimen.bazaar_media_item_list_margin_left),
            spacingTop = context.resources.getDimensionPixelOffset(R.dimen.bazaar_media_item_list_margin_top),
            spacingRight = context.resources.getDimensionPixelOffset(R.dimen.bazaar_media_item_list_margin_right),
            spacingBottom = context.resources.getDimensionPixelOffset(R.dimen.bazaar_media_item_list_margin_bottom)
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
        documentsAdapter?.submitList(uiMultimedia)
    }

    fun scrollToTop() {
        recyclerView.scrollToPosition(0)
    }

    fun destroy() {
        documentsHeaderAdapter?.let { concatAdapter?.removeAdapter(it) }
        documentsAdapter?.let { concatAdapter?.removeAdapter(it) }
        documentsHeaderAdapter = null
        documentsAdapter = null
        concatAdapter = null
    }

    private fun RecyclerView.smoothSnapToPosition(
        position: Int,
        snapMode: Int = LinearSmoothScroller.SNAP_TO_START
    ) {
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = snapMode
            override fun getHorizontalSnapPreference(): Int = snapMode
        }
        smoothScroller.targetPosition = position
        layoutManager?.startSmoothScroll(smoothScroller)
    }

}